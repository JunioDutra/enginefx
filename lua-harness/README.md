# Harness Lua/Native Image — Etapa 1A

Esta prova fica fora do artefato e da API pública da EngineFX. Ela fixa LuaJava `4.1.0` com Lua `5.4`, o binário desktop correspondente e Native Build Tools `1.1.12`. Não usa JSR-223. O plano original citava `1.1.13`, mas essa versão ainda não está publicada no Maven Central; `1.1.12` é a versão reproduzível mais recente consultada em 12/09/2026.

O executável exercita Lua UTF-8, uma callback Java registrada explicitamente, chamada Java → Lua, 200 criações/fechamentos de runtime e limite de instruções. A verificação nativa cria um `.lua` depois da compilação. Antes de avaliar a fonte, o harness limita a leitura a 1 MiB e recusa bytecode e UTF-8 inválido. O script recebe bibliotecas base, table, string, math e utf8; a única capacidade de aplicação publicada é `engine_record`. `java`, `io`, `os`, `debug`, `package`, `require`, `dofile`, `loadfile`, `load` e `collectgarbage` ficam ocultos.

O limite deste gate é de 100.000 instruções por estado, verificado a cada 1.000. `pcall` e `xpcall` preservam erros ordinários e múltiplos retornos, mas propagam o esgotamento do limite, inclusive em chamadas aninhadas. O futuro runtime ainda precisa definir o limite por callback, memória, profundidade e duração de operações nativas; o limite de fonte não é um limite de memória total de Lua.

## JVM

Todos os comandos abaixo partem da raiz de `enginefx`. Com JDK 25 configurado:

```powershell
.\mvnw.cmd -B -f .\lua-harness\pom.xml test
```

## Native Image Windows x64

Com PowerShell 7+, GraalVM Community `25.3.4.1` baseado em JDK 25, Native Image e compilador MSVC x64 disponíveis:

```powershell
.\lua-harness\verify-native.ps1 -JavaHome C:\caminho\para\graalvm
```

O script confere a versão do toolchain, executa testes e `native:compile`, copia apenas o EXE para um diretório novo em `target/lua-smoke-*` e cria os módulos de entrada após o build. Exige tanto o caso positivo quanto a rejeição de bytecode, UTF-8 inválido, callback ausente, retorno fracionário e loops protegidos/aninhados. Cada processo tem prazo de 30 segundos; `JAVA_HOME` e `PATH` são restaurados ao sair. As pastas de execução ficam disponíveis como evidência e são ignoradas pelo Git.

GraalVM 25.3 já não oferece fallback para JVM, por isso não recebe a opção obsoleta `--no-fallback`; o build concede `--enable-native-access=ALL-UNNAMED` porque LuaJava carrega uma DLL via `System.load`. `META-INF/native-image/enginefx/lua-native-harness/resource-config.json` inclui `lua5464.dll`; o loader do LuaJava a extrai da imagem e a carrega. `reachability-metadata.json` contém as entradas JNI necessárias à inicialização do LuaJava, sem entradas de launcher descobertas pelo agent. A prova não depende de uma DLL Lua ao lado do EXE.

Este é um gate técnico, não a API Lua da EngineFX. `ScriptRuntime`, `LuaScene`, `LuaComponent`, resolver de pacotes e migração do Nashorn pertencem às etapas 1B e 1C.

## Prova nativa de plataforma

```powershell
.\lua-harness\verify-platform-native.ps1 -JavaHome C:\caminho\para\graalvm
```

O segundo executável usa o classificador LWJGL `unsafe` e exercita GLFW oculto sem contexto OpenGL, consulta de versão do loader Vulkan, decodificação de imagem, métricas de fonte STB e um callback de teclado chamado através do trampolim JNI/libffi. Esse callback é sintético, não uma tecla física enviada pelo sistema operacional. O teste libera o callback, a janela e as alocações STB mesmo em falhas.

As fixtures de imagem/fonte vêm de `enginefx/tests/resources`; o WAV vem do checkout irmão `dinofx`. O script copia o EXE e `jsound.dll`, quando emitida pelo Native Image, para uma pasta nova em `target/platform-smoke-*`. A falha conhecida de Java Sound continua retornando código diferente de zero e tem diagnóstico no [relatório](REPORT_2026-09-12.md).

Esta prova de plataforma não cria dispositivo/swapchain, não envia textura à GPU, não rasteriza texto na tela e não substitui o smoke do renderer. Esses cenários e o backend de áudio nativo continuam nas etapas 12–14. O [review deste bloco](../docs/reviews/2026-09-12-lua-native-gate.md) separa as correções das pendências.
