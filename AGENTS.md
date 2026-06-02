# AGENTS.md

Guidance for AI coding agents working in this repository.

## Scope

- This is a Maven-based JavaFX game engine under `src/br/com/engine`.
- The main entry point is [src/br/com/engine/main/Executor.java](src/br/com/engine/main/Executor.java).
- There is no existing workspace documentation set; use the source as the primary reference.
- For a detailed architecture reference, see [Project_Architecture_Blueprint.md](Project_Architecture_Blueprint.md).

## Build And Run

- Preferred build command: `mvn clean compile`
- Package command: `mvn package`
- Tests: no test sources or test framework are configured in `pom.xml`
- Environment note: this workspace does not include `mvnw`, and in the current environment `mvn` is not on `PATH`; verify Maven is installed before relying on Maven commands.
- Runtime note: the engine expects asset files under `./res/...` at runtime.

## Architecture Anchors

- [src/br/com/engine/core/ControleBase.java](src/br/com/engine/core/ControleBase.java): singleton runtime controller, screen setup, scene switching, and main loop startup.
- [src/br/com/engine/core/MainLoopFx.java](src/br/com/engine/core/MainLoopFx.java): JavaFX `Timeline` loop calling `setup`, `processLogics`, `renderGraphics`, and `paintScreen`.
- [src/br/com/engine/core/Scene.java](src/br/com/engine/core/Scene.java): scene lifecycle, object collection management, deferred add/remove, collision pass.
- [src/br/com/engine/core/GameObject.java](src/br/com/engine/core/GameObject.java): entity container for components and parent/child propagation.
- [src/br/com/engine/resources/ResourceManager.java](src/br/com/engine/resources/ResourceManager.java): resource type constants and loading conventions for images, audio, scripts, fonts, maps, and config.

## Package Map

- `core`: engine control flow, scenes, game objects, vectors, loop.
- `componentes`: components attached to `GameObject`; includes drawable, physics, scripts, audio, debug, and builders.
- `resources`: config loading, scene definitions, content/resource lookup.
- `input`: keyboard and mouse handlers.
- `fisica`: collision detection and resolution.
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
- Scene switches are deferred through `ControleBase.nextScene(...)`; do not assume immediate scene replacement.
- The project targets Java 8 source/target in Maven, but depends on JavaFX 15 artifacts. Be cautious when changing build settings.
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