# Review do terceiro bloco — gate Lua/Native Image

Data: 12/09/2026. Base EngineFX: `8d4e9e6`; base FarmFX: `01c1be6`; DinoFX: `5eba0fb`. Escopo: novo subprojeto `lua-harness` e atualização de acompanhamento. O artefato principal da engine, o runtime Nashorn e os fontes dos jogos não mudaram neste bloco.

## Achados e correções

| Prioridade | Achado reproduzido ou verificado no código | Correção |
| --- | --- | --- |
| P1 | Lua capturava o erro de limite com `pcall`, continuava e o gate imprimia PASS; o contador acumulado permitia que uma falha posterior parecesse comprovar o limite. | Wrappers privados de `pcall`/`xpcall` verificam o orçamento antes e depois da chamada protegida. Callbacks de aplicação e fronteiras Java também recusam execução após o limite. O teste exige a exceção Lua esperada, sem aceitar qualquer `Throwable`. |
| P2 | Um módulo externo que apenas retornava 84, sem chamar `engine_record`, recebia PASS. Retornos 84.5 eram truncados para 84. | Verificação explícita da callback externa, tipo numérico e valor exato. Argumentos da callback também recusam strings, frações e overflow. |
| P2 | O relatório atribuía 200 ciclos ao EXE, mas somente o teste JUnit chamava o método de repetição. | `main` executa os 200 ciclos e só imprime `lifecycle=200` depois de concluí-los. |
| P2 | O suposto teste de callback nativo chamava `callback.invoke` em Java. Além disso, a inscrição nativa não era liberada e a janela criava um contexto OpenGL desnecessário. | Chamada pelo endereço do trampolim usando `JNI.invokePV`, desinscrição e fechamento explícitos; GLFW usa `GLFW_NO_API`. |
| P2 | A leitura carregava o arquivo inteiro antes de aplicar o limite de 1 MiB. | Leitura limitada a 1 MiB + 1 byte, seguida da rejeição antes de avaliar Lua. |
| P2 | Os scripts não reproduziam automaticamente o EXE em pasta isolada, não tinham prazo de execução e deixavam o toolchain no ambiente do chamador. | Toolchain exato conferido; testes antes de compilar; EXE copiado para pasta nova; seis casos negativos nativos; prazo de 30 segundos por processo; ambiente restaurado em `finally`. |
| P2 | `lua-harness/target` não estava coberto pelo `.gitignore` da engine, permitindo incluir EXEs, DLLs, relatórios e caches no commit inicial do subprojeto. | Diretório gerado explicitamente ignorado; apenas fontes, scripts e metadados entram no commit. |
| P3 | Decodificação STB que alocasse pixels e falhasse na conferência das dimensões não liberava o buffer. | Liberação em `finally`, também na falha da conferência. |
| P3 | Os comandos JVM da documentação combinavam um wrapper e um POM relativos a diretórios diferentes. O blueprint dizia não existir scripting apesar do Nashorn legado. | Comandos a partir da raiz da engine; distinção entre API Lua futura e scripting legado. |

Os três testes recebidos passaram. Quatro testes adicionais produziram três falhas antes das correções: orçamento capturado, callback ausente e retorno fracionário. A suíte final tem nove testes, incluindo preservação de retornos múltiplos/erros normais, UTF-8 com acentos e limite de tamanho. O orçamento em chamadas protegidas e aninhadas também foi verificado em processos nativos com prazo externo.

## Evidência de execução

JVM: OpenJDK 25.0.2, Windows x64, Maven 3.9.16. Native Image: GraalVM CE `25.3.4.1+1.1`, JDK `25.0.4.1+1`, MSVC `19.44.35228`, alvo padrão `x86-64-v3`. LuaJava `4.1.0`, Lua 5.4, Native Build Tools `1.1.12`, LWJGL `3.4.3`. Foi usado cache Maven offline; instalação sem cache não foi repetida.

Na raiz da engine:

```powershell
.\mvnw.cmd -B -f .\lua-harness\pom.xml test
.\lua-harness\verify-native.ps1 -JavaHome C:\caminho\para\graalvm
.\lua-harness\verify-platform-native.ps1 -JavaHome C:\caminho\para\graalvm
```

- JVM: **9 testes, zero falhas/erros/ignorados**; 5,812 s de build no JDK 25.0.2. O build Native Image também executou os nove testes em seu JDK.
- Lua nativa: **PASS**, callback 84, chamada Java → Lua 42, 100 hooks, módulo externo e 200 ciclos. Build de 1 min 31 s; EXE de 18.137.088 bytes.
- Rejeições nativas: bytecode, UTF-8 inválido, callback ausente, retorno fracionário, loop com `pcall` e loop protegido aninhado: **6/6**, todos com saída não zero e diagnóstico esperado.
- O EXE Lua foi copiado sozinho para pasta nova antes de criar as entradas; nenhuma DLL Lua foi copiada ao lado dele. Os recursos JNI/DLL permanecem empacotados pelo subprojeto.
- Script rejeitou JDK comum e preservou `JAVA_HOME`/`PATH` do chamador. Sintaxe dos três scripts PowerShell conferida.
- Plataforma: build concluído em 1 min 49 s; STB, consulta de versão Vulkan e trampolim de teclado passaram. O EXE terminou com **exit 1** em `AudioSystem.getClip`: `Can't find java.home ??`. O backend Unsafe tentou acesso a `jdk.internal.misc.Unsafe` e recorreu ao legado com diagnóstico. Esses bloqueios permanecem explicitamente abertos para as etapas 12–14; não foram convertidos em sucesso pelo script.

O [relatório do harness](../../lua-harness/REPORT_2026-09-12.md) registra o resultado da prova parcial de plataforma e seus bloqueios. Logs desta revisão estão em `../../../.review/bloco3/`: `jvm-before.log`, `regressions-before.log`, `jvm-after.log`, `native-lua.log` e `native-platform.log`. Fontes, scripts e testes de reprodução são versionados; logs completos e binários são locais.

## Parecer e limites

O gate obrigatório de seleção Lua funciona no JVM e Native Image com as correções acima. Isso permite continuar a etapa 1B; não significa aprovação da distribuição nativa da engine. A consulta ao loader Vulkan não prova dispositivo, swapchain, envio de textura, fonte desenhada ou apresentação. O callback sintético atravessa JNI/libffi, mas não simula uma tecla física do Windows. Áudio e o renderer nativo completo seguem o encaminhamento permitido às etapas 12–14.

Este harness limita instruções por estado e tamanho da fonte. Não implementa orçamento por callback, limite de memória da VM, limite estrutural de valores, tempo máximo de funções C, isolamento completo de mods, API `ScriptRuntime`, registro explícito de cenas ou migração do Nashorn. Os 200 ciclos demonstram o caminho repetido de fechamento, sem medição de vazamento nativo ou estabilidade prolongada. O alvo `x86-64-v3` foi executado na máquina local; CPUs x64 antigas e Windows limpo não foram validados.

Referências primárias conferidas: [hook de debug LuaJava](https://luajava.iroiro.party/examples/debug.html), [artefatos LuaJava](https://luajava.iroiro.party/getting-started.html), [chamadas protegidas de Lua 5.4](https://www.lua.org/manual/5.4/manual.html#pdf-pcall) e [classificador JNI/Unsafe do LWJGL](https://github.com/LWJGL/lwjgl3/blob/master/doc/FFM.md).
