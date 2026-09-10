# PRD — EngineFX

Atualizado em 10/09/2026. Este documento acompanha o estado implementado e os próximos passos. A auditoria original está em [VULKAN_MIGRATION_REVIEW.md](VULKAN_MIGRATION_REVIEW.md); resultados de execução estão em [REVIEW.md](REVIEW.md).

## Objetivo

Fornecer uma engine 2D desktop em Java que permita ao jogo definir configuração, assets, cenas e componentes sem programar Vulkan. A migração deve preservar comportamento visível, estabilidade de tempo e liberação de recursos, com build reproduzível.

## Andamento

| Frente | Estado | Evidência / limite |
| --- | --- | --- |
| Backend gráfico Vulkan exclusivo | Implementado | GLFW, comandos 2D e pipeline nativo; nenhum import AWT/Swing/JavaFX nos fontes |
| Tipos gráficos próprios | Implementado | RGBA, Image, Font e IntPoint; fonte medida e rasterizada por STB |
| Recursos em IDE e JAR | Implementado e exercitado | Resolução exata, cache compartilhado, fontes/áudio/TMX/scripts pelo mesmo resolvedor |
| Lifecycle e input com dono | Implementado e testado | Setup, mutações diferidas, descarte terminal e liberação de listeners |
| Relógio e física fixa | Parcial | Passo 60 Hz e testes do núcleo; interpolação visual e solver físico completo pendentes |
| Correções de submissão/apresentação | Parcial | Fence/semafóros e resize exercitados em FIFO; retirement formal de swapchain pendente |
| Crescimento de buffer | Implementado e exercitado | Duplica capacidade com limite e cleanup de falha |
| Build e documentação | Implementado | Wrapper, scripts independentes do CWD, testes e quatro documentos principais |
| Aceite visual e de driver | Pendente | Sem goldens offscreen; camada Khronos indisponível; MAILBOX não suportado na superfície local |

## Próximos passos, em ordem

### 1. Fechar a validação visual e Vulkan

- Instalar/disponibilizar a camada Khronos em ambiente de teste e rodar validação de sincronização por pelo menos cinco minutos.
- Exercitar cenas paradas/animadas, resize, minimizar/restaurar e mudança de monitor/DPI.
- Repetir em FIFO e em equipamento/superfície com MAILBOX, registrando GPU e driver.
- Criar teste offscreen com leitura de pixels para cores, alpha, sprites, fontes e ordem de desenho.
- Implementar retirement de swapchains com fences de apresentação via extensão detectada e habilitada.

**Aceite:** zero erros de uso Vulkan, ausência de flicker reproduzível, evidências visuais e teste de pixels aprovado. Smoke com exit code zero não substitui esse aceite.

### 2. Consolidar física e recursos de mapas/texto

- Definir a política de resposta para múltiplos colliders, velocidades altas e colisões simultâneas.
- Adicionar interpolação visual usando estado anterior/atual; testar equivalência em 30/60/144 Hz no loop completo.
- Migrar os exemplos que ainda têm solver próprio e tempo variável.
- Implementar elipses exatas e transformações de tiles com fixtures específicas.
- Ampliar Unicode, baseline, kerning/shaping e atlas conforme necessidade medida.

**Aceite:** deslocamentos e resultados físicos equivalentes entre taxas, nenhum listener residual após trocas repetidas e geometria/layout comparados com fixtures.

### 3. Escalar e robustecer o renderer

- Separar recursos persistentes daqueles dependentes da swapchain.
- Medir CPU/GPU e memória antes de introduzir múltiplos frames em voo.
- Implementar pools expansíveis de descritores, uploads agrupados e orçamento de memória/VMA se justificado.
- Tratar sistematicamente falhas parciais dos demais owners Vulkan, device lost e falta de memória.
- Estabilizar retenção de fontes/atlases em sessões longas com muitos tamanhos.

**Aceite:** memória estabilizada, resize sem recarregar assets persistentes e métricas que demonstrem o ganho.

### 4. Entrega

- CI em JDK 25 e versões correntes; runner GPU/lavapipe para renderização e driver real para apresentação.
- Perfis explícitos para outros sistemas/arquiteturas, atualmente fora da configuração de nativos.
- Empacotamento de runtime com jpackage e tutorial de minijogo.
- Revisar versões de dependências/plugins em mudanças separadas com testes de regressão.

## Fora desta etapa

Motor 3D completo, ray tracing, renderer alternativo e redesign em ECS não são requisitos para concluir a migração 2D.

## Regra de acompanhamento

Marque um item como concluído apenas quando implementação e evidência do aceite existirem. Registre ambientes indisponíveis como limites, não como testes aprovados. Novas falhas reproduzidas devem entrar no review e receber regressão antes de ampliar o escopo.
