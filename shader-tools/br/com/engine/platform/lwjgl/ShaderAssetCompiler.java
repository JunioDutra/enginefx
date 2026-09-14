package br.com.engine.platform.lwjgl;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import org.lwjgl.system.MemoryUtil;

/** Build-only entry point. Production rendering never links shaderc. */
public final class ShaderAssetCompiler
{
    private ShaderAssetCompiler() { }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 3) throw new IllegalArgumentException("Usage: source vert|frag output");
        Path source = Path.of(args[0]);
        int kind = switch (args[1]) { case "vert" -> shaderc_glsl_vertex_shader; case "frag" -> shaderc_glsl_fragment_shader;
            default -> throw new IllegalArgumentException("Unknown shader kind: " + args[1]); };
        Path output = Path.of(args[2]);
        ByteBuffer bytes = LwjglVulkanShaderCompiler.compileShader(Files.readString(source), kind, source.getFileName().toString());
        try
        {
            byte[] compiled = new byte[bytes.remaining()];
            bytes.get(compiled);
            Files.createDirectories(output.getParent());
            Files.write(output, compiled);
        }
        finally { MemoryUtil.memFree(bytes); }
    }
}
