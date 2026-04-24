#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out float Light;
out vec2 UV;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 tModelViewMat;
uniform mat4 tProjMat;

uniform vec3 PlanetRenderPos;
uniform vec3 PlanetRealPos;
uniform float PlanetRenderRadiusZoom;
uniform mat4 PlanetRotate;

void main() {
    vec3 RotatePos = Position * PlanetRenderRadiusZoom * mat3(PlanetRotate);
    gl_Position = tProjMat * tModelViewMat * vec4(RotatePos + PlanetRenderPos, 1.0);

    vec3 lightDir = normalize(-PlanetRealPos);
    float brightness = max(dot(Normal * mat3(PlanetRotate), lightDir), 0.0);

    Light = brightness + 0.000001;
    UV = UV0;
}
