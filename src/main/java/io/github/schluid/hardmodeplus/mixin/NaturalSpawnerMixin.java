package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.feature.Features;
import io.github.schluid.hardmodeplus.mobs.SpawnCount;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
	@Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
	private static void hardmodeplus$safeSurface(ServerLevel level, Mob mob, double distance,
			CallbackInfoReturnable<Boolean> cir) {
		if (mob.getType().getCategory() != MobCategory.MONSTER) {
			return;
		}
		BlockPos pos = mob.blockPosition();
		int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
		if (Features.blocksSurfaceSpawn(level, pos.getY(), surface) || !SpawnCount.keep(level, mob)) {
			cir.setReturnValue(false);
		}
	}
}
