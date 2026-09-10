# EngineFX

Engine 2D desktop em Java, com cenas e componentes, renderização **GLFW + LWJGL Vulkan**, imagens RGBA, fontes STB, áudio Java Sound e scripts Nashorn. Versão Maven: `enginefx:enginefx:2.0.0`.

## Documentação

| Documento | Finalidade |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Orientações para alterar e validar o código |
| [BluePrint.md](BluePrint.md) | Arquitetura implementada e pontos de extensão |
| [PRD.md](PRD.md) | Andamento, critérios de aceite e próximos passos |
| [REVIEW.md](REVIEW.md) | Falhas corrigidas, testes e limites desta revisão |
| [VULKAN_MIGRATION_REVIEW.md](VULKAN_MIGRATION_REVIEW.md) | Auditoria original que orientou a migração |

## Requisitos e build

- JDK **25 ou superior**; compilação com `--release 25`. A validação local usou OpenJDK 27.
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

O build executa os testes e instala `target/enginefx-2.0.0.jar` no repositório Maven local. Para testes sem instalação: `.\mvnw.cmd test`. `-SkipTests` é opcional no script.

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

A resolução procura, nessa ordem, `res/`, `src/main/resources/`, `target/classes/res/` e `/res/` no classpath. Só o mesmo caminho relativo sobrepõe um asset. Não há busca por nome base nem extração temporária do JAR. O jogo deve fornecer `fonts/font.ttf` para os componentes de texto padrão, loading e debug.

`setup()`, `update(long)`, `fixedUpdate(float)`, `draw()` e `dispose()` formam o ciclo de componentes. O passo fixo é de 1/60 s; velocidades nesse callback são em pixels/segundo. Em `update`, use `Time.getDeltaTime()`. Inscreva mouse com dono: `Mouse.infInstace().addListener(this, callback)`.

## Diagnóstico Vulkan

```powershell
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.presentMode=fifo' -jar ..\dinofx\target\dino.jar
$env:VK_LAYER_VALIDATE_SYNC = '1'
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.validation=true' -jar ..\dinofx\target\dino.jar
```

`presentMode` aceita `auto` (MAILBOX se disponível, senão FIFO), `fifo` e `mailbox`. Uma escolha explícita sem suporte falha com diagnóstico. A validação exige a camada Khronos instalada. O launcher usa o backend de memória FFM por padrão e respeita `-Dorg.lwjgl.system.memoryBackend`.

A migração ainda tem critérios de aceite pendentes. Consulte [PRD.md](PRD.md) antes de tratar um smoke aprovado como validação visual ou aprovação de driver.
