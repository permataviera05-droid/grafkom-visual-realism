package com.grafkom.viewing3d;

import java.util.ArrayList;
import java.util.List;

/** Pabrik pembuat model 3D: Sphere, Cube, dan Plane (lantai). Semua winding CCW (tampak depan). */
public final class MeshFactory {
    private MeshFactory() {}

    /** Builder sederhana untuk mengumpulkan segitiga. */
    private static class Builder {
        final List<Float> pos = new ArrayList<>();
        final List<Float> uv = new ArrayList<>();

        void vertex(float[] p, float[] t) {
            pos.add(p[0]); pos.add(p[1]); pos.add(p[2]);
            uv.add(t[0]); uv.add(t[1]);
        }

        void triangle(float[] p0, float[] t0, float[] p1, float[] t1, float[] p2, float[] t2) {
            vertex(p0, t0); vertex(p1, t1); vertex(p2, t2);
        }

        /** Quad BL, BR, TR, TL (CCW) menjadi dua segitiga. */
        void quad(float[] bl, float[] br, float[] tr, float[] tl, float uMax, float vMax) {
            float[] uBL = {0, 0}, uBR = {uMax, 0}, uTR = {uMax, vMax}, uTL = {0, vMax};
            triangle(bl, uBL, br, uBR, tr, uTR);
            triangle(bl, uBL, tr, uTR, tl, uTL);
        }

        MeshData build() {
            float[] p = new float[pos.size()];
            float[] t = new float[uv.size()];
            for (int i = 0; i < p.length; i++) p[i] = pos.get(i);
            for (int i = 0; i < t.length; i++) t[i] = uv.get(i);
            return new MeshData(p, t);
        }
    }

    public static MeshData sphere(float radius, int stacks, int sectors) {
        Builder b = new Builder();
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                float[] p00 = spherePoint(radius, i, j, stacks, sectors);
                float[] p01 = spherePoint(radius, i, j + 1, stacks, sectors);
                float[] p10 = spherePoint(radius, i + 1, j, stacks, sectors);
                float[] p11 = spherePoint(radius, i + 1, j + 1, stacks, sectors);
                float[] t00 = {(float) j / sectors, 1f - (float) i / stacks};
                float[] t01 = {(float) (j + 1) / sectors, 1f - (float) i / stacks};
                float[] t10 = {(float) j / sectors, 1f - (float) (i + 1) / stacks};
                float[] t11 = {(float) (j + 1) / sectors, 1f - (float) (i + 1) / stacks};
                if (i != 0)           b.triangle(p00, t00, p01, t01, p11, t11); // di kutub atas segitiga ini degenerate
                if (i != stacks - 1)  b.triangle(p00, t00, p11, t11, p10, t10); // di kutub bawah segitiga ini degenerate
            }
        }
        return b.build();
    }

    private static float[] spherePoint(float r, int i, int j, int stacks, int sectors) {
        double theta = Math.PI * i / stacks;       // 0 (atas) .. PI (bawah)
        double phi = 2.0 * Math.PI * j / sectors;  // 0 .. 2PI
        return new float[]{
                (float) (r * Math.sin(theta) * Math.cos(phi)),
                (float) (r * Math.cos(theta)),
                (float) (r * Math.sin(theta) * Math.sin(phi))};
    }

    /** Kubus dengan titik pusat di (0,0,0) dan panjang sisi = size. */
    public static MeshData cube(float size) {
        float h = size / 2f;
        Builder b = new Builder();
        // +Z (depan)
        b.quad(new float[]{-h, -h, h}, new float[]{h, -h, h}, new float[]{h, h, h}, new float[]{-h, h, h}, 1, 1);
        // -Z (belakang)
        b.quad(new float[]{h, -h, -h}, new float[]{-h, -h, -h}, new float[]{-h, h, -h}, new float[]{h, h, -h}, 1, 1);
        // +X (kanan)
        b.quad(new float[]{h, -h, h}, new float[]{h, -h, -h}, new float[]{h, h, -h}, new float[]{h, h, h}, 1, 1);
        // -X (kiri)
        b.quad(new float[]{-h, -h, -h}, new float[]{-h, -h, h}, new float[]{-h, h, h}, new float[]{-h, h, -h}, 1, 1);
        // +Y (atas)
        b.quad(new float[]{-h, h, h}, new float[]{h, h, h}, new float[]{h, h, -h}, new float[]{-h, h, -h}, 1, 1);
        // -Y (bawah)
        b.quad(new float[]{-h, -h, -h}, new float[]{h, -h, -h}, new float[]{h, -h, h}, new float[]{-h, -h, h}, 1, 1);
        return b.build();
    }

    /** Bidang datar pada y = 0 (menghadap atas). Tekstur diulang uvRepeat kali. */
    public static MeshData plane(float halfSize, float uvRepeat) {
        Builder b = new Builder();
        float s = halfSize;
        b.quad(new float[]{-s, 0, s}, new float[]{s, 0, s}, new float[]{s, 0, -s}, new float[]{-s, 0, -s}, uvRepeat, uvRepeat);
        return b.build();
    }
}
