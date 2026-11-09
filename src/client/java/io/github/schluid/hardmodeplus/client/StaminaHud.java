package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.feature.Features;
import io.github.schluid.hardmodeplus.feature.Stamina;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public final class StaminaHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "stamina_bar");
	private static final Identifier MAX_LEVEL_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "max_level");
	private static final int MAX_LEVEL_COLOR = 0xFFC0C0C0;
	private static final int WIDTH = 182;
	private static final int HEIGHT = 4;
	public static final int SHIFT = 7;
	private static final int LEVEL_SHIFT = 9;
	private static final int OUTLINE = 0xFF000000;
	private static final int EMPTY = 0xFF3A3A3A;
	private static final int FULL = 0xFFE8C547;
	private static final int EXHAUSTED = 0xFFD04A3A;

	private StaminaHud() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, ID, (graphics, tick) -> draw(graphics));
		for (Identifier bar : new Identifier[] {VanillaHudElements.HEALTH_BAR, VanillaHudElements.ARMOR_BAR,
				VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
				VanillaHudElements.HELD_ITEM_TOOLTIP, VanillaHudElements.OVERLAY_MESSAGE}) {
			HudElementRegistry.replaceElement(bar, element -> shifted(element, SHIFT));
		}
		HudElementRegistry.replaceElement(VanillaHudElements.EXPERIENCE_LEVEL, element -> shifted(element, LEVEL_SHIFT));
		HudElementRegistry.attachElementAfter(VanillaHudElements.EXPERIENCE_LEVEL, MAX_LEVEL_ID,
				(graphics, tick) -> drawMaxLevel(graphics));
	}

	private static HudElement shifted(HudElement element, int amount) {
		return (graphics, tick) -> {
			if (!visible(Minecraft.getInstance().player)) {
				element.extractRenderState(graphics, tick);
				return;
			}
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, -amount);
			element.extractRenderState(graphics, tick);
			graphics.pose().popMatrix();
		};
	}

	public static int shift() {
		return visible(Minecraft.getInstance().player) ? SHIFT : 0;
	}

	private static boolean exhausted(Player player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		return speed != null && speed.getModifier(Features.EXHAUST_ID) != null;
	}

	private static boolean visible(Player player) {
		return player != null && HardmodeConfig.isOn(FeatureSetting.EXHAUST) && HardmodeConfig.isOn(FeatureSetting.EXHAUST_BAR)
				&& !player.isCreative() && !player.isSpectator();
	}

	private static void drawMaxLevel(GuiGraphicsExtractor graphics) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !HardmodeConfig.isOn(FeatureSetting.EXHAUST) || !HardmodeConfig.isOn(FeatureSetting.EXHAUST_LEVELS)
				|| player.isCreative() || player.isSpectator()) {
			return;
		}
		int highest = Stamina.highestLevel(player);
		if (highest <= player.experienceLevel) {
			return;
		}
		Font font = Minecraft.getInstance().font;
		String text = String.valueOf(highest);
		int x = (graphics.guiWidth() - font.width(text)) / 2;
		int y = graphics.guiHeight() - 35 - (visible(player) ? LEVEL_SHIFT : 0) - 9;
		graphics.text(font, text, x + 1, y, 0xFF000000, false);
		graphics.text(font, text, x - 1, y, 0xFF000000, false);
		graphics.text(font, text, x, y + 1, 0xFF000000, false);
		graphics.text(font, text, x, y - 1, 0xFF000000, false);
		graphics.text(font, text, x, y, MAX_LEVEL_COLOR, false);
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!visible(player)) {
			return;
		}
		int left = graphics.guiWidth() / 2 - WIDTH / 2;
		int right = left + WIDTH;
		int bottom = graphics.guiHeight() - 30;
		int top = bottom - HEIGHT - 2;
		int filled = Math.round((WIDTH - 2) * Math.max(0, Math.min(100, Stamina.get(player))) / 100.0F);
		graphics.fill(left, top, right, bottom, OUTLINE);
		graphics.fill(left + 1, top + 1, right - 1, bottom - 1, EMPTY);
		if (filled > 0) {
			graphics.fill(left + 1, top + 1, left + 1 + filled, bottom - 1, exhausted(player) ? EXHAUSTED : FULL);
		}
	}
}
