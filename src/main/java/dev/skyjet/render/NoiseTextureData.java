package dev.skyjet.render;

import java.util.Random;

/**
 * Builds the tiling noise lookup the sky shader samples instead of hashing per pixel.
 *
 * <p>Evaluating value noise from hashes costs eight hashes per 3D sample, and nebulae alone were
 * roughly ninety percent of the shader's frame time. A single bilinear texture fetch replaces
 * all of it, and the hardware filtering is smoother than the hash lattice was.
 *
 * <p>Channel layout, matching the sampling helpers in {@code skyjet_sky.fsh}:
 * <ul>
 *   <li>{@code r} — field A at (x, y), the z slice for 3D noise</li>
 *   <li>{@code g} — field A at (x + 37, y + 17), the next z slice</li>
 *   <li>{@code b} — field B at (x, y), used for 2D noise</li>
 *   <li>{@code a} — field B offset, kept for future use</li>
 * </ul>
 * Everything wraps at {@link #SIZE}, so the texture must be sampled with repeat addressing.
 */
public final class NoiseTextureData {
    public static final int SIZE = 256;
    private static final int MASK = SIZE - 1;
    private static final int Z_OFFSET_X = 37;
    private static final int Z_OFFSET_Y = 17;

    private NoiseTextureData() {
    }

    /** Returns SIZE*SIZE pixels packed as ABGR, ready for {@code NativeImage.setPixelABGR}. */
    public static int[] generate() {
        Random random = new Random(0x5C1_7E7L);
        int[] fieldA = new int[SIZE * SIZE];
        int[] fieldB = new int[SIZE * SIZE];
        for (int i = 0; i < fieldA.length; i++) {
            fieldA[i] = random.nextInt(256);
            fieldB[i] = random.nextInt(256);
        }

        int[] pixels = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int here = y * SIZE + x;
                int shifted = (((y + Z_OFFSET_Y) & MASK) * SIZE) + ((x + Z_OFFSET_X) & MASK);
                int r = fieldA[here];
                int g = fieldA[shifted];
                int b = fieldB[here];
                int a = fieldB[shifted];
                pixels[here] = (a << 24) | (b << 16) | (g << 8) | r;
            }
        }
        return pixels;
    }
}
