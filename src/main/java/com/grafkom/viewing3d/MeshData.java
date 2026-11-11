package com.grafkom.viewing3d;

import java.util.HashMap;
import java.util.Map;

/**
 * Data mentah mesh berupa daftar segitiga (tiap 3 vertex = 1 face).
 * Kelas ini murni Java (tanpa OpenGL) sehingga mudah diuji.
 * Menghitung dua jenis vektor normal:
 *  - Flat   : 1 normal per face (face normal)
 *  - Smooth : rata-rata normal semua face yang berbagi posisi vertex yang sama
 */
public class MeshData {
    public final float[] positions; // 3 float per vertex
    public final float[] uvs;       // 2 float per vertex

    public MeshData(float[] positions, float[] uvs) {
        this.positions = positions;
        this.uvs = uvs;
    }

    public int vertexCount() { return positions.length / 3; }

    /** Normal face (sudah dinormalisasi) untuk segitiga yang dimulai pada vertex ke-t. */
    private float[] faceNormal(int t) {
        int a = t * 3, b = (t + 1) * 3, c = (t + 2) * 3;
        float ux = positions[b] - positions[a], uy = positions[b + 1] - positions[a + 1], uz = positions[b + 2] - positions[a + 2];
        float vx = positions[c] - positions[a], vy = positions[c + 1] - positions[a + 1], vz = positions[c + 2] - positions[a + 2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-8f) return new float[]{0f, 0f, 0f}; // segitiga degenerate
        return new float[]{nx / len, ny / len, nz / len};
    }

    /** Flat shading: ketiga vertex satu face memakai normal face yang sama. */
    public float[] flatNormals() {
        float[] normals = new float[positions.length];
        for (int t = 0; t < vertexCount(); t += 3) {
            float[] n = faceNormal(t);
            for (int k = 0; k < 3; k++) {
                normals[(t + k) * 3] = n[0];
                normals[(t + k) * 3 + 1] = n[1];
                normals[(t + k) * 3 + 2] = n[2];
            }
        }
        return normals;
    }

    /** Smooth shading: N_vertex = (SUM N_face_i) / ||SUM N_face_i||. */
    public float[] smoothNormals() {
        Map<String, float[]> sums = new HashMap<>();
        for (int t = 0; t < vertexCount(); t += 3) {
            float[] n = faceNormal(t);
            for (int k = 0; k < 3; k++) {
                float[] sum = sums.computeIfAbsent(key(t + k), s -> new float[3]);
                sum[0] += n[0];
                sum[1] += n[1];
                sum[2] += n[2];
            }
        }
        float[] normals = new float[positions.length];
        for (int v = 0; v < vertexCount(); v++) {
            float[] s = sums.get(key(v));
            float len = (float) Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
            if (len < 1e-8f) { s = new float[]{0f, 1f, 0f}; len = 1f; }
            normals[v * 3] = s[0] / len;
            normals[v * 3 + 1] = s[1] / len;
            normals[v * 3 + 2] = s[2] / len;
        }
        return normals;
    }

    /** Kunci posisi (dibulatkan) agar vertex di posisi sama (mis. jahitan UV) dianggap satu titik. */
    private String key(int v) {
        return Math.round(positions[v * 3] * 10000f) + "," +
               Math.round(positions[v * 3 + 1] * 10000f) + "," +
               Math.round(positions[v * 3 + 2] * 10000f);
    }

    /** Susun data: [pos(3) normal(3) uv(2)] per vertex. */
    public float[] interleave(float[] normals) {
        int n = vertexCount();
        float[] out = new float[n * 8];
        for (int v = 0; v < n; v++) {
            out[v * 8] = positions[v * 3];
            out[v * 8 + 1] = positions[v * 3 + 1];
            out[v * 8 + 2] = positions[v * 3 + 2];
            out[v * 8 + 3] = normals[v * 3];
            out[v * 8 + 4] = normals[v * 3 + 1];
            out[v * 8 + 5] = normals[v * 3 + 2];
            out[v * 8 + 6] = uvs[v * 2];
            out[v * 8 + 7] = uvs[v * 2 + 1];
        }
        return out;
    }
}
