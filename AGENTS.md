# Orientações para trabalhar na EngineFX

## Escopo e referências

Este repositório contém a biblioteca `enginefx:enginefx:2.0.0`. O jogo consumidor está no checkout irmão `dinofx`. Leia [README.md](README.md), [BluePrint.md](BluePrint.md) e [PRD.md](PRD.md). [REVIEW.md](REVIEW.md) registra a validação mais recente; a auditoria original está em [VULKAN_MIGRATION_REVIEW.md](VULKAN_MIGRATION_REVIEW.md).

## Build e validação

- JDK 25+, Maven Wrapper 3.9.16; nativos configurados para Windows x64.
- `.\mvnw.cmd test` executa JUnit em `tests/`; fixtures ficam em `tests/resources/`.
- `.\build.ps1` executa testes e instala a engine localmente.
- Para mudanças que afetam o consumidor, execute também `..\dinofx\build.ps1 -Smoke -PresentMode fifo`.
- O smoke cria uma pasta vazia e usa o JAR do jogo. Rodar apenas de dentro do checkout pode esconder falhas de assets.
- Compilar para Java 25 em JDK 27 não comprova execução em JDK 25.
- Java legado usa `windows-1252` no POM. Preserve os bytes existentes; novos trechos Java podem usar ASCII/escapes Unicode. Documentação usa UTF-8.
- Não edite `target/` nem confunda o smoke nativo com teste de pixels.
- Registre indisponibilidade de MAILBOX/camada Khronos sem declarar aceite visual.

## Arquivos de referência

| Responsabilidade | Arquivo |
| --- | --- |
| Entrada e loop Vulkan | [Executor.java](src/br/com/engine/main/Executor.java), [LwjglVulkanExecutor.java](src/br/com/engine/main/LwjglVulkanExecutor.java) |
| Cenas, tempo e shutdown | [ControleBase.java](src/br/com/engine/core/ControleBase.java), [Time.java](src/br/com/engine/core/Time.java) |
| Lifecycle e mutações | [Scene.java](src/br/com/engine/core/Scene.java), [GameObject.java](src/br/com/engine/core/GameObject.java) |
| Contrato gráfico | [EngineGraphicsContext.java](src/br/com/engine/graphics/EngineGraphicsContext.java) |
| Submissão e apresentação | [LwjglVulkanFrameRenderer.java](src/br/com/engine/platform/lwjgl/LwjglVulkanFrameRenderer.java), [LwjglVulkanRenderSession.java](src/br/com/engine/platform/lwjgl/LwjglVulkanRenderSession.java) |
| Recursos e TMX | [ResourceManager.java](src/br/com/engine/resources/ResourceManager.java), [ContentLoader.java](src/br/com/engine/resources/ContentLoader.java), [TmxParser.java](src/br/com/engine/resources/TmxParser.java) |

## Regras de implementação

- Preserve os nomes públicos em português/inglês; evite renomear APIs por estilo.
- O backend ativo é exclusivamente Vulkan. Não reintroduza AWT, JavaFX ou Swing no renderer ou na API gráfica.
- Componentes recebem seu `GameObject` via `SimpleComponent`. Adicione comportamento com `addComponente`; não altere diretamente as listas expostas.
- Mutações de cena e componentes durante callbacks são diferidas. Teste adição/remoção durante setup, update, draw e descarte quando alterar esse mecanismo.
- `GameObject.setup()` é protegido contra repetição. Descarte é terminal e idempotente; objetos descartados não podem ser reutilizados.
- Registre listeners com dono (cena, objeto ou componente), ou feche o `InputSubscription` explicitamente. Não restaure limpeza global indiscriminada de mouse.
- Use o argumento de `fixedUpdate(float)` para física. `update(long)` recebe milissegundos limitados com preservação das frações entre frames. Não use deslocamento fixo por frame.
- Não remova a espera do fence antes de alterar buffers/texturas. O semáforo de renderização é por imagem da swapchain; o fence de submissão não prova conclusão da apresentação.
- Em buffers nativos, preserve o endereço original na liberação. Não retenha dados STB após copiar pixels para o heap.
- Recursos usam caminho relativo completo com extensão. Falhas devem preservar o nome e a causa; fallback de configuração só se aplica a arquivo ausente.
- Não descarte flags TMX silenciosamente. Funcionalidades fora do subconjunto implementado devem falhar explicitamente ou receber implementação e testes completos.
- O parser reconhece elipses, mas a colisão atual usa AABB; não descreva isso como geometria elíptica exata.
- Atualize o PRD quando um critério de aceite mudar e o blueprint quando o fluxo implementado mudar.

## Limites do escopo

A arquitetura continua baseada em singletons, listas de componentes e um frame GPU em voo. Interpolação visual, pools expansíveis, uploads assíncronos, retirement avançado de swapchain e Unicode completo permanecem no PRD. Não declare esses itens concluídos apenas porque o build passou.
