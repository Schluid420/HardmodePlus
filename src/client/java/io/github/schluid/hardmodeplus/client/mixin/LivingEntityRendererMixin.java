package io.github.schluid.hardmodeplus.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.hardmodeplus.client.VariantSkins;
import io.github.schluid.hardmodeplus.mobs.Variants;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void hardmodeplus$rememberVariant(LivingEntity entity, LivingEntityRenderState state, float partialTick,
			CallbackInfo ci) {
		((FabricRenderState) state).setData(VariantSkins.VARIANT, Variants.shownKind(entity));
		((FabricRenderState) state).setData(VariantSkins.TYPE, entity.getType());
	}

	@WrapOperation(method = "getRenderType", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
	private Identifier hardmodeplus$variantSkin(LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state,
			Operation<Identifier> original) {
		Integer variant = ((FabricRenderState) state).getData(VariantSkins.VARIANT);
		EntityType<?> type = ((FabricRenderState) state).getData(VariantSkins.TYPE);
		if (variant != null && type != null) {
			Identifier skin = VariantSkins.skin(type, variant, state.isBaby);
			if (skin != null) {
				return skin;
			}
		}
		return original.call(renderer, state);
	}
}
