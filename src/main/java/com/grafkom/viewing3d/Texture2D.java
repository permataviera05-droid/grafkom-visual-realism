package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.stb.STBImage.*;

/** Membaca gambar PNG/JPG dengan STBImage lalu memuatnya ke GPU sebagai tekstur 2D. */
public class Texture2D {
    private final int textureID;

    /** @param resourcePath contoh: "/textures/brick.png" (di dalam src/main/resources) */
    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Wrap parameter
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        ByteBuffer fileData = IOUtil.readToBuffer(resourcePath);
        stbi_set_flip_vertically_on_load(true); // origin gambar di kiri-atas, UV di kiri-bawah
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load_from_memory(fileData, width, height, channels, 4);
            if (data == null) {
                throw new RuntimeException("Gagal memuat tekstur " + resourcePath + ": " + stbi_failure_reason());
            }
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, data);
            glGenerateMipmap(GL_TEXTURE_2D);
            stbi_image_free(data);
        }
        setMipmapped(true);
    }

    /**
     * true  = Bilinear + Mipmapping (GL_LINEAR_MIPMAP_LINEAR)
     * false = Nearest Neighbor (GL_NEAREST)
     */
    public void setMipmapped(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }
}
