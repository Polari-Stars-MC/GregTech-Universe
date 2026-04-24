#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 PlanetPos;

out float Light;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    Light = 1.0;
}
