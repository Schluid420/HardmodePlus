package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.feature.Thirst;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import java.util.Random;

public final class ThirstHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "thirst_bar");
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID,
			"textures/gui/thirst.png");
	private static final int ICON = 9;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int WOBBLE_FROM = 6;
	private static final Random RANDOM = new Random();

	private ThirstHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ID, (graphics, tick) -> draw(graphics));
		HudStatusBarHeightRegistry.addRight(ID, player -> visible(player) ? 10 : 0);
	}

	private static boolean visible(Player player) {
		return player != null && HardmodeConfig.isOn(FeatureSetting.THIRST) && !player.isCreative()
				&& !player.isSpectator();
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!visible(player)) {
			return;
		}
		int thirst = Thirst.get(player);
		int right = graphics.guiWidth() / 2 + 91;
		// Moves up with the other bars while the stamina bar is shown
		int top = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ID) - StaminaHud.shift();
		boolean wobble = thirst <= WOBBLE_FROM && player.tickCount % (thirst * 3 + 1) == 0;
		for (int i = 0; i < 10; i++) {
			int x = right - i * 8 - ICON;
			int y = top + (wobble ? RANDOM.nextInt(3) - 1 : 0);
			graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, ICON, ICON, ICON, ICON, 27, ICON, WHITE);
			int value = thirst - i * 2;
			if (value >= 2) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 18, 0, ICON, ICON, ICON, ICON, 27, ICON, WHITE);
			} else if (value == 1) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 9, 0, ICON, ICON, ICON, ICON, 27, ICON, WHITE);
			}
		}
		SaturationHud.drawThirst(graphics, top);
	}
}
