package io.github.schluid.hardmodeplus.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.hardmodeplus.feature.Features;
import io.github.schluid.hardmodeplus.feature.Injuries;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
	private static final Identifier[] HARDMODEPLUS$SLOWNESS = {Features.BACKSTAB_ID, Features.EXHAUST_ID,
			Features.BLAST_ID, Injuries.FRACTURE_ID};

	@WrapOperation(method = "getFieldOfViewModifier", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
	private double hardmodeplus$ignoreSlowness(AbstractClientPlayer player, Holder<Attribute> attribute,
			Operation<Double> original) {
		double value = original.call(player, attribute);
		if (attribute != Attributes.MOVEMENT_SPEED) {
			return value;
		}
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return value;
		}
		for (Identifier id : HARDMODEPLUS$SLOWNESS) {
			AttributeModifier slow = speed.getModifier(id);
			if (slow != null && slow.amount() > -1.0) {
				value /= 1.0 + slow.amount();
			}
		}
		return value;
	}
}
