#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

out vec2 UV;
out vec3 Pos;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 tModelViewMat;
uniform mat4 tProjMat;

uniform vec3 PlanetRenderPos;
uniform float PlanetRenderRadiusZoom;
uniform mat4 PlanetRotate;
uniform mat4 PlanetRingRotate;

void main() {
    vec3 RenderWorldPos = Position * PlanetRenderRadiusZoom * mat3(PlanetRotate) * mat3(PlanetRingRotate) + PlanetRenderPos;
    gl_Position = tProjMat * tModelViewMat * vec4(RenderWorldPos, 1.0);
    UV = UV0;
    Pos = RenderWorldPos;
}
