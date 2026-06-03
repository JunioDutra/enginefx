# AGENTS.md

Guidance for AI coding agents working in this repository.

## Scope

- This is a Maven-based desktop game engine under `src/br/com/engine`.
- The main entry point is [src/br/com/engine/main/Executor.java](src/br/com/engine/main/Executor.java).
- There is no existing workspace documentation set; use the source as the primary reference.
- For a detailed architecture reference, see [Project_Architecture_Blueprint.md](Project_Architecture_Blueprint.md).

## Build And Run

- Preferred build command: `mvn clean compile`
- Package command: `mvn package`
- Tests: no test sources or test framework are configured in `pom.xml`
- Environment note: this workspace does not include `mvnw`, and in the current environment `mvn` is not on `PATH`; verify Maven is installed before relying on Maven commands.
- Runtime note: the engine expects asset files under `./res/...` at runtime.
- Runtime smoke check: `java -cp target/classes br.com.engine.main.EngineSmokeApp`

## Architecture Anchors

- [src/br/com/engine/main/Executor.java](src/br/com/engine/main/Executor.java): entry point; selects the `vulkan` backend and starts `LwjglVulkanExecutor`.
- [src/br/com/engine/main/LwjglVulkanExecutor.java](src/br/com/engine/main/LwjglVulkanExecutor.java): the ACTIVE main loop (GLFW + LWJGL Vulkan). Initializes window/instance/device/swapchain/renderer and drives `processLogics` / `renderGraphics` / `drawFrame` each frame. The loop is uncapped (no `Thread.sleep`); the swapchain present mode paces it.
- [src/br/com/engine/core/ControleBase.java](src/br/com/engine/core/ControleBase.java): singleton runtime controller, screen setup, scene switching, frame timing. `changeScene()` rebuilds a fresh scene instance from `ScenesDefinition` and calls `resetTransform()` so a previous scene's camera offset does not leak.
- [src/br/com/engine/core/Time.java](src/br/com/engine/core/Time.java): Unity-style delta-time service. `Time.getDeltaTime()` (seconds, clamped to 0.25) and `Time.getDeltaMillis()`; fed by `ControleBase.processLogics()` from `System.nanoTime()` each frame before scenes update. Scale movement by delta to stay frame-rate independent.
- [src/br/com/engine/core/Scene.java](src/br/com/engine/core/Scene.java): scene lifecycle, object collection management, deferred add/remove, collision pass.
- [src/br/com/engine/core/GameObject.java](src/br/com/engine/core/GameObject.java): entity container for components and parent/child propagation.
- [src/br/com/engine/graphics/EngineGraphicsContext.java](src/br/com/engine/graphics/EngineGraphicsContext.java) and [src/br/com/engine/platform/lwjgl/VulkanGraphicsContext.java](src/br/com/engine/platform/lwjgl/VulkanGraphicsContext.java): backend-neutral drawing contract and its only active (Vulkan) implementation; includes `resetTransform()`.
- [src/br/com/engine/resources/ResourceManager.java](src/br/com/engine/resources/ResourceManager.java): resource type constants and loading conventions for images, audio, scripts, fonts, maps, and config.

## Package Map

- `core`: engine control flow, scenes, game objects, vectors, loop contract, and the `Time` service.
- `main`: backend selection (`Executor`) and the active Vulkan loop (`LwjglVulkanExecutor`), plus smoke apps.
- `graphics`: backend-neutral drawing abstraction (`EngineGraphicsContext`, `Color`, `Font`, `Image`, transforms).
- `platform.lwjgl`: concrete LWJGL Vulkan + GLFW implementation (window, instance, device, swapchain, renderer, quad pipeline, vertex buffer, caches, `VulkanGraphicsContext`).
- `componentes`: components attached to `GameObject`; includes drawable, physics, scripts, audio, debug, and builders.
- `resources`: config loading, scene definitions, content/resource lookup.
- `input`: keyboard and mouse handlers sourced from GLFW.
- `fisica`: collision detection and resolution.
- `geometry`: lightweight shapes such as `Rectangle`.
- `interfaces`: engine extension points such as `IComponent`, loop hooks, collision interfaces, and input callbacks.

## Working Conventions

- Follow the existing mixed Portuguese/English naming used by the codebase; do not rename symbols for style consistency unless the task explicitly requires it.
- Preserve the component lifecycle pattern: `setup()`, `update(long time)`, `draw()`.
- New components should align with [src/br/com/engine/componentes/SimpleComponent.java](src/br/com/engine/componentes/SimpleComponent.java) when they need parent `GameObject` access.
- Scene mutation is intentionally deferred while `Scene.update(...)` is iterating. Use `Scene.add(...)` and `Scene.remove(...)` instead of mutating the object list directly.
- Changes to [src/br/com/engine/core/ControleBase.java](src/br/com/engine/core/ControleBase.java) affect global singleton state; keep them small and verify knock-on effects.
- Resource loading is path-convention based. Keep new assets consistent with the prefixes and suffixes defined in [src/br/com/engine/resources/ResourceManager.java](src/br/com/engine/resources/ResourceManager.java).

## Common Pitfalls

- `Scene.setup()` always creates a default camera object.
- `GameObject.setup()` adds a `VectorMonitor`, so movement side effects can propagate to child objects.
- Debug mode injects extra debug components when objects are added to a scene.
- Scene switches are deferred through `ControleBase.nextScene(...)`; do not assume immediate scene replacement. On switch, a fresh scene instance is built and the graphics transform is reset, so scene fields do not persist across visits.
- The active desktop backend is LWJGL Vulkan + GLFW (`Executor` sets `enginefx.backend=vulkan`). A legacy Swing/Java2D path and `MainLoopFx` still exist in the tree but are not the active runtime.
- The frame loop is uncapped; movement must be scaled by `Time.getDeltaTime()` (pixels per second) rather than fixed pixels per frame, or it will run faster at higher frame rates.
- Script loading uses Nashorn via `ScriptEngineManager`; avoid introducing assumptions that require a different JS engine unless the task includes runtime/build updates.

## First Files To Read

When a task is not already anchored to a file, start with the nearest owner of the behavior:

1. [src/br/com/engine/core/ControleBase.java](src/br/com/engine/core/ControleBase.java)
2. [src/br/com/engine/core/Scene.java](src/br/com/engine/core/Scene.java)
3. [src/br/com/engine/core/GameObject.java](src/br/com/engine/core/GameObject.java)
4. [src/br/com/engine/componentes/SimpleComponent.java](src/br/com/engine/componentes/SimpleComponent.java)
5. [src/br/com/engine/resources/ResourceManager.java](src/br/com/engine/resources/ResourceManager.java)

## Change Strategy

- Prefer small, local edits in the owning package instead of broad refactors.
- Validate with the narrowest available check. For this repo that usually means a Maven compile when Maven is available.
- Do not create new customization files unless the repo grows enough to justify package-specific instructions.