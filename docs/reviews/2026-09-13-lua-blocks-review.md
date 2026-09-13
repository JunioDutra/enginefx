# Revisão dos blocos Lua 1B/1C — EngineFX 3.0

Data: 13/09/2026. Revisão confrontada com `../PLANO_FARMFX.md` e `../PLANO_DESENVOLVIMENTO.md` na raiz compartilhada, com o estado dos três repositórios e com os requisitos de cada `AGENTS.md`.

## Resultado e escopo

A EngineFX 3.0 integra Lua 5.4 e remove as APIs JavaScript/Nashorn. As correções desta revisão cobrem a fronteira de dados, orçamento e lifecycle Lua, resolução de módulos/recursos e criação de cenas. O DinoFX demonstra Lua no JAR; o FarmFX passa a consumir a mesma versão da engine. Catálogo Lua do FarmFX, execução de mods e Native Image integrado continuam em etapas posteriores.

## Problemas corrigidos

| Prioridade | Problema reproduzido ou confirmado no código | Correção e evidência |
| --- | --- | --- |
| Alta | `ScriptValue` previamente criado escapava dos limites mais estritos; chaves e retornos de APIs do host não eram revalidados. | Revalidação nas duas direções, incluindo valores aninhados e chaves. `LuaBoundaryTest` reproduziu a falha antes da correção. |
| Alta | A conversão de tabelas materializava a estrutura antes de contar nós e não identificava ciclos pela identidade real da tabela Lua. | Travessia da pilha com limites durante a leitura, comparação de identidade Lua e restauração da pilha em falhas; árvores grandes e ciclos são rejeitados sem inutilizar a instância. |
| Alta | Busca de callback via `__index` ocorria fora do orçamento e duas vezes no lifecycle. Reentrada do host reiniciava o orçamento; fechamento durante callback podia liberar o estado ativo. | Busca única em chamada protegida, orçamento compartilhado com busca e descarte, reentrada e fechamento ativo rejeitados. Regressões cobrem loop em `__index`, descarte sob limite e chamada recursiva. |
| Alta | Conversão completa Java/Lua perdia nulos internos e a distinção de lista vazia/mapa. | Protocolo de dados com `engine.value.null` e `engine.value.list({})`, exclusivamente tabelas/escalares Lua. Roundtrip de listas vazias, nulos iniciais/finais, mapas e chaves vazias coberto. |
| Média | Strings Lua inválidas viravam texto substituído silenciosamente; Java aceitava surrogate isolado. | UTF-8/Unicode estrito, limites em bytes para valores e chaves. |
| Média | Carregar A dependente de B já carregado com dependência C tentava recarregar B sem suas dependências e falhava. | Preservação dos módulos já carregados; teste A → B → C. Declaração de dependências continua sendo carregamento/validação de fontes, sem importação automática. |
| Média | `ControleBase.setup` chamava factories de todas as cenas e criava novamente a cena visitada; boot com alias era resolvido em apenas um sentido. | Factories só executam por visita; ambos os lados do boot são resolvidos. Fallback `@Bootable` consulta a classe sem construí-la/inicializá-la. Testes contam criação e descarte. |
| Média | Alias podia ser registrado depois como id; nomes de API podiam colidir com globais, palavras reservadas e caminhos de funções. | Rejeição antecipada de colisões de registro. O wildcard de capacidades `*`, já reconhecido por `allows`, também é aceito no contexto. |
| Média | `LuaComponent` podia ser recriado após descarte e manter a instância depois de falha em setup. | Descarte terminal/idempotente e limpeza em falha, preservando exceções. Teste confirma callback de descarte e recusa de reuso. |
| Média | Identidade textual de recurso permitia delimitadores ambíguos; duas grafias do mesmo caminho em memória sobrescreviam conteúdo. | Delimitadores/controles proibidos na identidade e duplicatas após normalização rejeitadas. |
| Média | FarmFX ainda dependia de 2.1.0, apesar de seu build instalar a engine irmã 3.0.0. | POM e README alinhados à 3.0.0; validação do consumidor registrada em seu review. |
| Baixa | Exemplo de migração fechava o runtime imediatamente após anexar o componente; smoke parcial DinoFX exigia dois descartes Lua mesmo quando não visitava a cena Lua. | Guia corrigido para ownership pela cena; smoke contabiliza somente visitas efetivas. Demo protege setup repetido. |

## Validação

Ambiente: Windows x64, OpenJDK 25.0.2, Maven Wrapper 3.9.16, LuaJava 4.1.0/Lua 5.4 e LWJGL 3.4.3. Maven rodou offline com dependências já instaladas. Não houve publicação remota.

- Baseline antes das correções: 44 testes verdes.
- Primeiras oito regressões: cinco falhas e um erro antes das correções; todas passaram depois. A hipótese de exposição de objeto Java por erro capturado não se confirmou e permanece coberta.
- A regressão posterior de nulos/listas reproduziu perda de dados. A falha inicial do contador de descarte no teste de cenas era do próprio fixture, que não respeitava idempotência; o fixture foi corrigido sem alterar o contrato anterior de `onCallChange`.
- `mvnw.cmd -B -o clean install`: **61 testes, zero falhas, erros ou ignorados**, JAR 3.0.0 instalado localmente. O README prescreve esse build; `target` foi verificado como diretório gerado, sem links nem arquivos rastreados.
- `dinofx/build.ps1 -Smoke -PresentMode fifo`: **5 testes e 1.680 frames**, sete cenas em duas passagens, Lua empacotada, descarte, cache, resize e crescimento do buffer GPU. AMD Radeon RX 9060 XT, Vulkan 1.4.349, driver 8389003, FFM com Unsafe negado.
- Busca nos fontes de produção: zero APIs ativas Nashorn/JS; LuaJava permanece exclusivamente em `scripting.internal`. Inspeção dos JARs da engine e do DinoFX: sem `.js`, classes JS removidas ou entradas Nashorn; runtime Lua presente e módulo de demo no JAR consumidor.

As regressões são versionadas em `tests/br/com/engine/scripting/LuaBoundaryTest.java`, `core/SceneRegistryLifecycleTest.java` e `core/LuaSceneTest.java`. Logs locais desta execução ficam em `../.review/blocos4-5/` na raiz compartilhada; resultados Surefire em `target/surefire-reports`. Artefatos gerados não são fonte de verdade nem entram no commit.

## Rastreabilidade e limites

- **1B:** contrato JVM integrado, limites de dados e lifecycle revisados, recursos qualificados e registro explícito disponíveis. O limite de instruções não é limite de heap nem timeout de funções nativas ou de APIs Java. O modelo continua sendo conteúdo local autorizado; não é uma barreira forte para código hostil.
- **1C:** fixture migrada, Nashorn removido, DinoFX em 3.0 e Lua validada fora do checkout. O [guia de migração](../javascript-to-lua.md) descreve ownership, valores e callbacks.
- **1D:** pendente. Embora a redação da etapa 1B associe a remoção reflexiva à versão 3.0, a etapa 1D a lista explicitamente como trabalho próprio. Esta revisão mantém o fallback legado e não declara encerrada a 1D. Também faltam os adaptadores/configuração e os contratos Native Image integrados previstos nessa etapa.
- O gate isolado Lua/Native Image 1A mantém sua evidência histórica; não foi reexecutado nesta revisão e não prova o runtime integrado, renderer ou áudio nativos da 3.0.
- O overload TMX resolve e armazena o mapa por pacote; composição/renderização de mapas de conteúdo FarmFX continua futura. Fontes e recursos são confiados ao pacote/host, sem quota geral de memória.
- Não foram feitos teste visual por pixels, gameplay manual, MAILBOX, validation layers, instalação sem cache, testes prolongados ou distribuição em outros sistemas.
