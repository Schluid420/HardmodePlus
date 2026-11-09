package io.github.schluid.hardmodeplus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.schluid.hardmodeplus.mobs.SpawnCount;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.LocalMobCapCalculator$MobCounts")
public abstract class MobCountsMixin {
	@WrapOperation(method = "canSpawn", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
	private int hardmodeplus$localCap(MobCategory category, Operation<Integer> original) {
		return SpawnCount.cap(category, original.call(category));
	}
}
