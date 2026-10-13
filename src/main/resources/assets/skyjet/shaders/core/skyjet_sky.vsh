#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

out vec3 rayDir;

void main() {
    vec2 ndc = Position.xy;
    gl_Position = vec4(ndc, 0.0, 1.0);

    mat4 invViewProj = inverse(ProjMat * ModelViewMat);
    vec4 farPoint = invViewProj * vec4(ndc, 0.5, 1.0);
    rayDir = farPoint.xyz / farPoint.w;
}
