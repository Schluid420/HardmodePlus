package io.github.schluid.hardmodeplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.schluid.hardmodeplus.mobs.Variants;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;

// Glowing eyes for variant mobs, needs variant/<mob>_eyes.png
public class VariantEyes<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
	public VariantEyes(RenderLayerParent<S, M> parent) {
		super(parent);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void register() {
		LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
			if (type == EntityTypes.ZOMBIE || type == EntityTypes.SKELETON || type == EntityTypes.CREEPER) {
				helper.register(new VariantEyes(renderer));
			}
		});
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
		if (state.isInvisible) {
			return;
		}
		Integer variant = ((FabricRenderState) state).getData(VariantSkins.VARIANT);
		EntityType<?> type = ((FabricRenderState) state).getData(VariantSkins.TYPE);
		if (variant == null || type == null || variant <= Variants.NORMAL) {
			return;
		}
		Identifier eyes = VariantSkins.eyes(type, state.isBaby);
		if (eyes != null) {
			collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(eyes), light,
					OverlayTexture.NO_OVERLAY, state.outlineColor);
		}
	}
}
