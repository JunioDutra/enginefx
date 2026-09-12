package br.com.engine.luaharness;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import party.iroiro.luajava.Lua;
import party.iroiro.luajava.LuaException;
import party.iroiro.luajava.lua54.Lua54;
import party.iroiro.luajava.value.LuaValue;

/**
 * Technical gate for the planned Lua runtime. This harness deliberately keeps
 * LuaJava types out of EngineFX's public API; the 2.2 runtime is a later step.
 */
public final class LuaNativeHarness
{
    public static final int DEFAULT_INSTRUCTION_LIMIT = 100_000;
    private static final int HOOK_GRANULARITY = 1_000;
    private static final int MAX_SOURCE_BYTES = 1_048_576;

    private LuaNativeHarness() { }

    public record Result(int callbackValue, int javaToLuaValue, int hookCalls, boolean externalModule) { }

    public static void main(String[] args) throws Exception
    {
        Path external = args.length == 0 ? null : Path.of(args[0]);
        Result result = verify(external);
        verifyRepeatedLifecycle(200);
        System.out.printf("PASS Lua 5.4 harness: callback=%d javaToLua=%d hooks=%d external=%s lifecycle=200%n",
            result.callbackValue(), result.javaToLuaValue(), result.hookCalls(), result.externalModule());
    }

    /** Runs the JVM/native proof once. The optional file must be plain UTF-8 Lua source. */
    public static Result verify(Path externalSource) throws IOException
    {
        String source = externalSource == null ? null : readLuaSource(externalSource);
        try (Lua lua = new Lua54())
        {
            AtomicInteger callbackValue = new AtomicInteger();
            AtomicInteger hookCalls = new AtomicInteger();
            AtomicInteger instructions = new AtomicInteger();
            configureSandbox(lua, callbackValue, hookCalls, instructions);

            LuaValue[] version = lua.eval("return _VERSION");
            require("Lua 5.4".equals(version[0].toString()), "Lua 5.4 was not loaded");
            require(isTrue(lua.eval("return java == nil and io == nil and os == nil and debug == nil and "
                + "package == nil and require == nil and dofile == nil and loadfile == nil and load == nil "
                + "and collectgarbage == nil and enginefx_instruction_hook == nil and enginefx_check_budget == nil")[0]),
                "Unsafe Lua libraries are still visible");
            require("a\u00e7\u00e3o".equals(lua.eval("return 'a\u00e7\u00e3o'")[0].toString()), "Lua UTF-8 round trip failed");
            require(isTrue(lua.eval("local ok = pcall(function() return engine_record:getClass() end); return not ok")[0]),
                "The callback exposes Java object capabilities");

            lua.run("function enginefx_double(value) engine_record(value); return value * 2 end");
            LuaValue[] doubled = lua.get("enginefx_double").call(21);
            require(doubled.length == 1 && isNumber(doubled[0], 42),
                "Java could not call the Lua function");
            require(callbackValue.get() == 21, "Lua could not invoke the registered Java callback");

            boolean external = source != null;
            if (external)
            {
                lua.run(source);
                checkBudget(instructions);
                LuaValue[] externalValue = lua.get("enginefx_external_value").call();
                checkBudget(instructions);
                require(externalValue.length == 1 && isNumber(externalValue[0], 84),
                    "External Lua module did not execute after the binary was built");
                require(callbackValue.get() == 84, "External Lua module did not invoke engine_record(84)");
            }

            LuaException limitFailure = captureFailure(() -> lua.run("while true do end"));
            require(limitFailure != null && limitFailure.getMessage().contains("Lua instruction limit exceeded"),
                "The instruction hook did not report the expected limit failure");
            require(instructions.get() >= DEFAULT_INSTRUCTION_LIMIT,
                "The instruction hook did not observe the configured limit");
            return new Result(callbackValue.get(), Math.toIntExact(doubled[0].toInteger()), hookCalls.get(), external);
        }
    }

    /** Creates and closes fresh Lua states, making native resource ownership observable in tests. */
    public static void verifyRepeatedLifecycle(int times) throws IOException
    {
        if (times <= 0) throw new IllegalArgumentException("times must be positive");
        for (int index = 0; index < times; index++)
        {
            Result result = verify(null);
            require(result.callbackValue() == 21 && result.javaToLuaValue() == 42,
                "Lua state " + index + " produced a different result");
        }
    }

    private static void configureSandbox(Lua lua, AtomicInteger callbackValue, AtomicInteger hookCalls, AtomicInteger instructions)
    {
        lua.openLibrary("base");
        lua.openLibrary("table");
        lua.openLibrary("string");
        lua.openLibrary("math");
        lua.openLibrary("utf8");
        lua.openLibrary("debug");
        lua.register("engine_record", (context, arguments) -> {
            checkBudget(instructions);
            if (arguments.length != 1 || arguments[0].type() != Lua.LuaType.NUMBER)
                throw new IllegalArgumentException("engine_record expects one integer");
            double value = arguments[0].toNumber();
            if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
                throw new IllegalArgumentException("engine_record expects one integer");
            callbackValue.set((int) value);
            return null;
        });
        lua.register("enginefx_check_budget", (context, arguments) -> {
            checkBudget(instructions);
            return null;
        });
        lua.register("enginefx_instruction_hook", (context, arguments) -> {
            hookCalls.incrementAndGet();
            if (instructions.addAndGet(HOOK_GRANULARITY) >= DEFAULT_INSTRUCTION_LIMIT)
                throw new InstructionLimitExceededException(DEFAULT_INSTRUCTION_LIMIT);
            return null;
        });
        // Keep protected calls useful, but never let them turn an exhausted budget into success.
        // All captured functions are private upvalues once debug and the host helper are hidden.
        lua.run("local check = enginefx_check_budget; local protected = pcall; local extended = xpcall; "
            + "local pack, unpack = table.pack, table.unpack; "
            + "pcall = function(...) check(); local values = pack(protected(...)); check(); return unpack(values, 1, values.n) end; "
            + "xpcall = function(...) check(); local values = pack(extended(...)); check(); return unpack(values, 1, values.n) end; "
            + "debug.sethook(enginefx_instruction_hook, '', " + HOOK_GRANULARITY + "); "
            + "debug = nil; enginefx_instruction_hook = nil; java = nil; io = nil; os = nil; "
            + "package = nil; require = nil; dofile = nil; loadfile = nil; load = nil; collectgarbage = nil; enginefx_check_budget = nil");
    }

    private static void checkBudget(AtomicInteger instructions)
    {
        if (instructions.get() >= DEFAULT_INSTRUCTION_LIMIT)
            throw new InstructionLimitExceededException(DEFAULT_INSTRUCTION_LIMIT);
    }

    private static String readLuaSource(Path path) throws IOException
    {
        Objects.requireNonNull(path, "path");
        byte[] bytes;
        try (var input = Files.newInputStream(path)) { bytes = input.readNBytes(MAX_SOURCE_BYTES + 1); }
        if (bytes.length == 0 || bytes.length > MAX_SOURCE_BYTES)
            throw new IllegalArgumentException("Lua source must contain 1 to " + MAX_SOURCE_BYTES + " bytes: " + path);
        if (bytes[0] == 0x1b)
            throw new IllegalArgumentException("Lua bytecode is not accepted: " + path);
        try
        {
            CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
            return decoded.toString();
        }
        catch (CharacterCodingException exception)
        {
            throw new IllegalArgumentException("Lua source must be UTF-8: " + path, exception);
        }
    }

    private static boolean isTrue(LuaValue value) { return value.type() == Lua.LuaType.BOOLEAN && value.toBoolean(); }

    private static boolean isNumber(LuaValue value, int expected)
    {
        return value.type() == Lua.LuaType.NUMBER && value.toNumber() == expected;
    }

    private static LuaException captureFailure(Runnable action)
    {
        try
        {
            action.run();
            return null;
        }
        catch (LuaException failure) { return failure; }
    }

    private static void require(boolean condition, String message)
    {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class InstructionLimitExceededException extends RuntimeException
    {
        private InstructionLimitExceededException(int limit) { super("Lua instruction limit exceeded: " + limit); }
    }
}
