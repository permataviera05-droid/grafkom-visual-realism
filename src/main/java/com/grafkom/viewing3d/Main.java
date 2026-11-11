package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite - Minggu 11 Grafika Komputer.
 *
 * KONTROL KEYBOARD:
 *  B = Phong <-> Blinn-Phong        F = Flat <-> Smooth Shading
 *  Z = Z-Buffer ON/OFF              C = Back-Face Culling ON/OFF
 *  T = Tekstur ON/OFF               N = Filter Nearest <-> Bilinear+Mipmap
 *  H = Bayangan (Shadow) ON/OFF     [ / ] = bias bayangan diperkecil / diperbesar, 0 = reset bias
 *  L = Cahaya berputar ON/OFF       SPACE = objek berputar ON/OFF
 *  W = Wireframe ON/OFF             V = VSync ON/OFF (untuk mengukur FPS)
 *  Panah = putar kamera             Q / E = zoom in / zoom out        ESC = keluar
 */
public class Main {
    private static final float DEFAULT_BIAS = 0.002f;

    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    // ---- State fitur (diubah lewat keyboard) ----
    private boolean blinnPhong = true;
    private boolean smooth = true;
    private boolean depthTest = true;
    private boolean cullFace = true;
    private boolean useTexture = true;
    private boolean mipmapped = true;
    private boolean shadows = true;
    private boolean animateLight = false;
    private boolean rotateObjects = true;
    private boolean wireframe = false;
    private boolean vsync = false;
    private float shadowBias = DEFAULT_BIAS;

    // ---- Objek & resource ----
    private ShaderProgram sceneShader, depthShader;
    private Mesh sphere, cube, ground;
    private Texture2D brickTexture, checkerTexture;
    private ShadowMapFBO shadowFBO;
    private final Camera camera = new Camera();

    private float objectAngle = 0f;
    private float lightAngle = 2.4f;
    private int lastFps = 0;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        init();
        loop();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    // =====================================================================
    // INISIALISASI
    // =====================================================================
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE); // diperlukan di macOS
        glfwWindowHint(GLFW_SAMPLES, 4);                        // anti-aliasing tepi (MSAA)

        window = glfwCreateWindow(fbWidth, fbHeight, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window GLFW");

        glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) onKeyPressed(key);
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);
        GL.createCapabilities();
        glEnable(GL_MULTISAMPLE);

        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");

        sphere = new Mesh(MeshFactory.sphere(1.0f, 16, 24));
        cube = new Mesh(MeshFactory.cube(1.5f));
        ground = new Mesh(MeshFactory.plane(10f, 10f));

        brickTexture = new Texture2D("/textures/brick.png");
        checkerTexture = new Texture2D("/textures/checker.png");
        shadowFBO = new ShadowMapFBO();

        printHelp();
    }

    // =====================================================================
    // INPUT
    // =====================================================================
    private void onKeyPressed(int key) {
        switch (key) {
            case GLFW_KEY_B: blinnPhong = !blinnPhong; break;
            case GLFW_KEY_F: smooth = !smooth; break;
            case GLFW_KEY_Z: depthTest = !depthTest; break;
            case GLFW_KEY_C: cullFace = !cullFace; break;
            case GLFW_KEY_T: useTexture = !useTexture; break;
            case GLFW_KEY_N:
                mipmapped = !mipmapped;
                brickTexture.setMipmapped(mipmapped);
                checkerTexture.setMipmapped(mipmapped);
                break;
            case GLFW_KEY_H: shadows = !shadows; break;
            case GLFW_KEY_L: animateLight = !animateLight; break;
            case GLFW_KEY_SPACE: rotateObjects = !rotateObjects; break;
            case GLFW_KEY_W: wireframe = !wireframe; break;
            case GLFW_KEY_V:
                vsync = !vsync;
                glfwSwapInterval(vsync ? 1 : 0);
                break;
            case GLFW_KEY_LEFT_BRACKET:  shadowBias *= 0.5f; break;
            case GLFW_KEY_RIGHT_BRACKET: shadowBias *= 2.0f; break;
            case GLFW_KEY_0: shadowBias = DEFAULT_BIAS; break;
            case GLFW_KEY_ESCAPE: glfwSetWindowShouldClose(window, true); break;
            default: return;
        }
        System.out.println(statusText());
        updateTitle();
    }

    private void handleContinuousInput(float dt) {
        float rot = 1.5f * dt;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  camera.rotate(-rot, 0f);
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) camera.rotate(rot, 0f);
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    camera.rotate(0f, rot);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  camera.rotate(0f, -rot);
        if (glfwGetKey(window, GLFW_KEY_Q) == GLFW_PRESS)     camera.zoom(-6f * dt);
        if (glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS)     camera.zoom(6f * dt);
    }

    // =====================================================================
    // LOOP UTAMA
    // =====================================================================
    private void loop() {
        double lastTime = glfwGetTime();
        double fpsTimer = lastTime;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            glfwPollEvents();
            handleContinuousInput(dt);
            if (rotateObjects) objectAngle += dt * 0.6f;
            if (animateLight) lightAngle += dt * 0.6f;

            render();
            glfwSwapBuffers(window);

            frames++;
            if (now - fpsTimer >= 1.0) {
                lastFps = frames;
                frames = 0;
                fpsTimer = now;
                updateTitle();
            }
        }
    }

    // =====================================================================
    // RENDER (2 PASS)
    // =====================================================================
    private Matrix4f sphereModel() {
        return new Matrix4f().translate(-1.5f, 1.0f, 0f).rotateY(objectAngle);
    }

    private Matrix4f cubeModel() {
        return new Matrix4f().translate(2.0f, 0.75f, 0f).rotateY(-objectAngle * 0.5f);
    }

    private void render() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }
        if (fbWidth == 0 || fbHeight == 0) return; // window di-minimize

        Vector3f lightPos = new Vector3f(7f * (float) Math.cos(lightAngle), 8f, 7f * (float) Math.sin(lightAngle));
        Matrix4f lightProjection = new Matrix4f().ortho(-10f, 10f, -10f, 10f, 1f, 30f);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0f, 0f, 0f), new Vector3f(0f, 1f, 0f));
        Matrix4f lightSpaceMatrix = new Matrix4f(lightProjection).mul(lightView);

        // ---------- PASS 1: Depth Pass (render dari sudut pandang CAHAYA) ----------
        if (shadows) {
            shadowFBO.bindForWriting();
            HSRManager.setWireframe(false);
            HSRManager.setupDepthTest(true);       // pass ini WAJIB memakai depth test
            HSRManager.setupBackFaceCulling(false);

            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
            depthShader.setMat4("model", sphereModel());
            sphere.draw(smooth);
            depthShader.setMat4("model", cubeModel());
            cube.draw(false);

            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ---------- PASS 2: Lighting Pass (render dari sudut pandang KAMERA) ----------
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.55f, 0.75f, 0.95f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);
        HSRManager.setWireframe(wireframe);

        Matrix4f projection = new Matrix4f().perspective(
                (float) Math.toRadians(45.0), (float) fbWidth / fbHeight, 0.1f, 100f);

        sceneShader.use();
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("view", camera.getViewMatrix());
        sceneShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", camera.getPosition());
        sceneShader.setVec3("lightColor", 1f, 1f, 1f);
        sceneShader.setBool("useBlinnPhong", blinnPhong);
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setBool("useShadows", shadows);
        sceneShader.setFloat("shadowBias", shadowBias);
        sceneShader.setInt("textureSampler", 0);
        sceneShader.setInt("shadowMap", 1);
        shadowFBO.bindDepthTexture(1);

        // Urutan gambar sengaja: bola -> kubus -> lantai.
        // Jika Z-Buffer dimatikan (tombol Z), lantai yang digambar terakhir akan menimpa objek (efek HSR terlihat jelas).
        brickTexture.bind(0);
        sceneShader.setVec3("objectColor", 0.8f, 0.3f, 0.2f);
        sceneShader.setMat4("model", sphereModel());
        sphere.draw(smooth);

        sceneShader.setVec3("objectColor", 0.2f, 0.5f, 0.8f);
        sceneShader.setMat4("model", cubeModel());
        cube.draw(false);

        checkerTexture.bind(0);
        sceneShader.setVec3("objectColor", 0.6f, 0.6f, 0.6f);
        sceneShader.setMat4("model", new Matrix4f());
        ground.draw(false);
    }

    // =====================================================================
    // TEKS STATUS
    // =====================================================================
    private String statusText() {
        return String.format(Locale.US,
                "[%s] [%s] [Depth:%s] [Cull:%s] [Tekstur:%s/%s] [Bayangan:%s] [Bias:%.5f]",
                blinnPhong ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                depthTest ? "ON" : "OFF",
                cullFace ? "ON" : "OFF",
                useTexture ? "ON" : "OFF",
                mipmapped ? "Mipmap" : "Nearest",
                shadows ? "ON" : "OFF",
                shadowBias);
    }

    private void updateTitle() {
        glfwSetWindowTitle(window, "FPS: " + lastFps + "  " + statusText() + (vsync ? " [VSync]" : ""));
    }

    private void printHelp() {
        System.out.println("=== 3D Visual Realism Suite ===");
        System.out.println("B=Phong/Blinn-Phong  F=Flat/Smooth  Z=Z-Buffer  C=Back-Face Culling");
        System.out.println("T=Tekstur  N=Nearest/Mipmap  H=Bayangan  [ ]=Bias  0=Reset bias");
        System.out.println("L=Cahaya berputar  SPACE=Objek berputar  W=Wireframe  V=VSync");
        System.out.println("Panah=Putar kamera  Q/E=Zoom  ESC=Keluar");
        System.out.println(statusText());
    }
}
