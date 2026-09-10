> Auditoria original preservada como histórico. O estado atualizado, as correções desta rodada e as limitações do ambiente estão em [REVIEW.md](REVIEW.md) e [PRD.md](PRD.md). Algumas descrições abaixo refletem o código anterior à implementação 2.0.0 revisada.

# Auditoria da migração Vulkan

Data: 10/09/2026. Projetos: `enginefx` e `dinofx`.

## Resultado e limites

Foi identificado um defeito de sincronização diretamente compatível com o relato de partes da tela piscando, inclusive em cenas paradas: `VkSubmitInfo.waitSemaphoreCount` permanecia zero. A submissão preenchia os ponteiros, mas não esperava a aquisição da imagem. O defeito foi corrigido e recebeu um teste sobre a estrutura nativa efetivamente usada pelo renderer.

A ausência visual do flicker ainda precisa ser confirmada no cenário relatado. Execução bem-sucedida e testes de estruturas não substituem comparação de imagens nem validação do driver. A camada `VK_LAYER_KHRONOS_validation` não está instalada na máquina: a execução solicitando essa camada retornou `VK_ERROR_LAYER_NOT_PRESENT` (-6). Não há alegação de execução sem VUIDs.

O caminho de apresentação é exclusivamente GLFW + Vulkan; não há fallback JavaFX/Swing/OpenGL. Ainda existem tipos e utilitários AWT para imagens, cores, métricas de fontes e TMX. Isso não constitui outro renderer, mas significa que a remoção integral das dependências Java2D ainda não terminou.

## Evidências e correções

| Prioridade | Problema identificado | Alteração |
| --- | --- | --- |
| P0 | Contagem de semáforos de espera igual a zero em `LwjglVulkanFrameRenderer` | `createSubmitInfo` define explicitamente `waitSemaphoreCount(1)`; teste verifica ponteiros, contagens e estágio de espera. |
| P1 | Um único semáforo de apresentação era reutilizado com base apenas no fence de submissão | Um semáforo por imagem da swapchain, indexado pela imagem adquirida. |
| P1 | Fence era resetado antes da aquisição; OUT_OF_DATE/SUBOPTIMAL encerravam a aplicação | Reset somente antes de submeter; tratamento dos retornos e `LwjglVulkanRenderSession` para recriar recursos dependentes da superfície. |
| P1 | Buffer truncava o upload, mas os draws conservavam a contagem original | Rejeição explícita de uploads inválidos; nenhum draw pode continuar com dados truncados. Crescimento automático continua no plano. |
| P1 | Recursos procurados apenas no filesystem relativo ao diretório de execução | JAR empacota assets em `res/`; extração temporária controlada preserva caminhos relativos para TMX, fontes e áudio. Arquivos externos continuam podendo sobrepor assets pelo nome relativo. |
| P1 | Nashorn era requisitado sem provedor no Java moderno | Dependência explícita `nashorn-core` 15.7 e merge de `META-INF/services` no JAR. |
| P1 | Cada tile recebia novo wrapper de imagem; ordem baseada em HashMap; colisores nulos em elipses | Reuso de imagens de tiles, lista ordenada e filtro de colisores nulos. Elipses reais permanecem pendentes. |
| P1 | Texturas e uploads acumulavam memória ao longo da execução | Imagens carregadas são compartilhadas; fontes usam chave por arquivo/tamanho; staging é liberado após upload; texturas fora do frame são liberadas após aguardar o fence. |
| P1 | RGB já codificado em sRGB era escrito diretamente no attachment sRGB, sofrendo nova codificação | Shader converte RGB de textura UNORM e cor para linear antes de escrever em attachment sRGB. Alpha permanece linear. Fallback UNORM conserva composição no espaço legado. |
| P2 | Alpha de origem era multiplicado por si próprio | Fator de alpha de origem alterado para ONE; RGB mantém SRC_ALPHA. |
| P2 | Dimensão física da janela confundida com coordenadas lógicas | Consulta ao framebuffer, projeção com tamanho lógico e conversão das coordenadas do mouse. |
| P2 | Seleção de dispositivo verificava apenas filas | Verifica também extensão swapchain, formatos e modos de apresentação; composite alpha escolhido entre os suportados. |
| P2 | Command buffers de upload e callbacks GLFW não eram liberados; falha de shader vazava compilador | Liberação após conclusão do upload, callbacks antes da janela e cleanup do compilador em `finally`. |
| P2 | Renderer ignorava acentos e quebras de linha; tamanho da fonte era multiplicado por 1,5 | Atlas Latin-1, nova linha e tamanho solicitado; caracteres fora do atlas recebem `?`. Métricas unificadas/Unicode completo seguem no plano. |
| P2 | `SpriteFont(int)` e `SpriteFont(String)` não recarregavam a fonte depois de trocar tamanho/nome | Construção passa a refletir os parâmetros solicitados. |
| P2 | Índices inválidos e falhas de setup de cenas eram mascarados | Limites explícitos, propagação da falha e reflexão com `getDeclaredConstructor().newInstance()`. |
| P2 | Câmera de `TiledMapGame` movia quantidade fixa por frame | Velocidade de 600 unidades/segundo usando delta time. |

LWJGL foi atualizado de 3.3.6 para 3.4.3. O launcher escolhe o backend de memória FFM antes de inicializar LWJGL, preservando override explícito via propriedade. O JAR mantém o manifesto Multi-Release. O target de compilação permanece Java 25; a execução local foi feita no OpenJDK 27 instalado. Compatibilidade com outros JDKs/GPU/OS requer a matriz de testes abaixo.

## Validação executada

- Build Maven da engine e empacotamento do jogo.
- Teste `VulkanSubmissionTest`: inspeciona a estrutura nativa da submissão, sem GPU.
- Teste `VulkanShaderCompilerTest`: verifica falha explícita de shader inválido e compilação válida posterior, conferindo o cabeçalho SPIR-V. Os dois testes passaram.
- Smoke Vulkan básico: 120 frames, swapchain com 3 imagens, formato 50 (B8G8R8A8_SRGB), filas graphics/present 0.
- `GameMigrationSmokeApp`: duas passagens por Menu, Spaceship, QuedaLivre, Level001, Level002 e TiledMapGame; 120 frames por cena, total de 1.440 frames; duas mudanças de tamanho por visita.
- Smoke executado de uma pasta vazia, com classes do harness de teste e JAR empacotado no classpath. Assets e provedor Nashorn vieram do JAR; o script `6 * 7` retornou 42.
- A mesma sequência passou com LWJGL 3.4.3, FFM e `--sun-misc-unsafe-memory-access=deny`.

O harness não joga automaticamente nem verifica cada pixel. Não cobre todas as combinações de teclas, colisões, perda de superfície, device lost, falta de memória, múltiplas GPUs ou mudanças de DPI entre monitores. Foram inspecionados o caminho Vulkan, abstrações gráficas, recursos, controle de cenas, componentes, entrada, física e os exemplos; isso não é uma prova de ausência de outros defeitos.

## Como reproduzir

Com Java 25+ e Maven disponíveis, na pasta que contém os dois projetos:

```powershell
mvn -f enginefx/pom.xml install
mvn -f dinofx/pom.xml clean package
java --enable-native-access=ALL-UNNAMED -jar dinofx/target/dino.jar
```

O Maven usado localmente está em `../.tools/apache-maven-3.9.16/bin/mvn.cmd`; foi baixado do Maven Central e teve SHA-512 conferido. Essa ferramenta local não faz parte de nenhum dos dois repositórios.

Smoke de integração, a partir de qualquer pasta sem assets externos:

```powershell
java --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=deny '-Denginefx.smoke.hidden=true' -cp 'C:/workspace/engine/dinofx/target/test-classes;C:/workspace/engine/dinofx/target/dino.jar' br.com.game.GameMigrationSmokeApp
```

Após instalar as ferramentas de validação do Vulkan SDK:

```powershell
$env:VK_LAYER_VALIDATE_SYNC = '1'
java --enable-native-access=ALL-UNNAMED '-Denginefx.vulkan.validation=true' -jar C:/workspace/engine/dinofx/target/dino.jar
```

Propriedades `-D` devem ficar entre aspas no PowerShell. O modo de validação falha explicitamente se a camada solicitada não estiver disponível.

## Plano de conclusão e modernização

### 1. Fechar a aceitação visual e Vulkan — prioridade imediata

- Repetir as cenas afetadas paradas e animadas, em FIFO e MAILBOX, com vídeo antes/depois e identificação de GPU/driver.
- Executar validação Khronos e sincronização por pelo menos 5 minutos, incluindo minimizar/restaurar, resize e troca de monitores. Aceite: zero erros de uso Vulkan e nenhum flicker reproduzível.
- Adicionar teste de renderização offscreen com leitura dos pixels para cores, alpha, sprites, fontes e ordem dos draws. Essa é a proteção contra regressões visuais que falta aos testes atuais.
- Usar fences de apresentação de `VK_EXT_swapchain_maintenance1` ou equivalente suportado para aposentar swapchains com garantia explícita. O cleanup atual mantém o padrão básico `vkDeviceWaitIdle`, que não constitui prova formal de conclusão da apresentação em Vulkan sem extensões.

### 2. Terminar o desacoplamento do legado — prioridade alta

- Substituir `java.awt.Color`, `BufferedImage` e `Graphics2D` na API pública por dados próprios de RGBA/imagem/métricas. Manter decoders e integração TMX em adaptadores internos até a substituição de libtiled.
- Unificar medição e rasterização de fontes; hoje a medição usa AWT e os glifos usam STB. Suportar Unicode e baseline/alinhamento completos, com fonte padrão empacotada em vez de caminho fixo do Windows.
- Consolidar todos os carregadores antigos (`PropertiesLoader`, `ScenesLoader`, `ResourceManager`, `SceneJs`) no mesmo resolvedor, com streams fechados e erros que preservem causa e nome do recurso.
- Adicionar ciclo de descarte para áudio e recursos das cenas. `AudioClip` atualmente pode ignorar falha de áudio e não oferece liberação explícita; não confundir silêncio com sucesso.
- Aceite: exemplos executam fora do checkout, fontes/sons/mapas usam os mesmos caminhos em IDE e JAR, e nenhum componente de jogo depende de classes de janela ou dispositivo Vulkan.

### 3. Consolidar o modelo de componentes e tempo — prioridade alta

- Preservar `setup/update/draw` e adicionar descarte explícito, inscrição de input com dono e mutações diferidas de componentes/objetos.
- Corrigir `Scene.removeAll/clearScene` para respeitar a iteração e proteger o lock com `try/finally`; impedir setup repetido e registros duplicados.
- Rever `AndarEmTile`: movimento ainda depende de frames e a lógica de chegada usa sinais que prejudicam deslocamentos negativos. `JumpSC` e `Gravity` também precisam migrar suas unidades.
- Unificar o delta limitado de `Time` com o `long time` legado, hoje acumulado sem o mesmo limite; definir passo fixo de física e interpolação visual.
- Separar detecção e resposta de colisão. Os testes atuais dos exemplos não validam estabilidade física, pares simultâneos, elipses, tiles transformados ou velocidades altas.
- Aceite: mesmos deslocamentos a 30/60/144 Hz, troca repetida de cenas sem listeners residuais e ausência de mutação concorrente de listas durante callbacks.

### 4. Escalar o renderer sem expor Vulkan aos jogos

- Crescimento controlado do vertex buffer e de pools de descritores; limites atuais: 65.536 quads e 1.024 texturas residentes num frame.
- Uploads agrupados e assíncronos com staging compartilhado, em vez de `vkQueueWaitIdle` para cada imagem; alocação em blocos/VMA com orçamento e métricas.
- Separar recursos persistentes de recursos da swapchain: atualmente resize recria renderer, pipeline e caches. Só depois introduzir mais frames em voo, duplicando buffers graváveis por frame.
- Tratar limpeza de construções parcialmente bem-sucedidas e erros de driver em todos os owners; inserir nomes de debug em objetos Vulkan.
- Manter ordem de transparência ao agrupar draws; considerar instancing/atlas e culling por câmera após medir gargalos.
- Avaliar Vulkan 1.3/1.4, synchronization2 e dynamic rendering por capacidade detectada. Usar render pass Vulkan 1.0 não é, por si, defeito nem exige outra API gráfica.
- Aceite: memória estabiliza em testes longos, resize não recarrega assets e métricas de CPU/GPU justificam cada otimização.

### 5. Entrega reproduzível e ergonomia

- Maven Wrapper, versões de plugins fixadas e CI em JDK 25 e versões atuais; runner com GPU/lavapipe para testes de renderização e driver real para apresentação.
- Resolver duplicações JAXB/Activation atualmente relatadas pelo shade; atualizar dependências separadamente com testes de mapas/áudio/scripts.
- Scripts únicos de build/run/test e empacotamento de runtime via jpackage; perfis explícitos de nativos por OS/arquitetura.
- Menu gerado pelas definições de cena, fixtures de componentes e documentação de criação de um minijogo sem conhecimento de Vulkan.
- Aceite: checkout limpo produz o executável com um comando e um novo jogo só fornece configuração, assets, cenas e componentes.

## Referências primárias consultadas

- [Khronos — reutilização de semáforos da swapchain](https://docs.vulkan.org/guide/latest/swapchain_semaphore_reuse.html): lifetime de apresentação e distinção em relação ao fence de submissão.
- [LWJGL — TriangleDemo.java](https://github.com/LWJGL/lwjgl3-demos/blob/main/src/org/lwjgl/demo/vulkan/TriangleDemo.java): exemplo Java define explicitamente `waitSemaphoreCount`; serve de referência de binding, não de arquitetura de produção.
- [LWJGL — VkSubmitInfo gerado](https://github.com/LWJGL/lwjgl3/blob/master/modules/lwjgl/vulkan/src/generated/java/org/lwjgl/vulkan/VkSubmitInfo.java): ponteiros e contador são campos independentes.
- [Khronos — recriação da swapchain](https://docs.vulkan.org/tutorial/latest/03_Drawing_a_triangle/04_Swap_chain_recreation.html): OUT_OF_DATE, SUBOPTIMAL, framebuffer e reset de fence.
- [Khronos — visão geral da validação](https://docs.vulkan.org/guide/latest/validation_overview.html) e [sincronização](https://docs.vulkan.org/guide/latest/synchronization.html).
- [Khronos — formatos](https://docs.vulkan.org/spec/latest/chapters/formats.html) e [espaços de cor](https://docs.vulkan.org/refpages/latest/refpages/source/VkColorSpaceKHR.html): distinção UNORM/sRGB e codificação de saída.
- [LWJGL 3.4.3](https://github.com/LWJGL/lwjgl3/releases/tag/3.4.3), [FFM](https://github.com/LWJGL/lwjgl3/blob/3.4.3/doc/FFM.md) e [configuração do backend](https://github.com/LWJGL/lwjgl3/blob/3.4.3/modules/lwjgl/core/src/main/java/org/lwjgl/system/Configuration.java).
- [OpenJDK Nashorn](https://github.com/openjdk/nashorn): provedor standalone para JDKs modernos.
- [VulkanBook em Java](https://github.com/lwjglgamedev/vulkanbook): referência complementar de organização e exemplos; a especificação Khronos prevalece para regras de sincronização.
