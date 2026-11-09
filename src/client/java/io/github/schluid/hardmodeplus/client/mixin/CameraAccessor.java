package io.github.schluid.hardmodeplus.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
	@Accessor("eyeHeight")
	float hardmodeplus$eyeHeight();

	@Accessor("eyeHeight")
	void hardmodeplus$setEyeHeight(float value);
}
