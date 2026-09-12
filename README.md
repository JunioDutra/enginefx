# EngineFX

Engine 2D desktop em Java, com cenas e componentes, renderização **GLFW + LWJGL Vulkan**, imagens RGBA, fontes STB, áudio Java Sound e scripts Nashorn legados durante a transição para Lua. Versão Maven: `enginefx:enginefx:2.1.0`.

## Documentação

| Documento | Finalidade |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Orientações para alterar e validar o código |
| [BluePrint.md](BluePrint.md) | Arquitetura implementada e pontos de extensão |
| [PRD.md](PRD.md) | Andamento, critérios de aceite e próximos passos |
| [Review 2.1](docs/reviews/2026-09-12-enginefx-2.1.md) | Contratos atuais, correções e validação no JDK 25 |
| [Harness Lua/Native Image](lua-harness/README.md) | Gate técnico isolado da Etapa 1A e sua reprodução |
| [Review do gate Lua](docs/reviews/2026-09-12-lua-native-gate.md) | Correções do terceiro bloco, testes nativos e limites |
| [REVIEW.md](REVIEW.md) | Revisão histórica da migração Vulkan |
| [VULKAN_MIGRATION_REVIEW.md](VULKAN_MIGRATION_REVIEW.md) | Auditoria original que orientou a migração |

## Requisitos e build

- JDK **25 ou superior**; compilação com `--release 25`. O checkpoint 2.1 foi validado no OpenJDK 25.0.2.
- Os nativos Maven atuais são para **Windows x64**.
- Driver com Vulkan para executar o jogo; os testes unitários usam bibliotecas nativas STB/shaderc, mas não precisam de GPU.
- Maven 3.9.16 é obtido pelo wrapper; não é necessário instalar Maven separadamente. O primeiro build precisa de acesso ao Maven Central.
- Configure `JAVA_HOME` ou coloque o JDK no `PATH`; os scripts não presumem um caminho de máquina.

Na raiz da engine:

```powershell
.\build.ps1
# Equivalente:
.\mvnw.cmd clean install
```

O build executa os testes e instala `target/enginefx-2.1.0.jar` no repositório Maven local. Para testes sem instalação: `.\mvnw.cmd test`. `-SkipTests` é opcional no script.

A engine é uma biblioteca; o exemplo executável está no repositório irmão `dinofx`. Com os dois checkouts lado a lado:

```powershell
.\dinofx\build.ps1 -Smoke -PresentMode fifo
java --enable-native-access=ALL-UNNAMED -jar .\dinofx\target\dino.jar
```

O script integrado instala a engine, empacota o jogo e, com `-Smoke`, roda as cenas a partir de uma pasta vazia. Pode ser chamado de qualquer diretório.

## Contrato para jogos

O jogo fornece `application.json`, classes `Scene` e assets. Sua entrada chama `Executor.loadGame(args)`.

```java
GameObject player = new GameObject("player");
player.addComponente(new Sprite("imagens/player.png"));
add(player);
```

Assets usam **caminhos exatos relativos a `res/`, com extensão**:

```java
ResourceManager.image("imagens/player.png");
ResourceManager.font("fonts/font.ttf", 24);
ResourceManager.audio("audio/theme.wav");
ResourceManager.map("mapas/level.tmx");
```

A resolução procura, nessa ordem, `res/`, `src/main/resources/`, `target/classes/res/` e `/res/` no classpath. Só o mesmo caminho relativo sobrepõe um asset. Não há busca por nome base nem extração temporária do JAR. O jogo deve fornecer `fonts/font.ttf` para os componentes de texto padrão e debug; o loading tolera falha de carregamento dessa fonte e pode ficar vazio.

`setup()`, `update(long)`, `fixedUpdate(float)`, `draw()` e `dispose()` formam o ciclo de componentes. O passo fixo é de 1/60 s; velocidades nesse callback são em pixels/segundo. Em `update`, use `Time.getDeltaTime()`. Inscreva mouse com dono: `Mouse.infInstace().addListener(this, callback)`.

`application.json` aceita `title`; ausência ou texto vazio mantém `Enginefx Vulkan`. `ControleBase.requestExit()` solicita saída normal do loop e pode ser repetido. `stop()` descarta uma única vez, mesmo se o descarte lançar exceção.

Teclado oferece `isDown`, `wasPressedThisFrame` e `wasReleasedThisFrame`. O loop consulta eventos antes da lógica e limpa as bordas publicadas ao concluir o frame. Callbacks recebidos durante renderização ficam pendentes para o próximo frame; teclas mantidas continuam disponíveis em `isDown`.

## Diagnóstico Vulkan

```powershell
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.presentMode=fifo' -jar ..\dinofx\target\dino.jar
$env:VK_LAYER_VALIDATE_SYNC = '1'
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.validation=true' -jar ..\dinofx\target\dino.jar
```

`presentMode` aceita `auto` (MAILBOX se disponível, senão FIFO), `fifo` e `mailbox`. Uma escolha explícita sem suporte falha com diagnóstico. A validação exige a camada Khronos instalada. O launcher usa o backend de memória FFM por padrão e respeita `-Dorg.lwjgl.system.memoryBackend`.

A migração ainda tem critérios de aceite pendentes. Consulte [PRD.md](PRD.md) antes de tratar um smoke aprovado como validação visual ou aprovação de driver.

O gate Lua 5.4/Native Image foi validado em um subprojeto isolado, sem expor LuaJava ao contrato da engine nem alterar o runtime Nashorn atual. Consulte o [relatório da prova](lua-harness/REPORT_2026-09-12.md); `ScriptRuntime` e a migração de consumidores pertencem à próxima etapa.
