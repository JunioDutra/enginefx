package br.com.engine.platform.lwjgl;

import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_initialize;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_initialize;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_into_spv;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_compilation_status;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compilation_status_success;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_bytes;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_get_error_message;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_result_release;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compile_options_release;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_compiler_release;

import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

public class LwjglVulkanShaderCompiler
{
	public static ByteBuffer compileShader( String source, int kind, String name )
	{
		long compiler = shaderc_compiler_initialize( );
		if( compiler == 0 ) throw new IllegalStateException( "Cannot initialize shader compiler" );
		long options = 0;
		long result = 0;
		try
		{
			options = shaderc_compile_options_initialize( );
			if( options == 0 ) throw new IllegalStateException( "Cannot initialize shader options" );
			result = shaderc_compile_into_spv( compiler, source, kind, name, "main", options );
			if( result == 0 ) throw new IllegalStateException( "Shader compiler returned no result: " + name );
			if( shaderc_result_get_compilation_status( result ) != shaderc_compilation_status_success )
				throw new IllegalStateException( "Failed to compile shader " + name + ": " + shaderc_result_get_error_message( result ) );
			ByteBuffer spv = shaderc_result_get_bytes( result );
			ByteBuffer output = MemoryUtil.memAlloc( spv.remaining( ) );
			output.put( spv ).flip( );
			return output;
		}
		finally
		{
			if( result != 0 ) shaderc_result_release( result );
			if( options != 0 ) shaderc_compile_options_release( options );
			shaderc_compiler_release( compiler );
		}
	}
}
