package br.com.engine.scripting.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import br.com.engine.resources.ResourceRef;
import br.com.engine.resources.ResourceResolver;
import br.com.engine.scripting.ScriptApiDefinition;
import br.com.engine.scripting.ScriptApiRegistry;
import br.com.engine.scripting.ScriptContext;
import br.com.engine.scripting.ScriptEvent;
import br.com.engine.scripting.ScriptException;
import br.com.engine.scripting.ScriptInstance;
import br.com.engine.scripting.ScriptLimitException;
import br.com.engine.scripting.ScriptLimits;
import br.com.engine.scripting.ScriptModule;
import br.com.engine.scripting.ScriptRuntime;
import br.com.engine.scripting.ScriptValue;
import party.iroiro.luajava.Lua;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.LuaValue;

/** LuaJava implementation. No LuaJava type appears outside this package. */
public final class LuaScriptRuntime implements ScriptRuntime
{
    private record ModuleKey(String packHash, String path) { }

    private final Thread gameThread = Thread.currentThread();
    private final ResourceResolver resolver;
    private final ScriptLimits limits;
    private final ScriptApiRegistry apis;
    private final Map<ModuleKey, LuaScriptModule> modules = new LinkedHashMap<>();
    private final Set<LuaScriptInstance> instances = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean closed;
    private int activeExecutions;

    public LuaScriptRuntime(ResourceResolver resolver, ScriptApiRegistry gameApis, ScriptLimits limits)
    {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.limits = Objects.requireNonNull(limits, "limits");
        this.apis = engineApis();
        if (gameApis != null)
            for (ScriptApiDefinition definition : gameApis.definitions())
                this.apis.register(definition.namespace(), definition.name(), definition.capability(), definition.implementation());
    }

    @Override public ScriptModule loadModule(ResourceRef reference) { return loadModule(reference, List.of()); }

    @Override public ScriptModule loadModule(ResourceRef reference, List<ResourceRef> dependencies)
    {
        requireGameThread();
        requireOpen();
        resolver.requireOwned(reference);
        if (!reference.path().endsWith(".lua")) throw new IllegalArgumentException("Lua modules must use the .lua extension: " + reference);
        List<ResourceRef> declared = List.copyOf(dependencies == null ? List.of() : dependencies);
        for (ResourceRef dependency : declared)
        {
            resolver.requireOwned(dependency);
            if (dependency.equals(reference)) throw new IllegalArgumentException("Lua module cannot depend on itself: " + reference);
            ModuleKey dependencyKey = new ModuleKey(dependency.packHash(), dependency.path());
            if (!modules.containsKey(dependencyKey)) loadModule(dependency, List.of());
        }
        ModuleKey key = new ModuleKey(reference.packHash(), reference.path());
        LuaScriptModule module = modules.get(key);
        if (module == null)
        {
            module = new LuaScriptModule(reference, readSource(reference), declared);
            modules.put(key, module);
        }
        else if (!module.dependencies().equals(declared))
            throw new IllegalArgumentException("Lua module was already loaded with different dependencies: " + reference);
        return module;
    }

    @Override public ScriptInstance createInstance(ScriptModule module, ScriptContext context)
    {
        requireGameThread();
        requireOpen();
        if (!(module instanceof LuaScriptModule luaModule) || !modules.containsValue(luaModule))
            throw new IllegalArgumentException("Script module was not loaded by this runtime");
        LuaScriptInstance instance = new LuaScriptInstance(luaModule, context == null ? ScriptContext.empty() : context);
        instances.add(instance);
        return instance;
    }

    @Override public boolean isClosed() { requireGameThread(); return closed; }

    @Override public void close()
    {
        requireGameThread();
        if (closed) return;
        if (activeExecutions != 0) throw new IllegalStateException("Cannot close a runtime during a Lua callback");
        closed = true;
        RuntimeException failure = null;
        for (LuaScriptInstance instance : List.copyOf(instances))
            try { instance.dispose(); } catch (RuntimeException exception) { failure = combine(failure, exception); }
        modules.clear();
        if (failure != null) throw failure;
    }

    private String readSource(ResourceRef reference)
    {
        final byte[] bytes;
        try { bytes = resolver.read(reference, limits.maxSourceBytes()); }
        catch (IOException exception) { throw new ScriptException("Cannot read Lua module: " + reference, exception); }
        if (bytes.length == 0) throw new ScriptException("Lua module is empty: " + reference);
        if (bytes[0] == 0x1b) throw new ScriptException("Lua bytecode is not accepted: " + reference);
        try
        {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        }
        catch (CharacterCodingException exception) { throw new ScriptException("Lua source must be UTF-8: " + reference, exception); }
    }

    private static ScriptApiRegistry engineApis()
    {
        ScriptApiRegistry result = new ScriptApiRegistry();
        result.registerEngine("clock.now_millis", "engine.clock", (context, arguments) -> {
            exact(arguments, 0, "engine.clock.now_millis");
            Instant now = context.clock().now();
            return ScriptValue.of(now.toEpochMilli());
        });
        result.registerEngine("state.get", "engine.state", (context, arguments) -> {
            exact(arguments, 1, "engine.state.get");
            return context.state(requireString(arguments.get(0), "engine.state.get"));
        });
        result.registerEngine("state.set", "engine.state", (context, arguments) -> {
            exact(arguments, 2, "engine.state.set");
            context.putState(requireString(arguments.get(0), "engine.state.set"), arguments.get(1));
            return ScriptValue.of(null);
        });
        result.registerEngine("events.emit", "engine.events", (context, arguments) -> {
            exact(arguments, 2, "engine.events.emit");
            context.emitter().emit(new ScriptEvent(requireString(arguments.get(0), "engine.events.emit"), arguments.get(1)));
            return ScriptValue.of(null);
        });
        return result;
    }

    private static void exact(List<ScriptValue> arguments, int expected, String api)
    {
        if (arguments.size() != expected) throw new IllegalArgumentException(api + " expects " + expected + " argument(s)");
    }

    private static String requireString(ScriptValue value, String api)
    {
        try { return value.asString(); }
        catch (IllegalStateException exception) { throw new IllegalArgumentException(api + " requires a string", exception); }
    }

    private void requireGameThread()
    {
        if (Thread.currentThread() != gameThread)
            throw new IllegalStateException("ScriptRuntime is confined to game thread " + gameThread.getName());
    }

    private void requireOpen()
    {
        if (closed) throw new IllegalStateException("ScriptRuntime is closed");
    }

    private static RuntimeException combine(RuntimeException first, RuntimeException next)
    {
        if (first == null) return next;
        first.addSuppressed(next);
        return first;
    }

    private static final class LuaScriptModule implements ScriptModule
    {
        private final ResourceRef reference;
        private final String source;
        private final List<ResourceRef> dependencies;

        private LuaScriptModule(ResourceRef reference, String source, List<ResourceRef> dependencies)
        {
            this.reference = reference;
            this.source = source;
            this.dependencies = List.copyOf(dependencies);
        }

        @Override public ResourceRef reference() { return reference; }
        @Override public List<ResourceRef> dependencies() { return dependencies; }
    }

    private final class LuaScriptInstance implements ScriptInstance
    {
        private final LuaScriptModule script;
        private final ScriptContext context;
        private final Lua lua;
        private final AtomicInteger instructions = new AtomicInteger();
        private LuaValue module;
        private LuaValue callbackLookup;
        private LuaValue nullValue;
        private LuaValue listMarkers;
        private boolean disposed;
        private boolean callbackActive;

        private LuaScriptInstance(LuaScriptModule script, ScriptContext context)
        {
            this.script = script;
            this.context = context;
            Lua created = null;
            try
            {
                created = new Lua54();
                this.lua = created;
                configureSandbox();
                LuaValue[] shapes = lua.eval("local null={}; local lists=setmetatable({}, {__mode='k'}); "
                    + "local kind=type; local fail=error; "
                    + "engine.value={null=null, list=function(value) "
                    + "if kind(value)~='table' or value==null then fail('Expected a list table') end; "
                    + "lists[value]=true; return value end}; return null,lists");
                nullValue = shapes[0];
                listMarkers = shapes[1];
                callbackLookup = lua.eval("return function(module, name) return module[name] end")[0];
                LuaValue[] values = budgeted(() -> lua.eval(script.source));
                if (values == null || values.length != 1 || values[0].type() != Lua.LuaType.TABLE)
                    throw new ScriptException("Lua module must return exactly one table: " + script.reference());
                module = values[0];
            }
            catch (RuntimeException exception)
            {
                if (created != null) try { created.close(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
                throw wrap("Cannot load Lua module " + script.reference(), exception);
            }
        }

        private void configureSandbox()
        {
            lua.openLibrary("base");
            lua.openLibrary("table");
            lua.openLibrary("string");
            lua.openLibrary("math");
            lua.openLibrary("utf8");
            lua.openLibrary("debug");
            lua.register("__enginefx_invoke", this::invokeApi);
            lua.register("__enginefx_check_budget", (state, arguments) -> {
                checkBudget();
                return null;
            });
            lua.register("__enginefx_instruction_hook", (state, arguments) -> {
                if (instructions.addAndGet(limits.hookGranularity()) >= limits.maxInstructionsPerCallback())
                    throw new InstructionLimitExceededException(limits.maxInstructionsPerCallback());
                return null;
            });
            lua.run(sandboxSource());
        }

        private String sandboxSource()
        {
            StringBuilder source = new StringBuilder();
            source.append("local check=__enginefx_check_budget; local protected=pcall; local extended=xpcall; ")
                .append("local pack,unpack=table.pack,table.unpack; local invoke=__enginefx_invoke; ")
                .append("pcall=function(...) check(); local values=pack(protected(...)); check(); return unpack(values,1,values.n) end; ")
                .append("xpcall=function(...) check(); local values=pack(extended(...)); check(); return unpack(values,1,values.n) end; ")
                .append("debug.sethook(__enginefx_instruction_hook,'',").append(limits.hookGranularity()).append("); ")
                .append("__enginefx_invoke=nil; __enginefx_check_budget=nil; __enginefx_instruction_hook=nil; ")
                .append("java=nil; io=nil; os=nil; debug=nil; package=nil; require=nil; dofile=nil; loadfile=nil; load=nil; collectgarbage=nil; ");
            for (ScriptApiDefinition definition : apis.definitions()) appendApi(source, definition);
            return source.toString();
        }

        private static void appendApi(StringBuilder source, ScriptApiDefinition definition)
        {
            String[] namespace = definition.namespace().split("\\.");
            source.append(namespace[0]).append('=').append(namespace[0]).append(" or {}; ");
            String table = namespace[0];
            for (int index = 1; index < namespace.length; index++)
            {
                table += "." + namespace[index];
                source.append(table).append('=').append(table).append(" or {}; ");
            }
            String[] function = definition.name().split("\\.");
            for (int index = 0; index < function.length - 1; index++)
            {
                table += "." + function[index];
                source.append(table).append('=').append(table).append(" or {}; ");
            }
            source.append(table).append('.').append(function[function.length - 1]).append("=function(...) return invoke('")
                .append(definition.namespace()).append("','").append(definition.name()).append("',...) end; ");
        }

        private LuaValue[] invokeApi(Lua state, LuaValue[] arguments)
        {
            if (!callbackActive) throw new SecurityException("Script APIs are available only while a callback is running");
            checkBudget();
            if (arguments.length < 2 || arguments[0].type() != Lua.LuaType.STRING || arguments[1].type() != Lua.LuaType.STRING)
                throw new IllegalArgumentException("Invalid script API call");
            List<ScriptValue> values = new ArrayList<>();
            for (int index = 2; index < arguments.length; index++) values.add(fromLua(arguments[index]));
            ScriptValue result = apis.invoke(arguments[0].toString(), arguments[1].toString(), context, values);
            return new LuaValue[] { toLua(state, result) };
        }

        @Override public void setup() { optional("setup"); }
        @Override public void update(double deltaSeconds) { delta("update", deltaSeconds); }
        @Override public void fixedUpdate(double deltaSeconds) { delta("fixed_update", deltaSeconds); }
        @Override public void onEvent(ScriptEvent event)
        {
            if (event == null) throw new IllegalArgumentException("Script event is required");
            optional("on_event", ScriptValue.of(event.type()), event.payload());
        }

        private void delta(String callback, double deltaSeconds)
        {
            if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) throw new IllegalArgumentException("Lua delta must be finite and non-negative");
            optional(callback, ScriptValue.of(deltaSeconds));
        }

        private void optional(String callback, ScriptValue... arguments)
        {
            invokeCallback(callback, true, arguments);
        }

        @Override public ScriptValue call(String callback, ScriptValue... arguments)
        {
            return invokeCallback(callback, false, arguments);
        }

        private ScriptValue invokeCallback(String callback, boolean optional, ScriptValue... arguments)
        {
            requireLive();
            if (callback == null || !callback.matches("[a-z][a-z0-9_]{0,127}"))
                throw new IllegalArgumentException("Invalid Lua callback: " + callback);
            if (callbackActive) throw new IllegalStateException("Cannot reenter an active Lua instance");
            try
            {
                return budgeted(() -> {
                    LuaValue function = lookup(callback);
                    if (optional && function.type() == Lua.LuaType.NIL) return ScriptValue.of(null);
                    if (function.type() != Lua.LuaType.FUNCTION)
                        throw new ScriptException("Lua callback is missing: " + callback + " in " + script.reference());
                    Object[] converted = new Object[arguments == null ? 0 : arguments.length];
                    for (int index = 0; index < converted.length; index++)
                        converted[index] = toLua(lua, arguments[index] == null ? ScriptValue.of(null) : arguments[index]);
                    LuaValue[] results = function.call(converted);
                    checkBudget();
                    if (results == null) throw new ScriptException("Lua callback failed: " + callback);
                    if (results.length > 1) throw new ScriptException("Lua callback returned more than one value: " + callback);
                    return results.length == 0 ? ScriptValue.of(null) : fromLua(results[0]);
                });
            }
            catch (RuntimeException exception) { throw wrap("Lua callback failed: " + callback + " in " + script.reference(), exception); }
        }

        private LuaValue lookup(String callback)
        {
            // A protected Lua call keeps __index execution inside the callback budget.
            LuaValue[] found = callbackLookup.call(module, callback);
            checkBudget();
            if (found == null || found.length != 1) throw new ScriptException("Cannot resolve Lua callback: " + callback);
            return found[0];
        }

        private <T> T budgeted(java.util.function.Supplier<T> action)
        {
            if (callbackActive) throw new IllegalStateException("Cannot reenter an active Lua instance");
            instructions.set(0);
            callbackActive = true;
            activeExecutions++;
            try
            {
                T result = action.get();
                checkBudget();
                return result;
            }
            finally { activeExecutions--; callbackActive = false; }
        }

        private void checkBudget()
        {
            if (instructions.get() >= limits.maxInstructionsPerCallback())
                throw new InstructionLimitExceededException(limits.maxInstructionsPerCallback());
        }

        private LuaValue toLua(Lua state, ScriptValue value)
        {
            ScriptValue checked = ScriptValue.of(value.toJavaObject(), limits);
            int top = state.getTop();
            try
            {
                pushPlain(state, checked.toJavaObject(), false);
                return state.get();
            }
            finally { state.setTop(top); }
        }

        private void pushPlain(Lua state, Object value, boolean nested)
        {
            state.checkStack(4);
            if (value == null)
            {
                if (nested) state.push(nullValue); else state.pushNil();
            }
            else if (value instanceof List<?> list)
            {
                state.createTable(list.size(), 0);
                int table = state.getTop();
                for (int index = 0; index < list.size(); index++)
                {
                    pushPlain(state, list.get(index), true);
                    state.rawSetI(table, index + 1);
                }
                state.push(listMarkers);
                state.pushValue(table);
                state.push(true);
                state.rawSet(-3);
                state.pop(1);
            }
            else if (value instanceof Map<?, ?> map)
            {
                state.createTable(0, map.size());
                int table = state.getTop();
                for (Map.Entry<?, ?> entry : map.entrySet())
                {
                    state.push((String)entry.getKey());
                    pushPlain(state, entry.getValue(), true);
                    state.rawSet(table);
                }
            }
            else state.push(value, Lua.Conversion.FULL);
        }

        private ScriptValue fromLua(LuaValue value)
        {
            int top = lua.getTop();
            try
            {
                lua.push(value);
                return ScriptValue.of(toPlain(lua.getTop(), 0, new ArrayList<>(), new int[1]), limits);
            }
            finally { lua.setTop(top); }
        }

        private Object toPlain(int index, int depth, List<Integer> ancestors, int[] nodes)
        {
            if (depth > limits.maxValueDepth()) throw new ScriptException("Lua value exceeds maximum depth");
            if (++nodes[0] > limits.maxValueNodes()) throw new ScriptException("Lua value exceeds maximum node count");
            return switch (lua.type(index))
            {
                case NIL -> null;
                case BOOLEAN -> lua.toBoolean(index);
                case STRING -> string(index);
                case NUMBER -> number(index);
                case TABLE -> table(index, depth, ancestors, nodes);
                default -> throw new ScriptException("Lua value type cannot cross the script boundary: " + lua.type(index));
            };
        }

        private String string(int index)
        {
            if (lua.rawLength(index) > limits.maxStringBytes()) throw new ScriptException("Lua string exceeds maximum size");
            try
            {
                return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(lua.toBuffer(index)).toString();
            }
            catch (CharacterCodingException exception) { throw new ScriptException("Lua value must contain UTF-8 text", exception); }
        }

        private Object number(int index)
        {
            if (lua.isInteger(index)) return lua.toInteger(index);
            double value = lua.toNumber(index);
            if (!Double.isFinite(value)) throw new ScriptException("Lua numbers must be finite");
            return value;
        }

        private Object table(int index, int depth, List<Integer> ancestors, int[] nodes)
        {
            lua.checkStack(4);
            lua.push(nullValue);
            boolean isNull = lua.rawEqual(index, -1);
            lua.pop(1);
            if (isNull) return null;
            lua.push(listMarkers);
            lua.pushValue(index);
            lua.rawGet(-2);
            boolean declaredList = lua.toBoolean(-1);
            lua.pop(2);
            for (int ancestor : ancestors)
                if (lua.rawEqual(index, ancestor)) throw new ScriptException("Lua tables cannot contain cycles");
            ancestors.add(index);
            int top = lua.getTop();
            try
            {
                Map<String, Object> map = new LinkedHashMap<>();
                Map<Long, Object> list = new TreeMap<>();
                boolean stringKeys = false, numericKeys = false;
                lua.pushNil();
                while (lua.next(index) != 0)
                {
                    int key = lua.getTop() - 1;
                    Object converted = toPlain(lua.getTop(), depth + 1, ancestors, nodes);
                    if (lua.type(key) == Lua.LuaType.STRING)
                    {
                        stringKeys = true;
                        map.put(string(key), converted);
                    }
                    else if (lua.isInteger(key) && lua.toInteger(key) >= 1)
                    {
                        numericKeys = true;
                        list.put(lua.toInteger(key), converted);
                    }
                    else throw new ScriptException("Lua tables require string map keys or positive integer list keys");
                    lua.pop(1);
                    if (stringKeys && numericKeys) throw new ScriptException("Lua tables cannot mix map and list keys");
                }
                if (declaredList && stringKeys) throw new ScriptException("Lua lists cannot contain map keys");
                if (numericKeys || declaredList)
                {
                    List<Object> result = new ArrayList<>();
                    long expected = 1;
                    for (Map.Entry<Long, Object> entry : list.entrySet())
                    {
                        if (entry.getKey() != expected++) throw new ScriptException("Lua lists must have contiguous keys starting at one");
                        result.add(entry.getValue());
                    }
                    return result;
                }
                return map;
            }
            finally { lua.setTop(top); ancestors.remove(ancestors.size() - 1); }
        }

        @Override public boolean isDisposed() { requireGameThread(); return disposed; }

        @Override public void dispose()
        {
            requireGameThread();
            if (disposed) return;
            if (callbackActive) throw new IllegalStateException("Cannot dispose an active Lua instance");
            disposed = true;
            RuntimeException failure = null;
            try { optionalDispose(); }
            catch (RuntimeException exception) { failure = wrap("Lua dispose failed in " + script.reference(), exception); }
            finally
            {
                instances.remove(this);
                try { lua.close(); }
                catch (RuntimeException cleanup) { failure = combine(failure, cleanup); }
                finally { module = null; callbackLookup = null; nullValue = null; listMarkers = null; }
            }
            if (failure != null) throw failure;
        }

        private void optionalDispose()
        {
            if (module == null) return;
            budgeted(() -> {
                LuaValue function = lookup("dispose");
                if (function.type() == Lua.LuaType.NIL) return null;
                if (function.type() != Lua.LuaType.FUNCTION) throw new ScriptException("Lua dispose callback is not a function");
                LuaValue[] results = function.call();
                checkBudget();
                if (results == null || results.length > 0) throw new ScriptException("Lua dispose callback must not return a value");
                return null;
            });
        }

        private void requireLive()
        {
            requireGameThread();
            requireOpen();
            if (disposed) throw new IllegalStateException("Script instance is disposed");
        }
    }

    private static ScriptException wrap(String message, RuntimeException exception)
    {
        if (exception instanceof ScriptException script) return script;
        String detail = String.valueOf(exception.getMessage());
        if (detail.contains("Lua instruction limit exceeded"))
            return new ScriptLimitException("Lua instruction limit exceeded", exception);
        return new ScriptException(message + ": " + detail, exception);
    }

    private static final class InstructionLimitExceededException extends RuntimeException
    {
        private InstructionLimitExceededException(int limit) { super("Lua instruction limit exceeded: " + limit); }
    }
}
