#version 150

in float Light;
in vec2 UV;

out vec4 fragColor;

uniform sampler2D Sampler1;

void main() {
    vec4 color = texture(Sampler1, UV);
    fragColor = color * Light;
}
