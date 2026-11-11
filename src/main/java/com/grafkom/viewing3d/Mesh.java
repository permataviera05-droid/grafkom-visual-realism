package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL33.*;

/**
 * Mesh di GPU. Disimpan dalam DUA versi VAO:
 *  - flat   : normal per-face
 *  - smooth : normal per-vertex hasil rata-rata
 * Sehingga tombol F dapat berpindah seketika tanpa menghitung ulang.
 */
public class Mesh {
    private final int vaoFlat;
    private final int vaoSmooth;
    private final int vertexCount;

    public Mesh(MeshData data) {
        this.vertexCount = data.vertexCount();
        this.vaoFlat = createVao(data.interleave(data.flatNormals()));
        this.vaoSmooth = createVao(data.interleave(data.smoothNormals()));
    }

    private int createVao(float[] interleaved) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, interleaved, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);                    // aPos
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);      // aNormal
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);      // aTexCoords
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
        return vao;
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }
}
