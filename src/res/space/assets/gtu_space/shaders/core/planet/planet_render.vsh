#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out vec2 UV;
out vec3 Pos;
out vec3 WorldNormal;
out vec3 ReferColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 tModelViewMat;
uniform mat4 tProjMat;

uniform vec3 PlanetRenderPos;
uniform vec3 PlanetRealPos;
uniform float PlanetRenderRadiusZoom;
uniform mat3 PlanetRotate;
uniform mat3 iPlanetRotate;

void main() {
    vec3 RotatePos = Position * PlanetRenderRadiusZoom * PlanetRotate;
    Pos = RotatePos;
    ReferColor = vec3(1.0);

    gl_Position = tProjMat * tModelViewMat * vec4(RotatePos + PlanetRenderPos, 1.0);

    WorldNormal = normalize(iPlanetRotate * Normal);
    UV = UV0;
}
