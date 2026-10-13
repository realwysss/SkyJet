package dev.skyjet.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.skyjet.SkyJet;
import net.minecraft.client.renderer.RenderPipelines;

//? if >=26 {
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
//?} else {
/*import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.DepthTestFunction;
*///?}

//? if >=26.2 {
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
//?} else {
/*import com.mojang.blaze3d.vertex.VertexFormat;
*///?}

/**
 * Builds the SkyJet render pipelines. The GPU pipeline builder changed shape between 1.21.11,
 * 26.1 and 26.2, so the configuration calls are version split while the shaders stay shared.
 */
public final class SkyPipelines {
    public static final String UNIFORM_BLOCK = "SkyJetData";
    public static final String WATERMARK_SAMPLER = "WatermarkSampler";
    public static final String[] SKYBOX_SAMPLERS = {
            "Skybox0Sampler", "Skybox1Sampler", "Skybox2Sampler",
            "Skybox3Sampler", "Skybox4Sampler", "Skybox5Sampler"
    };

    private static RenderPipeline sky;

    private SkyPipelines() {
    }

    public static RenderPipeline sky() {
        if (sky == null) {
            sky = buildSky();
        }
        return sky;
    }

    private static RenderPipeline buildSky() {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(SkyJet.id("pipeline/skyjet_sky"))
                .withVertexShader(SkyJet.id("core/skyjet_sky"))
                .withFragmentShader(SkyJet.id("core/skyjet_sky"))
                .withCull(false);

        //? if >=26.2 {
        BindGroupLayout.Builder bindGroup = BindGroupLayout.builder()
                .withUniform(UNIFORM_BLOCK, UniformType.UNIFORM_BUFFER)
                .withSampler(SkyNoise.SAMPLER)
                .withSampler(WATERMARK_SAMPLER);
        for (String sampler : SKYBOX_SAMPLERS) {
            bindGroup.withSampler(sampler);
        }
        builder.withBindGroupLayout(bindGroup.build());
        builder.withVertexBinding(0, DefaultVertexFormat.POSITION);
        builder.withPrimitiveTopology(PrimitiveTopology.TRIANGLES);
        builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        //?} elif >=26 {
        /*builder.withUniform(UNIFORM_BLOCK, UniformType.UNIFORM_BUFFER);
        builder.withSampler(SkyNoise.SAMPLER);
        builder.withSampler(WATERMARK_SAMPLER);
        for (String sampler : SKYBOX_SAMPLERS) {
            builder.withSampler(sampler);
        }
        builder.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES);
        builder.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT));
        builder.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false));
        *///?} else {
        /*builder.withUniform(UNIFORM_BLOCK, UniformType.UNIFORM_BUFFER);
        builder.withSampler(SkyNoise.SAMPLER);
        builder.withSampler(WATERMARK_SAMPLER);
        for (String sampler : SKYBOX_SAMPLERS) {
            builder.withSampler(sampler);
        }
        builder.withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLES);
        builder.withBlend(BlendFunction.TRANSLUCENT);
        builder.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST);
        builder.withDepthWrite(false);
        *///?}

        return builder.build();
    }

    public static void invalidate() {
        sky = null;
    }
}
