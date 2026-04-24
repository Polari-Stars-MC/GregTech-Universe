#version 150

in vec3 Position;

out vec4 OutStarColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform vec3 StarRenderPos;
uniform float StarRenderRadiusZoom;
uniform mat4 StarRotate;
uniform vec4 StarColor;

void main() {
    vec3 RotatePos = Position * StarRenderRadiusZoom * mat3(StarRotate);
    gl_Position = ProjMat * ModelViewMat * vec4(RotatePos + StarRenderPos, 1.0);
    OutStarColor = StarColor;
}
