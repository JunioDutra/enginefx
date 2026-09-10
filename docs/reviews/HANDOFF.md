# Review handoff — enginefx 2.0.0

## Escopo alterado

- Migração das APIs gráficas públicas para tipos próprios (`Color`, `Paint`, `Image`, `Font` e `IntPoint`), sem `java.awt`.
- Carregamento de recursos por API tipada e suporte a recursos no classpath do JAR.
- Parser TMX StAX para o subconjunto usado pelas cenas.
- Lifecycle com descarte idempotente, mutações diferidas e atualização física fixa.
- Seleção configurável de present mode Vulkan e melhorias de diagnóstico.
- Buffer de vértices com crescimento por duplicação.

## Validação executada

```powershell
$env:JAVA_HOME='C:/Users/dev/AppData/Local/mise/installs/java/27.0.0'
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
.\mvnw.cmd test install
```

Resultado: 6 testes aprovados em JDK 27.

## Pontos para revisão cuidadosa

- `ContentLoader` mantém buffers STB nativos até o encerramento controlado. A liberação imediata derrubava o processo nesta combinação de LWJGL/JDK; `releaseNativeImageBuffers()` existe para o descarte após o renderer encerrar.
- A sincronização avançada de apresentação (`VK_KHR_swapchain_maintenance1`) e os goldens offscreen ainda merecem revisão/implementação específica antes do aceite final de driver.
- O renderer ainda possui oportunidades previstas no roadmap: múltiplos frames em voo, VMA, uploads agrupados e descriptor pools expansíveis.
