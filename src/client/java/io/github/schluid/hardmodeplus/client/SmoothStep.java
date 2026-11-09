package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.client.mixin.CameraAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

// Limping steps up a whole block at once, this lets the camera follow slowly
public final class SmoothStep {
	private static final double MIN_STEP = 0.6;
	private static final double MAX_STEP = 1.2;

	private static double lastY = Double.NaN;
	private static boolean wasOnGround;

	private SmoothStep() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null || player.isPassenger() || minecraft.gameRenderer.mainCamera().entity() != player) {
			lastY = Double.NaN;
			return;
		}
		double rise = player.getY() - lastY;
		if (wasOnGround && player.onGround() && rise > MIN_STEP && rise < MAX_STEP) {
			CameraAccessor camera = (CameraAccessor) minecraft.gameRenderer.mainCamera();
			camera.hardmodeplus$setEyeHeight(camera.hardmodeplus$eyeHeight() - (float) rise);
		}
		lastY = player.getY();
		wasOnGround = player.onGround();
	}
}
