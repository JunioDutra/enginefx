package br.com.engine.luaharness;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LuaNativeHarnessTest
{
    @TempDir Path temporaryDirectory;

    @Test void runsLuaCallbacksInBothDirectionsAndLoadsFreshExternalSource() throws Exception
    {
        Path external = temporaryDirectory.resolve("created-after-build.lua");
        Files.writeString(external, "engine_record(84)\nfunction enginefx_external_value() return 84 end\n");
        LuaNativeHarness.Result result = LuaNativeHarness.verify(external);
        assertEquals(84, result.callbackValue());
        assertEquals(42, result.javaToLuaValue());
        assertTrue(result.hookCalls() >= 100);
        assertTrue(result.externalModule());
    }

    @Test void closesRepeatedLuaStatesWithoutChangingTheContract() throws Exception
    {
        LuaNativeHarness.verifyRepeatedLifecycle(200);
    }

    @Test void rejectsLuaBytecodeAndMalformedUtf8BeforeEvaluation() throws Exception
    {
        Path bytecode = temporaryDirectory.resolve("module.lua");
        Files.write(bytecode, new byte[] { 0x1b, 'L', 'u', 'a' });
        assertThrows(IllegalArgumentException.class, () -> LuaNativeHarness.verify(bytecode));
        Path malformed = temporaryDirectory.resolve("malformed.lua");
        Files.write(malformed, new byte[] { (byte) 0xc3, (byte) 0x28 });
        assertThrows(IllegalArgumentException.class, () -> LuaNativeHarness.verify(malformed));
    }

    @Test void rejectsAnExternalModuleThatDoesNotCallTheHost() throws Exception
    {
        Path external = temporaryDirectory.resolve("no-callback.lua");
        Files.writeString(external, "function enginefx_external_value() return 84 end");
        assertThrows(IllegalStateException.class, () -> LuaNativeHarness.verify(external));
    }

    @Test void rejectsFractionalOrStringReturnValues() throws Exception
    {
        for (String value : new String[] { "84.5", "'84'" })
        {
            Path external = temporaryDirectory.resolve("wrong-return.lua");
            Files.writeString(external, "engine_record(84)\nfunction enginefx_external_value() return " + value + " end");
            assertThrows(IllegalStateException.class, () -> LuaNativeHarness.verify(external), value);
        }
    }

    @Test void refusesAModuleThatCatchesItsInstructionLimit() throws Exception
    {
        for (String protectedCall : new String[] {
            "pcall(function() local n=0 while n<1000000 do n=n+1 end end)",
            "pcall(function() xpcall(function() while true do end end, function(err) return err end) end)" })
        {
            Path external = temporaryDirectory.resolve("catch-limit.lua");
            Files.writeString(external, protectedCall + "\nengine_record(84)\nfunction enginefx_external_value() return 84 end");
            RuntimeException error = assertThrows(RuntimeException.class, () -> LuaNativeHarness.verify(external));
            assertTrue(error.getMessage().contains("Lua instruction limit exceeded"));
        }
    }

    @Test void protectedCallsKeepOrdinaryErrorAndMultipleReturnSemantics() throws Exception
    {
        Path external = temporaryDirectory.resolve("protected.lua");
        Files.writeString(external, "local ok,a,b,c = pcall(function() return 1,nil,3 end)\n"
            + "assert(ok and a==1 and b==nil and c==3)\n"
            + "local good,err = xpcall(function() error('test') end, function() return 'handled' end)\n"
            + "assert(not good and err=='handled')\nengine_record(84)\nfunction enginefx_external_value() return 84 end");
        assertEquals(84, LuaNativeHarness.verify(external).callbackValue());
    }

    @Test void hostCallbackRequiresAnIntegerWithoutCoercion() throws Exception
    {
        for (String value : new String[] { "84.5", "'84'", "0/0", "2147483648" })
        {
            Path external = temporaryDirectory.resolve("callback-type.lua");
            Files.writeString(external, "engine_record(" + value + ")\nfunction enginefx_external_value() return 84 end");
            assertThrows(RuntimeException.class, () -> LuaNativeHarness.verify(external), value);
        }
    }

    @Test void acceptsNonAsciiUtf8AndRejectsOversizedSource() throws Exception
    {
        Path external = temporaryDirectory.resolve("utf8.lua");
        Files.writeString(external, "assert(utf8.len('a\u00e7\u00e3o') == 4)\nengine_record(84)\n"
            + "function enginefx_external_value() return 84 end");
        assertEquals(84, LuaNativeHarness.verify(external).callbackValue());
        Path oversized = temporaryDirectory.resolve("oversized.lua");
        Files.write(oversized, new byte[1_048_577]);
        assertThrows(IllegalArgumentException.class, () -> LuaNativeHarness.verify(oversized));
    }
}
