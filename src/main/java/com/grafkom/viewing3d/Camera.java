package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Kamera orbit: berputar mengelilingi satu titik target. */
public class Camera {
    private float yaw = 0.6f;
    private float pitch = 0.45f;
    private float distance = 10f;
    private final Vector3f target = new Vector3f(0f, 1f, 0f);

    public void rotate(float dYaw, float dPitch) {
        yaw += dYaw;
        pitch = Math.max(0.05f, Math.min(1.5f, pitch + dPitch));
    }

    public void zoom(float delta) {
        distance = Math.max(3f, Math.min(30f, distance + delta));
    }

    public Vector3f getPosition() {
        return new Vector3f(
                target.x + distance * (float) (Math.cos(pitch) * Math.sin(yaw)),
                target.y + distance * (float) Math.sin(pitch),
                target.z + distance * (float) (Math.cos(pitch) * Math.cos(yaw)));
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(getPosition(), target, new Vector3f(0f, 1f, 0f));
    }
}
