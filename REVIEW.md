# Review — EngineFX 2.0.0

Data: 10/09/2026. Escopo: todas as alterações pendentes, combinando index, working tree e arquivos novos, em relação a `c6287d6`. Inclui integração com o dinofx e organização da documentação.

## Resultado

Foram corrigidas regressões de recursos, memória nativa, lifecycle, física, texto, TMX e build. A migração está apta a este commit de implementação e correções, mas **o aceite visual/de driver ainda não está concluído**.

O [handoff original](docs/reviews/HANDOFF.md) foi preservado como histórico. Suas alegações de execução não substituem a validação abaixo.

## Falhas encontradas e corrigidas

| Prioridade | Falha | Correção e evidência |
| --- | --- | --- |
| P1 | JAR falhava fora do checkout ao procurar `font.ttf` | Caminho `fonts/font.ttf`, carregamento de debug só quando necessário; reproduzido antes, smoke aprovado depois |
| P1 | Liberação STB usava buffer após avanço da posição; workaround retinha alocações indefinidamente | Leitura absoluta preserva o ponteiro e liberação imediata em finally; 200 decodificações na regressão, sem retenção nativa |
| P1 | Imagens do JAR eram decodificadas novamente e a busca local por basename podia escolher outro asset | Resolvedor por caminho exato, cache por origem para filesystem/classpath; teste de identidade e smoke do cache |
| P1 | Remove/clear/dispose podiam ressuscitar objetos pendentes ou deixar listeners; falha interrompia cleanup | Mutações diferidas em todas as fases, cancelamento de adições, dono de input e descarte que continua após exceção; LifecycleTest/MouseTest |
| P1 | Física fixa não sincronizava posição anterior/filhos; seleção por identityHashCode podia omitir contatos e invocar collider errado | Checkpoint por passo, sincronização dos monitores, pares em ordem de cena e callbacks nos colliders do contato; regressões de rollback e contatos simultâneos |
| P2 | Milissegundos truncados em cada frame congelavam timers em FPS alto | Preservação do resto limitado; TimeTest cobre 2.000 frames de 0,5 ms e clamp |
| P2 | Fonte usava estimativa constante de largura e contava linhas/Unicode diferentemente do desenho | Avanços e altura STB compatíveis com o atlas; testes contra dados rasterizados |
| P2 | Construtores de SpriteFont carregavam fonte padrão mesmo quando havia fonte explícita | Delegação direta ao construtor com nome e tamanho finais |
| P2 | GREEN mudou de `#00ff00` para `#008000` sem requisito visual | Valor anterior preservado na API própria |
| P1 | TMX descartava flags e presumía todos os assets em `imagens/`; mapas antigos sem columns falhavam | Caminhos relativos ao TMX, colunas derivadas da imagem, ordem de renderização e limites validados; recursos não suportados falham explicitamente |
| P2 | Elipses não circulares eram reduzidas a um quadrado menor | AABB integral preservado; colisão elíptica exata continua fora do suporte atual |
| P1 | Crescimento de buffer podia entrar em laço com capacidade zero/após close; falha de seleção de memória vazava buffer | Validação de estado/capacidade, cleanup de alocações parciais e preservação do buffer anterior; teste de capacidade e crescimento em GPU |
| P2 | Falhas de áudio/JS podiam deixar streams/linhas ou ocultar o recurso causador | Cleanup e propagação com causa/caminho; testes de áudio inválido e SceneJs/Nashorn |
| P2 | Carregadores antigos ainda tinham resolvedores distintos e SceneJs ignorava a definição configurada | Fachadas depreciadas sobre ResourceManager; cena JS usa o recurso informado |
| P1 | build.ps1 usava JDK de uma máquina e podia instalar o projeto errado conforme CWD | JDK por ambiente, POM explícito, wrapper com checksum e build integrado validado da pasta pai |

## Validação executada

- Baseline: seis testes existentes passaram, mas o JAR falhou com `Resource not found: font.ttf` na pasta sem assets.
- Build final da engine: `.\mvnw.cmd clean install -B` — **27 testes, zero falhas/erros**.
- Jogo consumidor: **4 testes, zero falhas/erros** e JAR empacotado.
- Smoke FIFO: seis cenas, duas passagens, 120 frames/cena/passagem (**1.440 frames**), dois resizes por visita, assets do JAR, Nashorn, cache de imagens e crescimento do buffer GPU.
- Execução com `--enable-native-access=ALL-UNNAMED` e `--sun-misc-unsafe-memory-access=deny`.
- Ambiente: Windows x64, OpenJDK 27, LWJGL 3.4.3, AMD Radeon RX 9060 XT, Vulkan reportado 1.4.349, valor de driver reportado 8389003.
- Tentativa MAILBOX: falhou explicitamente porque o modo não é suportado pela superfície. Não foi contabilizada como smoke aprovado.
- Tentativa com Khronos/sync validation: falhou com `VK_ERROR_LAYER_NOT_PRESENT` (-6). Não há alegação de zero VUIDs.

## Reprodução

Com JDK configurado e checkouts irmãos, na pasta pai:

```powershell
.\enginefx\mvnw.cmd -f .\enginefx\pom.xml clean install
.\dinofx\build.ps1 -Smoke -PresentMode fifo
```

A engine usa Java 25 como release de compilação; a execução local foi no JDK 27. O fixture `tests/resources/res/fonts/test.ttf` é uma cópia da fonte já usada pelo jogo, exclusiva dos testes.

## Limites e próximos passos

Não foram implementados nesta revisão: goldens offscreen, retirement por fences de apresentação, múltiplos frames em voo, pools expansíveis, uploads assíncronos/VMA, Unicode completo ou solver físico abrangente. O smoke não joga automaticamente nem compara pixels.

Esses itens e os critérios de aceite estão em [PRD.md](PRD.md). O [BluePrint.md](BluePrint.md) documenta apenas o fluxo implementado. O conjunto principal agora usa `README.md`, `AGENTS.md`, `BluePrint.md` e `PRD.md`; o blueprint antigo foi substituído, mantendo seu histórico no Git.

Referências para a correção de memória/texto: [LWJGL STBImage](https://javadoc.lwjgl.org/org/lwjgl/stb/STBImage.html), [MemoryUtil](https://javadoc.lwjgl.org/org/lwjgl/system/MemoryUtil.html) e [STBTruetype](https://javadoc.lwjgl.org/org/lwjgl/stb/STBTruetype.html). As regras de apresentação continuam seguindo as fontes Khronos citadas na auditoria original.
