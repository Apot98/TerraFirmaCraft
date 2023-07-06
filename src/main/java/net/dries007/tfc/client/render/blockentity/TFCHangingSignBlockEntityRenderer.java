/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.client.render.blockentity;

import java.util.Map;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.mixin.client.accessor.SignRendererAccessor;

public class TFCHangingSignBlockEntityRenderer extends HangingSignRenderer
{
    private final Map<WoodType, HangingSignModel> hangingSignModels;

    public TFCHangingSignBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
        this(context, TFCBlocks.WOODS.keySet()
            .stream()
            .map(map -> new TFCSignBlockEntityRenderer.SignModelData(
                TerraFirmaCraft.MOD_ID,
                map.getSerializedName(),
                map.getVanillaWoodType()
            )));
    }

    public TFCHangingSignBlockEntityRenderer(BlockEntityRendererProvider.Context context, Stream<TFCSignBlockEntityRenderer.SignModelData> blocks)
    {
        super(context);

        ImmutableMap.Builder<WoodType, HangingSignModel> modelBuilder = ImmutableMap.builder();
        blocks.forEach(data -> {
            modelBuilder.put(data.type(), new HangingSignModel(context.bakeLayer(new ModelLayerLocation(new ResourceLocation(data.domain(), "hanging_sign/" + data.name()), "main"))));
        });
        this.hangingSignModels = modelBuilder.build();
    }

    @Override
    public void render(SignBlockEntity sign, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        final BlockState blockstate = sign.getBlockState();
        final SignBlock signblock = (SignBlock)blockstate.getBlock();
        final WoodType woodtype = SignBlock.getWoodType(signblock);
        final HangingSignRenderer.HangingSignModel model = this.hangingSignModels.get(woodtype);
        model.evaluateVisibleParts(blockstate);
        ((SignRendererAccessor) (Object) this).invoke$renderSignWithText(sign, poseStack, buffer, light, overlay, blockstate, signblock, woodtype, model);
    }
}