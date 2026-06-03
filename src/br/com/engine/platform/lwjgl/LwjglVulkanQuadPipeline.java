package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_fragment_shader;
import static org.lwjgl.util.shaderc.Shaderc.shaderc_glsl_vertex_shader;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_SCISSOR;
import static org.lwjgl.vulkan.VK10.VK_DYNAMIC_STATE_VIEWPORT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_LOGIC_OP_COPY;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPushConstantRange;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

public class LwjglVulkanQuadPipeline implements AutoCloseable
{
	private static final String VERTEX_SHADER = 
		"#version 450\n" +
		"layout(location = 0) in vec2 inPosition;\n" +
		"layout(location = 1) in vec2 inTexCoord;\n" +
		"layout(location = 2) in vec4 inColor;\n" +
		"layout(push_constant) uniform Constants {\n" +
		"    mat4 projection;\n" +
		"} push;\n" +
		"layout(location = 0) out vec2 fragTexCoord;\n" +
		"layout(location = 1) out vec4 fragColor;\n" +
		"void main() {\n" +
		"    gl_Position = push.projection * vec4(inPosition, 0.0, 1.0);\n" +
		"    fragTexCoord = inTexCoord;\n" +
		"    fragColor = inColor;\n" +
		"}\n";

	private static final String FRAGMENT_SHADER = 
		"#version 450\n" +
		"layout(location = 0) in vec2 fragTexCoord;\n" +
		"layout(location = 1) in vec4 fragColor;\n" +
		"layout(binding = 0) uniform sampler2D texSampler;\n" +
		"layout(location = 0) out vec4 outColor;\n" +
		"void main() {\n" +
		"    vec4 texColor = texture(texSampler, fragTexCoord);\n" +
		"    outColor = texColor * fragColor;\n" +
		"}\n";

	private final LwjglVulkanDevice device;
	private final long descriptorSetLayout;
	private final long pipelineLayout;
	private final long pipeline;

	public LwjglVulkanQuadPipeline( LwjglVulkanDevice device, long renderPass )
	{
		this.device = device;
		this.descriptorSetLayout = createDescriptorSetLayout( );
		this.pipelineLayout = createPipelineLayout( );
		this.pipeline = createPipeline( renderPass );
	}

	public long getPipeline( )
	{
		return pipeline;
	}

	public long getPipelineLayout( )
	{
		return pipelineLayout;
	}

	public long getDescriptorSetLayout( )
	{
		return descriptorSetLayout;
	}

	@Override
	public void close( )
	{
		vkDestroyPipeline( device.getLogicalDevice( ), pipeline, null );
		vkDestroyPipelineLayout( device.getLogicalDevice( ), pipelineLayout, null );
		vkDestroyDescriptorSetLayout( device.getLogicalDevice( ), descriptorSetLayout, null );
	}

	private long createDescriptorSetLayout( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc( 1, stack )
				.binding( 0 )
				.descriptorType( VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER )
				.descriptorCount( 1 )
				.stageFlags( VK_SHADER_STAGE_FRAGMENT_BIT );

			VkDescriptorSetLayoutCreateInfo createInfo = VkDescriptorSetLayoutCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO )
				.pBindings( bindings );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateDescriptorSetLayout( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan descriptor set layout: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createPipelineLayout( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc( 1, stack )
				.stageFlags( VK_SHADER_STAGE_VERTEX_BIT )
				.offset( 0 )
				.size( 64 ); // mat4

			VkPipelineLayoutCreateInfo createInfo = VkPipelineLayoutCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO )
				.pSetLayouts( stack.longs( descriptorSetLayout ) )
				.pPushConstantRanges( pushConstantRanges );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreatePipelineLayout( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan pipeline layout: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createPipeline( long renderPass )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			ByteBuffer vertSpv = LwjglVulkanShaderCompiler.compileShader( VERTEX_SHADER, shaderc_glsl_vertex_shader, "quad.vert" );
			ByteBuffer fragSpv = LwjglVulkanShaderCompiler.compileShader( FRAGMENT_SHADER, shaderc_glsl_fragment_shader, "quad.frag" );

			long vertModule = createShaderModule( stack, vertSpv );
			long fragModule = createShaderModule( stack, fragSpv );

			MemoryUtil.memFree( vertSpv );
			MemoryUtil.memFree( fragSpv );

			VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc( 2, stack );
			
			shaderStages.get( 0 )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO )
				.stage( VK_SHADER_STAGE_VERTEX_BIT )
				.module( vertModule )
				.pName( stack.UTF8( "main" ) );

			shaderStages.get( 1 )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO )
				.stage( VK_SHADER_STAGE_FRAGMENT_BIT )
				.module( fragModule )
				.pName( stack.UTF8( "main" ) );

			VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc( 1, stack )
				.binding( 0 )
				.stride( 8 * 4 ) // 2 float pos + 2 float uv + 4 float color
				.inputRate( VK_VERTEX_INPUT_RATE_VERTEX );

			VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc( 3, stack );
			
			attributeDescriptions.get( 0 )
				.binding( 0 )
				.location( 0 )
				.format( VK_FORMAT_R32G32_SFLOAT )
				.offset( 0 );

			attributeDescriptions.get( 1 )
				.binding( 0 )
				.location( 1 )
				.format( VK_FORMAT_R32G32_SFLOAT )
				.offset( 2 * 4 );

			attributeDescriptions.get( 2 )
				.binding( 0 )
				.location( 2 )
				.format( VK_FORMAT_R32G32B32A32_SFLOAT )
				.offset( 4 * 4 );

			VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO )
				.pVertexBindingDescriptions( bindingDescription )
				.pVertexAttributeDescriptions( attributeDescriptions );

			VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO )
				.topology( VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST )
				.primitiveRestartEnable( false );

			VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO )
				.viewportCount( 1 )
				.scissorCount( 1 );

			VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO )
				.depthClampEnable( false )
				.rasterizerDiscardEnable( false )
				.polygonMode( VK_POLYGON_MODE_FILL )
				.lineWidth( 1.0f )
				.cullMode( VK_CULL_MODE_NONE )
				.frontFace( VK_FRONT_FACE_CLOCKWISE )
				.depthBiasEnable( false );

			VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO )
				.sampleShadingEnable( false )
				.rasterizationSamples( VK_SAMPLE_COUNT_1_BIT );

			VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc( 1, stack )
				.colorWriteMask( VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT )
				.blendEnable( true )
				.srcColorBlendFactor( VK_BLEND_FACTOR_SRC_ALPHA )
				.dstColorBlendFactor( VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA )
				.colorBlendOp( VK_BLEND_OP_ADD )
				.srcAlphaBlendFactor( VK_BLEND_FACTOR_SRC_ALPHA )
				.dstAlphaBlendFactor( VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA )
				.alphaBlendOp( VK_BLEND_OP_ADD );

			VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO )
				.logicOpEnable( false )
				.logicOp( VK_LOGIC_OP_COPY )
				.pAttachments( colorBlendAttachment );

			VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO )
				.pDynamicStates( stack.ints( VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR ) );

			VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc( 1, stack )
				.sType( VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO )
				.pStages( shaderStages )
				.pVertexInputState( vertexInputInfo )
				.pInputAssemblyState( inputAssembly )
				.pViewportState( viewportState )
				.pRasterizationState( rasterizer )
				.pMultisampleState( multisampling )
				.pColorBlendState( colorBlending )
				.pDynamicState( dynamicState )
				.layout( pipelineLayout )
				.renderPass( renderPass )
				.subpass( 0 )
				.basePipelineHandle( 0 )
				.basePipelineIndex( -1 );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateGraphicsPipelines( device.getLogicalDevice( ), 0, pipelineInfo, null, pointer );

			vkDestroyShaderModule( device.getLogicalDevice( ), vertModule, null );
			vkDestroyShaderModule( device.getLogicalDevice( ), fragModule, null );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan graphics pipeline: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createShaderModule( MemoryStack stack, ByteBuffer spirv )
	{
		VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc( stack )
			.sType( VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO )
			.pCode( spirv );

		LongBuffer pointer = stack.mallocLong( 1 );
		int result = vkCreateShaderModule( device.getLogicalDevice( ), createInfo, null, pointer );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to create Vulkan shader module: " + result );
		}

		return pointer.get( 0 );
	}
}