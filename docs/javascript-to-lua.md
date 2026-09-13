# Migração de JavaScript para Lua — EngineFX 3.0

EngineFX 3.0 removeu Nashorn e todas as APIs JavaScript. `SceneJs`, `ScriptJsComponent`, `ScriptBuilder.createJs`, `ResourceManager.script` e o carregamento de recursos `.js` não existem mais. Uma definição com `"type": "js"` falha com o código `SCRIPT_TYPE_REMOVED` e esta orientação de migração.

## Substituição direta

O JavaScript legado recebia objetos Java diretamente:

```javascript
function update(time) { gameObject.getPosition().plus(2, 0); }
```

Em Lua, o módulo retorna uma tabela e só chama uma API do jogo que tenha sido registrada e autorizada:

```lua
return {
  update = function(delta)
    game.object.move(delta)
  end,
  dispose = function()
    game.object.disposed()
  end
}
```

`delta` está em segundos. O jogo registra `game.object.move` em `ScriptApiRegistry`, atribui a capability `game.object` ao `ScriptContext` e mantém a referência ao `GameObject` exclusivamente no código Java. Não transfira `Scene`, `Screen`, singletons, input ou objetos Java para Lua.

## Carregamento no JAR

Use um `ResourceResolver` e referências qualificadas pelo pacote:

```java
// Campos e callbacks da cena Java; gameApis e context são definidos pelo jogo.
private ScriptRuntime runtime;

@Override public void setup() {
    super.setup();
    if (runtime != null) return;
    var resources = new ClasspathResourceResolver("my-game", "1.0.0", "res");
    runtime = ScriptRuntime.lua(resources, gameApis);
    var module = runtime.loadModule(resources.ref("scripts/player.lua"));
    var object = new GameObject("player");
    object.addComponente(new LuaComponent(runtime, module, context));
    add(object);
}

@Override public void dispose() {
    try { super.dispose(); } // Descarta componentes enquanto Lua ainda está aberta.
    finally {
        var closing = runtime;
        runtime = null;
        if (closing != null) closing.close();
    }
}
```

Mantenha o runtime aberto durante toda a vida da cena. Fechá-lo logo após adicionar o componente impede os callbacks seguintes. O controlador descarta a cena também quando o setup falha.

Use `DirectoryResourceResolver` para um pacote instalado. Lua deve ser UTF-8, terminar em `.lua` e retornar uma única tabela. Os callbacks aceitos são `setup`, `update`, `fixed_update`, `on_event` e `dispose`.

Para lógica de cena, registre uma factory Java no `SceneRegistry` e componha um `LuaScene` ou `LuaComponent` com comandos e APIs autorizados. A configuração `application.json` aponta para o id estável da factory, por exemplo `"scene": "game:menu"`; ela não contém nome de classe. `"type": "java"` pode permanecer temporariamente em arquivos antigos, mas não muda a criação explícita.


## Valores e limites

Na fronteira, inteiros continuam inteiros, textos devem conter Unicode válido e strings Lua devem ser UTF-8. Os limites do runtime valem para argumentos, retornos, chaves e valores aninhados, mesmo quando o host já criou um `ScriptValue`.

Lua usa `nil` para remover entradas de tabelas. Para representar nulos dentro de listas/mapas, use `engine.value.null`. Para uma lista vazia criada por Lua, use `engine.value.list({})`; uma tabela vazia comum representa um mapa. Listas e nulos recebidos do Java já usam essa representação e podem ser devolvidos sem perda de dados. Uma lista preenchida comum deve ter índices consecutivos a partir de 1. Não acrescente campos ao marcador de nulo.

```lua
return {
  data = function()
    return { items = engine.value.list({}), selected = engine.value.null }
  end
}
```

A busca de callbacks, inclusive `__index` e `dispose`, compartilha o orçamento da execução. O host não pode reentrar na mesma instância nem fechar instância/runtime enquanto um callback está ativo. Dependências de módulos são referências declaradas, carregadas e validadas; não há importação automática nem `require` disponível.
