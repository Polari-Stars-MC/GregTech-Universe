#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;
out vec4 fragColor;

uniform mat4 iProjMat;
uniform mat4 iModelViewMat;
uniform int ScatterCount;

void main() {
    fragColor = texture(DiffuseSampler, texCoord);
}
