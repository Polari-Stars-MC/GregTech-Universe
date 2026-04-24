#version 150

in vec2 UV;
in vec3 Pos;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec4 color = texture(Sampler0, UV);
    fragColor = color;
}
