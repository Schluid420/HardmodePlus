package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public final class WelcomeToast {
	private static final SystemToast.SystemToastId ID = new SystemToast.SystemToastId(10000L);

	private WelcomeToast() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			if (HardmodeConfig.welcomeShown || minecraft.gui.overlay() != null
					|| !(minecraft.gui.screen() instanceof TitleScreen)) {
				return;
			}
			SystemToast.add(minecraft.gui.toastManager(), ID,
					Component.translatable("hardmodeplus.welcome.title"),
					Component.translatable("hardmodeplus.welcome.message"));
			HardmodeConfig.welcomeShown = true;
			HardmodeConfig.save();
		});
	}
}
