# Review do bootstrap Native Image — Etapa 1D

Data: 13/09/2026. Escopo: bloco posterior a EngineFX `ea51da3`, DinoFX `414f019` e FarmFX `478cff8`, confrontado com os planos geral/detalhado da raiz compartilhada.

## Resultado

A configuração usa um adaptador explícito; cenas são criadas por factories do `SceneRegistry`, sem reflexão por nome de classe. O perfil é escolhido antes do startup da plataforma e permanece fixo. O bootstrap integrado foi compilado e executado localmente em Native Image, usando configuração empacotada e scripts externos criados após o build.

## Achados e correções

| Prioridade | Problema | Correção/evidência |
| --- | --- | --- |
| Alta | `RuntimeProfile` tratava uma propriedade de sistema como prova de inicialização. Era possível autorizar startup sem inicializar, apagar a propriedade e trocar de perfil; alterações posteriores no backend não eram detectadas pelo launcher. | Estado privado sincronizado guarda o perfil ativo. `requireInitialized` verifica estado real, seleção e backend antes de chamar GLFW. Três probes em JVMs separadas reproduziram as falhas e passaram após a correção. |
| Média | `ControleBase.setup` criava loading e publicava parte das definições antes de encontrar um id/boot inválido. Uma tentativa corrigida podia ficar bloqueada pelo estado parcial. | Validação integral antes de publicar definições ou iniciar loading. Regressão verifica ausência de cena ativa/definições parciais e setup válido após a rejeição. |
| Média | Os smokes dos dois jogos substituíam a comparação da cena carregada por `registry.contains(id)`. Isso não detecta uma cena errada ou uma troca perdida. | Verificação da classe efetivamente carregada contra o id esperado, sem instanciar cenas extras. Anotações `@Bootable` sem efeito foram retiradas do DinoFX. |
| Média | O gate compilava a engine com testes ignorados e exercitava só um retorno escalar, sem provar a fronteira host/Lua e o lifecycle integrado no EXE. | Engine testada pelo próprio script; 32 ciclos com setup/update/fixed_update/on_event/dispose, 32 callbacks Java e roundtrip de UTF-8, inteiros, frações, listas vazias e nulos. Seis casos negativos exigem exit diferente de zero e diagnóstico específico. |
| Média | O CI solicitava JDK 25.0.4, enquanto o helper exige GraalVM 25.3.4.1, baseada em JDK 25.0.4.1. A versão exata não estava fixada. | Download da release oficial exata com SHA-256 fixado e validação do release pelo mesmo helper local. O workflow também cobre mudanças em testes/componentes Lua e tem prazo de 30 minutos. O job remoto não foi disparado. |
| Média | O plugin acrescentava metadados externos automaticamente, inclusive de Gson 2.14.0 para a dependência 2.8.6, além dos metadados manuais declarados. | Repositório automático de metadados desabilitado no harness. O gate final usa os metadados versionados e os fornecidos pelas próprias dependências/toolchain; não houve cópia de saída bruta do tracing agent. |
| Baixa | `bootstrap-harness/target` aparecia entre os arquivos novos para commit, incluindo JAR/classes. | Diretório acrescentado ao `.gitignore`; somente fontes, POM, workflow, script e metadados são versionados. |

A hipótese inicial de que `native:compile` não compilava o harness foi descartada: o descriptor do plugin 1.1.12 declara `executePhase=package`, confirmado no log. O script original também compilou e executou com sucesso. A toolchain estava em `.tools`, fora do PATH; a afirmação preliminar de ausência de `native-image` não se confirmou.

## Validação

- Baseline EngineFX: 65 testes verdes no OpenJDK 25.0.2.
- Regressões antes da correção: seis testes focados, quatro falhas (três de perfil e uma de setup parcial).
- EngineFX após correções: **69 testes, zero falhas, erros ou ignorados**, no OpenJDK 25.0.2 e no JDK GraalVM 25.0.4.1. Build limpo removeu a classe obsoleta `EngineSmokeApp` do artefato.
- Gate Native Image: GraalVM CE **25.3.4.1+1.1**, JDK **25.0.4.1+1**, MSVC **19.44.35228**, Windows x64, Native Build Tools **1.1.12**. EXE copiado sozinho para pasta nova; nenhum arquivo de configuração ou DLL Lua externo foi copiado. A configuração vem dos recursos da imagem.
- Aceite positivo do EXE: `registry=true resources=true externalLua=true profile=NATIVE_JNI lifecycle=32 hostCallbacks=32 values=true`.
- Seis rejeições nativas: retorno externo incorreto, loop protegido, capacidade ausente, tabela cíclica, UTF-8 inválido e bytecode. O prazo é de 30 segundos por processo; timeout não conta como rejeição aprovada.
- Os smokes finais dos consumidores e a inspeção dos JARs são registrados nos reviews do DinoFX e FarmFX. Os resultados locais ficam em `../.review/bloco6/`, a partir da raiz da engine, incluindo `engine-before.log`, `regressions-before.log`, `engine-final.log`, `native-before.log`, `native-expanded.log`, `native-final.log`, `dino-final.log` e `farm-final.log`.

Reprodução na raiz da engine:

```powershell
.\mvnw.cmd clean install
.\lua-harness\verify-bootstrap-native.ps1 -JavaHome C:\caminho\graalvm
..\dinofx\build.ps1 -Smoke -PresentMode fifo
..\farmfx\build.ps1 -Smoke -PresentMode fifo
```

O arquivo da toolchain foi conferido localmente com SHA-256 `770a0d78aba4c19bd40ee410ec82afbbee8e33fdd762e509006ac64e1007b4ce`, o mesmo fixado no workflow.

## Rastreabilidade e limites

O aceite de bootstrap da etapa 1D é coberto pelo harness integrado: configuração explícita, factories registradas, seleção de perfil, recursos empacotados e Lua externa. Os consumidores usam os mesmos registries no launcher e nos smokes JVM.

O harness não abre janela, device ou swapchain e não testa renderização, áudio, input ou cenas completas no Native Image. O valor `NATIVE_JNI` prova a seleção do perfil; não prova toda operação LWJGL nativa. O build pode gerar `jsound.dll` por reachability, mas ela não foi copiada nem áudio foi exercitado. O gate de plataforma anterior conserva seus próprios limites.

O catálogo Lua FarmFX, integração de save/Continuar, gameplay visual e distribuição nativa dos jogos continuam fora deste bloco. Não houve aceite visual por pixels, MAILBOX, validation layers, sessão prolongada ou instalação sem cache Maven. O workflow foi revisado localmente; a execução em GitHub Actions depende de um push autorizado e não foi simulada como sucesso remoto.

## Fontes técnicas

- [Native Build Tools — plugin Maven](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html): consumo de metadados em `META-INF/native-image` e configuração do repositório externo.
- [Release oficial GraalVM CE 25.3.4.1](https://github.com/graalvm/graalvm-ce-builds/releases/tag/graal-25.3.4.1): distribuição Innovation 3/JDK 25.0.4.1 utilizada no gate.
