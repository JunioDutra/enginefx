# EngineFX

Engine 2D desktop em Java, com cenas e componentes, renderização **GLFW + LWJGL Vulkan**, imagens RGBA, fontes STB, áudio Java Sound/OpenAL e API Lua 5.4 restrita. Versão Maven preparada para release: `enginefx:enginefx:3.1.0`.

## Documentação

| Documento | Finalidade |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Orientações para alterar e validar o código |
| [BluePrint.md](BluePrint.md) | Arquitetura implementada e pontos de extensão |
| [PRD.md](PRD.md) | Andamento, critérios de aceite e próximos passos |
| [Review 2.1](docs/reviews/2026-09-12-enginefx-2.1.md) | Contratos atuais, correções e validação no JDK 25 |
| [Harness Lua/Native Image](lua-harness/README.md) | Gate técnico isolado da Etapa 1A e sua reprodução |
| [Review do gate Lua](docs/reviews/2026-09-12-lua-native-gate.md) | Correções do terceiro bloco, testes nativos e limites |
| [Review da API Lua 2.2](docs/reviews/2026-09-13-enginefx-2.2-lua-api.md) | Implementação da Etapa 1B e contratos ainda legados |
| [Migração JavaScript para Lua](docs/javascript-to-lua.md) | Guia de substituição da API removida na 3.0 |
| [Review da migração Lua 3.0](docs/reviews/2026-09-13-enginefx-3.0-lua-migration.md) | Remoção de Nashorn e smoke do consumidor empacotado |
| [Review bootstrap Native Image](docs/reviews/2026-09-13-native-bootstrap.md) | Contratos explícitos da Etapa 1D e evidências disponíveis |
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

O build executa os testes e instala `target/enginefx-3.1.0.jar` no repositório Maven local. Para testes sem instalação: `.\mvnw.cmd test`. `-SkipTests` é opcional no script.

A engine é uma biblioteca; o exemplo executável está no repositório irmão `dinofx`. Com os dois checkouts lado a lado:

```powershell
.\dinofx\build.ps1 -Smoke -PresentMode fifo
java --enable-native-access=ALL-UNNAMED -jar .\dinofx\target\dino.jar
```

O script integrado instala a engine, empacota o jogo e, com `-Smoke`, roda as cenas a partir de uma pasta vazia. Pode ser chamado de qualquer diretório.

## Contrato para jogos

O jogo fornece `application.json`, factories de `Scene` e assets. Sua entrada registra todos os ids antes de chamar `Executor.loadGame(args, scenes)`:

```java
SceneRegistry scenes = new SceneRegistry()
    .register("game:menu", MenuScene::new)
    .register("game:play", PlayScene::new);
Executor.loadGame(args, scenes);
```

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

Para conteúdo pertencente a um pacote, use `ResourceRef` e um `ResourceResolver` específico do pacote. A referência carrega `packId`, hash e caminho relativo; imagens, fontes e mapas são cacheados com essa identidade, de modo que mods com o mesmo nome de arquivo não colidem. `DirectoryResourceResolver` valida o caminho real antes de abrir o arquivo e recusa travessia, caminho absoluto, namespace alheio e fuga por symlink/junction. Para scripts que já estão no JAR, `ClasspathResourceResolver` abre somente recursos abaixo de uma raiz declarada, sem expor caminhos de disco.

## Lua 5.4 (API 3.0)

`ScriptRuntime` cria estados Lua isolados e é estritamente confinado à thread que o criou. Sua API pública usa apenas `ScriptValue`, `ScriptContext`, `ScriptEvent`, `ResourceRef` e interfaces da engine; tipos de LuaJava nunca chegam ao jogo ou ao script. A fonte precisa ser UTF-8, ter até 1 MiB e retornar uma tabela de callbacks.

```java
var resolver = new DirectoryResourceResolver("base", packHash, packRoot);
var apis = new ScriptApiRegistry()
    .register("farm", "register_plant", "farm.content", (context, args) -> ScriptValue.of(null));
try (var lua = ScriptRuntime.lua(resolver, apis)) {
    var module = lua.loadModule(resolver.ref("scripts/content.lua"));
    var instance = lua.createInstance(module,
        new ScriptContext(Set.of("farm.content"), Map.of(), clock, events::add));
    instance.setup();
}
```

Os callbacks opcionais são `setup`, `update`, `fixed_update`, `on_event` e `dispose`. Deltas Lua são sempre segundos. A engine expõe somente `engine.clock.now_millis`, `engine.state.get/set` e `engine.events.emit`, todos condicionados a capacidades explícitas do contexto; jogos registram apenas seus próprios namespaces. Não são expostos `GameObject`, `Scene`, `Screen`, singletons, `java`, sistema de arquivos, processo, rede, `debug`, `package` ou loaders Lua. O orçamento é de 100.000 instruções por callback e não pode ser convertido em sucesso por `pcall`/`xpcall`.

`LuaComponent` adapta esses callbacks ao lifecycle de componentes, sem passar seu pai para Lua. `LuaScene` aceita apenas comandos previamente autorizados em um `LuaSceneCommandSink`; Java continua dono da composição de objetos. Toda cena de `application.json` é um id de `SceneRegistry`, não um nome de classe. `bootScene` também é um id registrado; se ausente, a primeira cena declarada é o boot determinístico. `type: "js"` continua falhando com `SCRIPT_TYPE_REMOVED`; `type: "java"` é aceito somente como compatibilidade de configuração e não ativa reflexão.

`setup()`, `update(long)`, `fixedUpdate(float)`, `draw()` e `dispose()` formam o ciclo de componentes. O passo fixo é de 1/60 s; velocidades nesse callback são em pixels/segundo. Em `update`, use `Time.getDeltaTime()`. Inscreva mouse com dono: `Mouse.infInstace().addListener(this, callback)`.

Depois do bootstrap, um jogo pode agendar uma transição pelo id estável, sem depender da posição no JSON: `ControleBase.getInstance().nextScene("game:play")`. O id deve constar na configuração e no `SceneRegistry`; aliases configurados resolvem para a mesma cena.

`application.json` aceita `title`; ausência ou texto vazio mantém `Enginefx Vulkan`. `ControleBase.requestExit()` solicita saída normal do loop e pode ser repetido. `stop()` descarta uma única vez, mesmo se o descarte lançar exceção.

Teclado oferece `isDown`, `wasPressedThisFrame` e `wasReleasedThisFrame`. O loop consulta eventos antes da lógica e limpa as bordas publicadas ao concluir o frame. Callbacks recebidos durante renderização ficam pendentes para o próximo frame; teclas mantidas continuam disponíveis em `isDown`.

## Diagnóstico Vulkan

```powershell
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.presentMode=fifo' -jar ..\dinofx\target\dino.jar
$env:VK_LAYER_VALIDATE_SYNC = '1'
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.validation=true' -jar ..\dinofx\target\dino.jar
```

`presentMode` aceita `auto` (MAILBOX se disponível, senão FIFO), `fifo` e `mailbox`. Uma escolha explícita sem suporte falha com diagnóstico. A validação exige a camada Khronos instalada. `RuntimeProfile` escolhe o backend antes de carregar GLFW, Vulkan ou Lua: no JVM, `JVM_FFM`/`ffm`; na imagem nativa, `NATIVE_JNI`/`unsafe`. Para diagnosticar, use `-Denginefx.runtimeProfile=jvm-ffm` ou `native-jni` antes do launcher. Não defina `org.lwjgl.system.memoryBackend` diretamente: divergências falham cedo.

A migração ainda tem critérios de aceite pendentes. Consulte [PRD.md](PRD.md) antes de tratar um smoke aprovado como validação visual ou aprovação de driver.

O gate Lua 5.4/Native Image foi validado inicialmente em um subprojeto isolado. A Etapa 1D adiciona `bootstrap-harness/`, que compila a engine como Native Image e verifica configuração empacotada, registro de cenas e módulo Lua externo criado depois do build. Os metadados manuais ficam em `src/META-INF/native-image/enginefx/enginefx/`; o tracing agent só pode descobrir hipóteses, nunca gerar metadados aceitos sem revisão.


A [revisão dos blocos Lua 1B/1C](docs/reviews/2026-09-13-lua-blocks-review.md) documenta as correções de limites, conversão de listas/nulos, lifecycle e registro de cenas, além da validação dos dois consumidores contra a 3.0.0.


A validação atual da etapa 1D está no [review do bootstrap integrado](docs/reviews/2026-09-13-native-bootstrap.md): perfil fixo, configuração validada antes do loading e EXE com Lua externa. O gate executa 32 ciclos e seis rejeições esperadas; não representa renderer ou áudio nativos completos.
