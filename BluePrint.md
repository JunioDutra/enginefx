# BluePrint — EngineFX

Atualizado em 10/09/2026. Descreve o código implementado; trabalho futuro está em [PRD.md](PRD.md).

## Arquitetura e dependências

Engine 2D monolítica com composição de componentes. Java 25, Maven, LWJGL 3.4.3 (Vulkan/GLFW/STB/shaderc), Java Sound, Nashorn 15.7, Gson e Commons Lang. JUnit 5 cobre o núcleo e contratos nativos sem criar um dispositivo Vulkan.

```mermaid
flowchart TD
    Game["Jogo: configuração, cenas e assets"] --> Executor
    Executor --> Loop["LwjglVulkanExecutor"]
    Loop --> Control["ControleBase / Time"]
    Control --> Scene
    Scene --> Objects["GameObject / IComponent"]
    Objects --> Draw["EngineGraphicsContext"]
    Draw --> Commands["VulkanGraphicsContext: comandos ordenados"]
    Commands --> Session["LwjglVulkanRenderSession"]
    Session --> Renderer["FrameRenderer / pipeline / buffers / caches"]
    Renderer --> GPU["Vulkan / swapchain"]
    Objects --> Resources["ResourceManager / ContentLoader"]
    Resources --> Assets["Filesystem ou classpath res/"]
    Loop --> Input["GLFW → KeyBoard / Mouse"]
    Input --> Objects
```

Não há container de injeção, ECS de dados, barramento de eventos geral, servidor ou banco de dados. `ControleBase`, `Time`, teclado e mouse são serviços globais. Alguns componentes acessam diretamente o controlador: o contrato gráfico limita exposição de Vulkan ao jogo, mas as camadas não são totalmente independentes.

## Organização

| Pacote | Responsabilidade |
| --- | --- |
| `main` | Bootstrap, loop desktop e executáveis de smoke |
| `core` | Controlador, cenas, objetos, vetores, relógio e cenas JSON/JS |
| `graphics` | Contrato de desenho e dados próprios de cor, imagem e fonte |
| `platform.lwjgl` | Janela, instance/device, swapchain, shaders, pipeline, caches e buffers |
| `componentes` | Sprites, texto, mapas, áudio, scripts, câmera, física e debug |
| `resources` | Configuração, resolução de assets, cache e parser TMX |
| `input` | Estado do teclado e inscrições de mouse com dono |
| `fisica`, `geometry`, `interfaces` | Detecção AABB, primitivas e contratos de extensão |

Fontes de produção ficam em `src/br/com/engine`; testes, em `tests/br/com/engine`. O POM configura essa disposição explicitamente.

## Inicialização e cenas

1. `Executor.loadGame` seleciona Vulkan e inicia `LwjglVulkanExecutor`.
2. O launcher configura FFM antes de inicializar LWJGL, instala callback de erro GLFW e carrega a configuração.
3. O controlador cria `Screen`; o launcher associa `VulkanGraphicsContext` e abre instance, janela, device e sessão de renderização.
4. `ControleBase.setup` cria loading, registra definições e identifica a cena `@Bootable`.
5. `nextScene(index)` agenda a troca. Na próxima iteração, a cena anterior é descartada, uma nova instância é criada, a câmera/transformação e o acumulador de física são reiniciados.
6. O encerramento fecha owners Vulkan, descarta a cena, limpa caches Java e termina GLFW.

Falhas de setup são propagadas e o estado parcialmente criado é descartado. A engine recria a instância ao revisitar uma cena; persistência entre visitas precisa de estado fora dela.

## Tempo, componentes e colisões

Cada iteração consulta eventos, executa lógica, coleta comandos e apresenta. `Time.update` limita o delta a 250 ms. Milissegundos legados preservam o resto de nanossegundos para não congelar temporizadores em FPS elevado.

Após `update`, o controlador acumula tempo para `fixedUpdate(1/60f)`, limitado a cinco passos por frame. Tempo excedente de física é descartado para evitar recuperação ilimitada. `Time.getInterpolationAlpha()` expõe a fração restante, mas os sprites ainda não interpolam posições.

`GameObject` possui componentes, filhos e posição aceita no último passo físico. `VectorMonitor` sincroniza câmera e propagação para filhos; componentes novos têm setup uma vez. Objetos/componentes adicionados durante callbacks entram ao concluir a iteração; remoção impede uso posterior e chama descarte.

A cena detecta contatos antes da resposta, percorrendo pares de objetos em ordem de inserção. Para cada par, `Colisao` retorna o primeiro par de colliders sobrepostos; a resposta retorna posições ao último estado aceito e invoca os colliders efetivamente envolvidos. É um mecanismo AABB discreto e simples, sem solver de impulsos, varredura contínua ou tratamento completo de todos os colliders de um mesmo par.

Descarte remove inscrições de mouse pertencentes à cena, ao objeto e aos componentes, inclui adições pendentes e continua a limpar os demais recursos se um callback falhar. Exceções adicionais são preservadas como suprimidas.

## Desenho e sincronização

`VulkanGraphicsContext` registra comandos de imagem, texto, retângulo e quad, incluindo cor e estado de transformação. O renderer converte a lista em vértices e agrupa apenas draws consecutivos que compartilham textura, preservando a ordem de transparência.

- Um frame em voo; fence aguardado antes de reescrever o buffer ou liberar texturas.
- Aquisição com semáforo próprio; `VkSubmitInfo` define explicitamente a contagem de espera.
- Semáforo de conclusão por imagem da swapchain.
- Reset do fence imediatamente antes do submit.
- OUT_OF_DATE/SUBOPTIMAL e mudança no framebuffer provocam recriação pela sessão.
- A sessão recria renderer, pipeline e caches junto com a swapchain.
- O buffer de vértices cresce por duplicação e preserva a alocação anterior se a nova falhar; limite explícito inferior a 2 GiB.
- Texturas ausentes do frame são descartadas após a espera. Upload de imagens ainda usa espera de fila; o pool de descritores tem capacidade fixa.
- Aposentadoria da swapchain usa `vkDeviceWaitIdle`; fences de apresentação via maintenance1 ainda não estão implementados.

O shader converte RGB sRGB para linear ao escrever em attachment sRGB; alpha permanece linear. O caminho UNORM mantém a composição legada. Esses contratos possuem testes estruturais, mas ainda faltam comparação de pixels e aceite visual.

## Recursos, texto e áudio

`ResourceManager` expõe APIs tipadas. `ContentLoader` resolve caminho exato em três raízes locais e depois em `/res/` do classpath. Imagens são compartilhadas por origem; fontes por origem/tamanho. Áudio e scripts possuem instâncias próprias porque carregam estado mutável.

STB decodifica RGBA para memória nativa, copia para `Image` imutável no heap e libera a alocação original imediatamente. O renderer recebe cópias RGBA para staging. `Font` mede os avanços com STB; o cache de rasterização usa os mesmos glifos e escala. O atlas é Latin-1 (32–255), com fallback `?` por code point e suporte a nova linha; Unicode completo, kerning e shaping não existem.

`AudioEffect` é dono do `AudioClip` e fecha a linha no descarte. Streams e linhas parcialmente abertas também são fechados em falhas. Java Sound permanece como API de áudio, embora a API gráfica não dependa de AWT.

Os antigos `PropertiesLoader`, `ScenesLoader` e `ConfigurationsManager` são fachadas depreciadas do resolvedor comum. `SceneJs` recebe caminhos de definições JSON e scripts com extensão; Nashorn vem como dependência explícita.

## TMX

O parser StAX aceita mapas finitos ortogonais, tilesets inline com imagem, layers base64 com zlib opcional, visibilidade e objetos retangulares/elípticos. Colunas podem vir do atributo ou da largura da imagem em mapas antigos. A ordem right/left e up/down orienta os draws.

Imagens são resolvidas relativamente ao arquivo TMX. O renderer valida os limites do tile na imagem e alinha tiles altos pela base da célula. Os objetos de colisão usam AABB completo, inclusive elipses.

TSX externo, flags de flip/rotação, grupos, image layers, offsets, opacidade parcial e formas não suportadas falham explicitamente. Não há promessa de compatibilidade geral com Tiled.

## Extensão e testes

Para adicionar comportamento, derive `SimpleComponent`, use setup para obter dependências e fixedUpdate para física, e libere inscrições/recursos em dispose. Para um novo comando gráfico, altere o contrato, o registro e o renderer, preservando ordem e regras de sincronização.

Testes locais cobrem lifecycle, tempo, input, recursos, scripts, áudio inválido, métricas STB, TMX, crescimento de capacidade, seleção de present mode, shader e estrutura de submissão. O harness do `dinofx` cobre o caminho GPU, empacotamento, troca de cenas, resize e crescimento nativo do buffer. Os limites e resultados da máquina estão em [REVIEW.md](REVIEW.md).

Atualize este arquivo quando mudar o fluxo de cenas, as regras de recursos, o contrato gráfico ou ownership de memória. Critérios ainda não demonstrados permanecem no PRD.
