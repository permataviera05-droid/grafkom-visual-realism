package com.grafkom.viewing3d;

import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/** Utilitas kecil untuk membaca file dari folder src/main/resources. */
public final class IOUtil {
    private IOUtil() {}

    public static byte[] readBytes(String path) {
        try (InputStream in = IOUtil.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource tidak ditemukan: " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Gagal membaca resource: " + path, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    public static ByteBuffer readToBuffer(String path) {
        byte[] bytes = readBytes(path);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }
}
