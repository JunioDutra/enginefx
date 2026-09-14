#version 450
layout(location = 0) in vec2 fragTexCoord;
layout(location = 1) in vec4 fragColor;
layout(binding = 0) uniform sampler2D texSampler;
layout(location = 0) out vec4 outColor;
vec3 srgbToLinear(vec3 c) { return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), greaterThan(c, vec3(0.04045))); }
void main() {
    vec4 texColor = texture(texSampler, fragTexCoord);
    outColor = texColor * fragColor;
    outColor.rgb = srgbToLinear(texColor.rgb) * srgbToLinear(fragColor.rgb);
}
