package dev.skyjet.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Thin shims over the handful of blaze3d entry points that were renamed between 1.21.11 and 26.x.
 */
public final class SkyCompat {
    private SkyCompat() {
    }

    public static Camera mainCamera(Minecraft mc) {
        //? if >=26.2 {
        return mc.gameRenderer.mainCamera();
        //?} else {
        /*return mc.gameRenderer.getMainCamera();
        *///?}
    }

    /**
     * The view rotation, rebuilt from where the camera is actually looking.
     *
     * <p>The sky shader turns this back into a ray direction, so anything the game folds into the
     * view matrix lands on the sky as movement. View bobbing folds in three things: half a block
     * of sway, a roll, and a pitch of several degrees every step. For geometry at infinity all
     * three shift the reconstructed ray instead of the camera, and the sky lurches up and down as
     * you walk.
     *
     * <p>Stripping the translation only fixed the sway and left the pitch, so the sky still
     * bounced. Taking the rotation from the camera's own orientation drops the whole bob, plus
     * the hurt-tilt and anything else layered on later, in one go.
     */
    public static Matrix4f modelViewMatrix() {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc == null ? null : mainCamera(mc);
        if (camera != null) {
            return new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));
        }
        //? if >=26.2 {
        Matrix4f m = RenderSystem.getModelViewMatrixCopy();
        //?} else {
        /*Matrix4f m = new Matrix4f(RenderSystem.getModelViewMatrix());
        *///?}
        return m.setTranslation(0.0f, 0.0f, 0.0f);
    }

    public static void bindVertices(RenderPass pass, GpuBuffer buffer) {
        //? if >=26.2 {
        pass.setVertexBuffer(0, buffer.slice());
        //?} else {
        /*pass.setVertexBuffer(0, buffer);
        *///?}
    }

    public static net.minecraft.client.gui.screens.Screen currentScreen(Minecraft mc) {
        //? if >=26.2 {
        return mc.gui.screen();
        //?} else {
        /*return mc.screen;
        *///?}
    }

    public static void setScreen(Minecraft mc, net.minecraft.client.gui.screens.Screen screen) {
        //? if >=26.2 {
        mc.gui.setScreen(screen);
        //?} else {
        /*mc.setScreen(screen);
        *///?}
    }

    public static void drawTriangle(RenderPass pass) {
        //? if >=26.2 {
        pass.draw(3, 1, 0, 0);
        //?} else {
        /*pass.draw(0, 3);
        *///?}
    }
}
