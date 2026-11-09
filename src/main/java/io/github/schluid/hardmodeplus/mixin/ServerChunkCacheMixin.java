package io.github.schluid.hardmodeplus.mixin;

import io.github.schluid.hardmodeplus.mobs.SpawnCount;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"))
	private void hardmodeplus$beginSpawning(ProfilerFiller profiler, CallbackInfo info) {
		SpawnCount.begin(level);
	}

	@Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
	private void hardmodeplus$endSpawning(ProfilerFiller profiler, CallbackInfo info) {
		SpawnCount.end();
	}
}
