#version 150

in vec2 UV;
in vec3 Pos;
in vec3 WorldNormal;
in vec3 ReferColor;

out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec3 PlanetRenderPos;
uniform vec3 PlanetRealPos;
uniform float PlanetR;

void main() {
    vec3 normal = normalize(WorldNormal);
    vec3 lightDir = normalize(-PlanetRealPos);
    float brightness = max(dot(normal, lightDir), 0.0);

    vec4 color = texture(Sampler0, UV);
    vec4 night_color = texture(Sampler1, UV);

    color *= brightness;
    night_color *= (1.0 - brightness) * 0.75;
    night_color.a = 1.0;

    fragColor = (color + night_color) * vec4(ReferColor, 1.0);
}
