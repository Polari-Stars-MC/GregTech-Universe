#version 150

in vec4 vertexColor;
in vec3 vertexPos;

uniform vec4 FogColor;
uniform float FogStart;
uniform float FogEnd;

out vec4 fragColor;

void main() {
    vec4 color = vertexColor;

    // Edge highlight: brighten pixels near cube edges
    vec3 absPos = abs(normalize(vertexPos));
    float maxCoord = max(absPos.x, max(absPos.y, absPos.z));
    float edgeFactor = smoothstep(0.7, 1.0, maxCoord);
    color.rgb += edgeFactor * 0.08;

    // Fog
    float dist = length(vertexPos);
    float fogFactor = clamp((FogEnd - dist) / (FogEnd - FogStart), 0.0, 1.0);
    color.rgb = mix(FogColor.rgb, color.rgb, fogFactor);

    fragColor = color;
}
