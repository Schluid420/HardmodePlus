package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.config.ServerSettings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class HardmodePlusClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientOptions.load();
		WelcomeToast.register();
		DarkVignette.register();
		ThirstHud.register();
		StaminaHud.register();
		SaturationHud.register();
		EffectsHud.register();
		VariantEyes.register();
		ClientTickEvents.END_CLIENT_TICK.register(SmoothStep::tick);

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (minecraft.player == null || minecraft.isLocalServer()) {
				ServerSettings.read(null);
			} else {
				ServerSettings.read(((AttachmentTarget) minecraft.player).getAttached(ServerSettings.SETTINGS));
			}
		});
	}
}
