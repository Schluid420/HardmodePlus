package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.feature.Thirst;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

public final class SaturationHud {
	private static final Identifier FOOD_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "food_saturation");
	private static final int ICON = 9;
	private static final int GOLD = 0xFFF2C230;
	private static final int[][] FOOD_OUTLINE = {{1, -1}, {2, -1}, {3, -1}, {4, -1}, {0, 0}, {1, 0}, {4, 0}, {5, 0},
			{-1, 1}, {0, 1}, {5, 1}, {6, 1}, {-1, 2}, {6, 2}, {7, 2}, {-1, 3}, {7, 3}, {-1, 4}, {0, 4}, {7, 4}, {0, 5},
			{1, 5}, {7, 5}, {8, 5}, {9, 5}, {1, 6}, {2, 6}, {9, 6}, {2, 7}, {3, 7}, {4, 7}, {5, 7}, {9, 7}, {5, 8},
			{8, 8}, {9, 8}, {5, 9}, {6, 9}, {7, 9}, {8, 9}};
	private static final int[][] THIRST_OUTLINE = {{3, -1}, {4, -1}, {5, -1}, {2, 0}, {3, 0}, {5, 0}, {6, 0}, {1, 1},
			{2, 1}, {6, 1}, {7, 1}, {1, 2}, {7, 2}, {0, 3}, {1, 3}, {7, 3}, {8, 3}, {0, 4}, {8, 4}, {0, 5}, {8, 5},
			{0, 6}, {8, 6}, {0, 7}, {1, 7}, {7, 7}, {8, 7}, {1, 8}, {2, 8}, {6, 8}, {7, 8}, {2, 9}, {3, 9}, {4, 9},
			{5, 9}, {6, 9}};

	private SaturationHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, FOOD_ID, (graphics, tick) -> drawFood(graphics));
	}

	private static boolean shown(LocalPlayer player) {
		return player != null && ClientOptions.showSaturation() && !player.isCreative() && !player.isSpectator();
	}

	private static void drawFood(GuiGraphicsExtractor graphics) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!shown(player) || player.getVehicle() instanceof LivingEntity) {
			return;
		}
		int top = graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(VanillaHudElements.FOOD_BAR) - StaminaHud.shift();
		frames(graphics, top, Thirst.shownFoodSaturation(player), FOOD_OUTLINE);
	}

	public static void drawThirst(GuiGraphicsExtractor graphics, int top) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (shown(player)) {
			frames(graphics, top, Thirst.shownSaturation(player), THIRST_OUTLINE);
		}
	}

	private static void frames(GuiGraphicsExtractor graphics, int top, double saturation, int[][] outline) {
		int right = graphics.guiWidth() / 2 + 91;
		for (int i = 0; i < 10 && saturation > i * 2; i++) {
			int x = right - i * 8 - ICON;
			for (int[] pixel : outline) {
				graphics.fill(x + pixel[0], top + pixel[1], x + pixel[0] + 1, top + pixel[1] + 1, GOLD);
			}
		}
	}
}
