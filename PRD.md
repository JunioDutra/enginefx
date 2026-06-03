# PRD: Migracao completa da engine para Vulkan

## 1. Contexto

A `enginefx` ja foi migrada para remover JavaFX e atualmente possui dois caminhos de execucao relevantes:

- Backend padrao Swing/Java2D.
- Backend opt-in LWJGL/Vulkan, ativado por `--backend=vulkan`, `--vulkan` ou `-Denginefx.backend=vulkan`.

O backend Vulkan atual cria janela GLFW, instance Vulkan, surface, device, queues, swapchain e apresenta frames. Para manter compatibilidade rapida com o jogo, ele ainda usa Java2D para desenhar em um `BufferedImage` e depois copia esse framebuffer para o swapchain Vulkan.

O objetivo deste PRD e guiar a proxima fase: remover Java2D do caminho Vulkan e fazer a renderizacao 2D da engine acontecer nativamente em Vulkan, preservando as interfaces usadas pelo `dinofx`.

## 2. Referencias

- LWJGL: https://www.lwjgl.org/
- LWJGL Wiki: https://github.com/LWJGL/lwjgl3-wiki/wiki
- Vulkan Book para Java/LWJGL: https://github.com/lwjglgamedev/vulkanbook

Pontos relevantes das referencias:

- LWJGL fornece bindings diretos e de baixo nivel para APIs nativas como Vulkan, GLFW, STB e OpenAL.
- LWJGL nao e um framework de renderizacao pronto; a engine precisa implementar pipelines, buffers, texturas, sincronizacao e gerenciamento de recursos.
- O Vulkan Book descreve uma ordem incremental adequada: instance, devices, surface, swapchain, command buffers, sincronizacao, pipeline, shaders, texturas, memoria, GUI e audio.

## 3. Objetivos

1. Remover Java2D do backend Vulkan.
2. Implementar `VulkanGraphicsContext` como substituto nativo de `Java2DGraphicsContext` para o modo Vulkan.
3. Preservar as interfaces publicas existentes usadas pelo jogo:
   - `Executor.loadGame(args)`
   - `Scene`
   - `GameObject`
   - `IComponent`
   - `EngineGraphicsContext`
   - `ResourceManager`
   - `KeyBoard` e `Mouse`
4. Fazer o `dinofx` rodar com sprites, tiles, formas, texto e debug usando Vulkan no caminho de renderizacao.
5. Manter a migracao em marcos pequenos, sempre compilando `enginefx` e empacotando/rodando `dinofx` apos mudancas relevantes.

## 4. Nao objetivos

- Reescrever as cenas do `dinofx`.
- Trocar o modelo de componentes da engine.
- Introduzir JavaFX novamente.
- Criar um motor 3D completo nesta fase.
- Implementar ray tracing, deferred rendering ou recursos avancados antes da renderizacao 2D estar estavel.

## 5. Estado atual

Fluxo atual no backend Vulkan:

```text
Scene / GameObject / Componentes
    -> EngineGraphicsContext
    -> Java2DGraphicsContext
    -> BufferedImage da Screen
    -> LwjglVulkanBufferedImageRenderer
    -> staging buffer Vulkan
    -> swapchain image
    -> tela
```

Fluxo desejado:

```text
Scene / GameObject / Componentes
    -> EngineGraphicsContext
    -> VulkanGraphicsContext
    -> command buffers / pipelines / buffers / textures
    -> swapchain image
    -> tela
```

## 6. Escopo funcional

O backend Vulkan deve suportar as operacoes atuais de `EngineGraphicsContext`:

- `save()` e `restore()`
- `setFill(Paint)`
- `setStroke(Paint)`
- `fillRect(...)`
- `strokeRect(...)`
- `strokeOval(...)`
- `strokeRoundRect(...)`
- `drawImage(Image, x, y)`
- `drawImage(Image, source..., destination...)`
- `fillText(...)`
- `setFont(...)`
- `setTextBaseline(...)`
- `translate(...)`
- `getTransform()`

## 7. Escopo tecnico

### 7.1 Backend Vulkan nativo

Criar um contexto grafico Vulkan que implemente `EngineGraphicsContext`.

Componentes esperados:

- `VulkanGraphicsContext`
- pipeline 2D para quads coloridos
- pipeline 2D para quads texturizados
- command buffer por frame
- vertex/index buffers dinamicos
- gerenciamento de memoria para buffers e imagens
- descriptor sets para texturas
- cache de texturas por `Image`
- suporte a transformacao 2D simples

### 7.2 Texturas

O backend deve carregar imagens como `VkImage` e nao depender de `BufferedImage` no caminho de apresentacao.

Estrutura esperada:

- staging buffer para upload inicial
- `VkImage` device-local
- `VkImageView`
- sampler
- descriptor set por textura ou estrategia de atlas
- cache para evitar reupload por frame

### 7.3 Texto

Substituir `Graphics2D.drawString` por uma solucao Vulkan.

Opcoes aceitaveis:

- STB TrueType via LWJGL para gerar atlas de fonte.
- Bitmap font atlas pregerado.

Primeira implementacao recomendada:

- usar STB TrueType para gerar atlas em runtime
- renderizar cada caractere como quad texturizado
- respeitar `setFont`, `setFill` e `setTextBaseline` o suficiente para manter o comportamento atual

### 7.4 Formas e debug

Implementar formas simples por geometria:

- `fillRect`: quad colorido
- `strokeRect`: quatro quads finos ou linhas
- `strokeRoundRect`: inicialmente equivalente a `strokeRect` quando arcs forem zero; depois aproximar cantos
- `strokeOval`: aproximacao por segmentos

### 7.5 Transformacoes

Suportar no minimo translacao acumulada, pois a camera usa `translate(...)`.

Requisitos:

- pilha de estado para `save/restore`
- `getTransform().getTx()` e `getTransform().getTy()` coerentes com o comportamento atual
- aplicacao da transformacao nos vertices enviados ao Vulkan

## 8. Marcos de implementacao

### Marco 1: `VulkanGraphicsContext` minimo [completed]

Objetivo: remover Java2D da limpeza de tela e de retangulos basicos no modo Vulkan.

Entregas:

- criar `VulkanGraphicsContext implements EngineGraphicsContext`
- implementar `setFill`, `fillRect`, `translate`, `save`, `restore`, `getTransform`
- criar pipeline de quads coloridos
- integrar com `LwjglVulkanExecutor`
- manter `Java2DGraphicsContext` apenas para backend Swing, se o fallback continuar existindo

Criterios de aceite:

- `mvn compile` em `enginefx` passa
- `dinofx` empacota
- `java -jar target/dino.jar --backend=vulkan` abre e apresenta formas basicas sem usar `Java2DGraphicsContext` no caminho Vulkan

### Marco 2: sprites e imagens

Objetivo: renderizar sprites e tiles do `dinofx` por texturas Vulkan.

Entregas:

- upload de `Image` para `VkImage`
- cache de textura por instancia/caminho
- pipeline texturizado
- `drawImage(Image, x, y)`
- `drawImage(Image, source..., destination...)` com UVs

Criterios de aceite:

- spritesheets renderizam corretamente
- mapas TMX renderizam tiles visiveis
- nao ha upload de textura por frame para a mesma imagem

### Marco 3: batching 2D

Objetivo: reduzir overhead de draw calls, principalmente em mapas.

Entregas:

- acumular quads por textura
- flush por textura ou por lote
- buffers dinamicos redimensionaveis

Criterios de aceite:

- mapas com muitos tiles continuam fluidos
- execucao nao cria objetos Vulkan por tile a cada frame

### Marco 4: texto nativo Vulkan

Objetivo: substituir `fillText` sem Java2D.

Entregas:

- atlas de fonte via STB TrueType ou bitmap font
- renderizacao de caracteres como quads texturizados
- suporte inicial a `Font`, `Paint` e `VPos`

Criterios de aceite:

- FPS/debug text aparece
- textos de HUD e menus aparecem
- nenhuma chamada a Java2D e necessaria para texto no backend Vulkan

### Marco 5: formas restantes e debug

Objetivo: completar o contrato grafico usado pelos componentes de debug e primitivas.

Entregas:

- `strokeRect`
- `strokeRoundRect`
- `strokeOval`
- cor de stroke independente de fill

Criterios de aceite:

- debug de colisores e sprites aparece no modo Vulkan
- comportamento visual aceitavel comparado ao backend atual

### Marco 6: remover a ponte `BufferedImage -> Vulkan`

Objetivo: eliminar Java2D do backend Vulkan.

Entregas:

- `LwjglVulkanBufferedImageRenderer` deixa de ser usado pelo executor Vulkan
- `Screen.getFrameBuffer()` nao e usado pelo caminho Vulkan
- `LwjglVulkanExecutor` usa somente `VulkanGraphicsContext`

Criterios de aceite:

- busca por uso de `Java2DGraphicsContext` no caminho Vulkan nao encontra referencias
- `dinofx` roda em Vulkan com cenas, sprites, mapas, texto e debug

### Marco 7: decisao sobre fallback Java2D

Objetivo: decidir se Java2D permanece apenas como backend alternativo ou se sera removido da engine inteira.

Opcoes:

- Manter Java2D como fallback desktop simples.
- Remover Java2D completamente e tornar Vulkan o unico backend grafico.

Criterios de decisao:

- necessidade de compatibilidade com maquinas sem Vulkan
- complexidade de manutencao
- objetivo do projeto como engine educacional ou engine Vulkan-first

## 9. Criterios globais de aceite

O projeto sera considerado migrado para Vulkan nativo quando:

1. `dinofx` abrir com `--backend=vulkan`.
2. Sprites, mapas, formas e textos aparecerem sem depender de Java2D.
3. Input de teclado e mouse continuar funcionando via GLFW.
4. Audio continuar funcionando sem regressao.
5. `mvn compile` passar em `enginefx`.
6. `mvn package -DskipTests` passar em `dinofx`.
7. A busca por referencias Java2D no caminho Vulkan confirmar que ele nao usa:
   - `Java2DGraphicsContext`
   - `Graphics2D`
   - `BufferedImage` como framebuffer de apresentacao
8. O contrato `EngineGraphicsContext` continuar preservado para as cenas existentes.

## 10. Riscos

### Complexidade Vulkan

Vulkan exige controle explicito de memoria, sincronizacao, layouts de imagem, command buffers e lifetime de recursos.

Mitigacao:

- implementar em marcos pequenos
- validar com smoke apps
- manter recursos Vulkan bem encapsulados

### Texto

Texto e uma das partes menos diretas ao sair de Java2D.

Mitigacao:

- implementar atlas simples primeiro
- aceitar suporte inicial limitado de fonte
- evoluir para cache e layout melhor depois

### Performance inicial

Uma primeira implementacao pode funcionar, mas ainda nao ser eficiente.

Mitigacao:

- priorizar corretude visual primeiro
- adicionar batching no Marco 3
- evitar criar recursos Vulkan por frame

### Compatibilidade de hardware

Vulkan requer suporte do driver/GPU.

Mitigacao:

- manter fallback Java2D ate decisao final
- detectar falta de Vulkan e mostrar erro claro

## 11. Validacao recomendada por marco

Para cada marco relevante:

```powershell
Set-Location "c:\workspace\java-game-engine\enginefx"
& "C:\Users\juniodbl\AppData\Local\mise\installs\maven\3.9.16\apache-maven-3.9.16\bin\mvn.cmd" compile
& "C:\Users\juniodbl\AppData\Local\mise\installs\maven\3.9.16\apache-maven-3.9.16\bin\mvn.cmd" install -DskipTests

Set-Location "c:\workspace\java-game-engine\dinofx"
& "C:\Users\juniodbl\AppData\Local\mise\installs\maven\3.9.16\apache-maven-3.9.16\bin\mvn.cmd" package -DskipTests
java -jar .\target\dino.jar --backend=vulkan
```

## 12. Decisao tecnica recomendada

Comecar pelo `VulkanGraphicsContext` nativo, mas manter o backend Java2D como fallback temporario ate o Marco 6 estar completo.

Motivo:

- permite comparar visualmente os dois backends
- reduz risco de regressao enquanto sprites/texto/mapas estao sendo migrados
- preserva uma forma simples de rodar a engine durante a construcao do renderer Vulkan

Depois que o backend Vulkan renderizar tudo nativamente, a engine pode decidir se remove Java2D totalmente ou se o mantem como backend legado.