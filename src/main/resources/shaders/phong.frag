#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;
uniform float shadowBias;

// Mengembalikan 0.0 (terang) sampai 1.0 (gelap/bayangan penuh)
float shadowCalculation(vec4 fragPosLight, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLight.xyz / fragPosLight.w;   // perspective divide
    proj = proj * 0.5 + 0.5;                         // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                    // di luar frustum cahaya

    // Bias dinamis berdasarkan kemiringan permukaan (anti Shadow Acne)
    float bias = max(shadowBias * (1.0 - dot(norm, lightDir)), shadowBias * 0.25);

    // PCF 3x3 supaya tepi bayangan lebih halus
    float shadow = 0.0;
    vec2 texel = 1.0 / textureSize(shadowMap, 0);
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closestDepth = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > closestDepth) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    if (diff <= 0.0) spec = 0.0; // tidak ada kilau di sisi yang membelakangi cahaya
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow Factor (0.2 jika sepenuhnya di dalam bayangan, 1.0 jika terang)
    float visibility = 1.0;
    if (useShadows) {
        float shadow = shadowCalculation(FragPosLightSpace, norm, lightDir);
        visibility = 1.0 - 0.8 * shadow;
    }

    // Combine Texture & Lighting
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + visibility * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
