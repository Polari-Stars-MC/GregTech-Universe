#version 150

uniform sampler2D Sampler0;
uniform float Exposure;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 outColor = texture(Sampler0, texCoord0) * Exposure;
    outColor.a = 1;
    fragColor = outColor;
}
