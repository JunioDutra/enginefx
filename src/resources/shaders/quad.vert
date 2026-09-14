#version 450
layout(location = 0) in vec2 inPosition;
layout(location = 1) in vec2 inTexCoord;
layout(location = 2) in vec4 inColor;
layout(push_constant) uniform Constants {
    mat4 projection;
} push;
layout(location = 0) out vec2 fragTexCoord;
layout(location = 1) out vec4 fragColor;
void main() {
    gl_Position = push.projection * vec4(inPosition, 0.0, 1.0);
    fragTexCoord = inTexCoord;
    fragColor = inColor;
}
