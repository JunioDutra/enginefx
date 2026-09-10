package br.com.engine.platform.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;
import static org.junit.jupiter.api.Assertions.*;

class VulkanShaderCompilerTest
{
    @Test void failedCompilationDoesNotPreventTheNextCompilation( )
    {
        assertThrows( IllegalStateException.class, () -> LwjglVulkanShaderCompiler.compileShader( "invalid", shaderc_glsl_vertex_shader, "invalid.vert" ) );
        var spirv = LwjglVulkanShaderCompiler.compileShader( "#version 450\nvoid main() { gl_Position = vec4(0, 0, 0, 1); }", shaderc_glsl_vertex_shader, "valid.vert" );
        try { assertEquals( 0x07230203, spirv.getInt( 0 ) ); }
        finally { MemoryUtil.memFree( spirv ); }
    }
}
