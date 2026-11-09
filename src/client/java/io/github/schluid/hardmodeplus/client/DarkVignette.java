package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class DarkVignette {
	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID,
			"textures/gui/dark_vignette.png");
	private static final int TEXTURE_SIZE = 256;

	private DarkVignette() {
	}

	public static void register() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
				Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "dark_vignette"),
				(graphics, tick) -> draw(graphics));
	}

	private record Edge(float alpha, float size) {
		float power() {
			return size <= 0 ? 0 : alpha / size;
		}
	}

	private static final Edge NONE = new Edge(0, 0);

	private static boolean playing(LocalPlayer player) {
		return player != null && !player.isCreative() && !player.isSpectator() && player.isAlive();
	}

	private static Edge hungerEdge(LocalPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.FOOD) || !HardmodeConfig.isOn(FeatureSetting.STARVE_DARK)
				|| player.getFoodData().getFoodLevel() > 0) {
			return NONE;
		}
		float strength = (float) (HardmodeConfig.get(FeatureSetting.DARK_STRENGTH) / 100.0);
		return new Edge(strength, 1.55F);
	}

	private static final double STANDARD_HEARTS = 10;

	private static Edge healthEdge(LocalPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.LOW_HEALTH_DARK)) {
			return NONE;
		}
		boolean standard = (int) HardmodeConfig.get(FeatureSetting.LOW_HEALTH_BASE) == 1;
		double hearts = standard ? STANDARD_HEARTS : player.getMaxHealth() / 2.0;
		double missing = Math.floor((hearts - player.getHealth() / 2.0) * 2) / 2;
		double from = HardmodeConfig.get(FeatureSetting.LOW_HEALTH_FROM);
		if (missing < from) {
			return NONE;
		}
		double range = hearts - 1 - from + 0.5;
		double progress = range <= 0 ? 1 : Math.min(1, (missing - from + 0.5) / range);
		int curve = (int) HardmodeConfig.get(FeatureSetting.LOW_HEALTH_CURVE);
		if (curve > 0) {
			progress = Math.pow(progress, curve == 1 ? 1.5 : 2.2);
		}
		float strength = (float) (HardmodeConfig.get(FeatureSetting.LOW_HEALTH_MAX) / 100.0 * progress);
		return new Edge(Math.min(1.0F, 0.75F + strength * 0.35F), 1.35F - 1.0F * strength);
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!playing(player)) {
			return;
		}
		Edge hunger = hungerEdge(player);
		Edge health = healthEdge(player);
		Edge edge = health.power() >= hunger.power() ? health : hunger;
		if (edge.power() <= 0) {
			return;
		}
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int w = Math.round(width * edge.size());
		int h = Math.round(height * edge.size());
		int x = (width - w) / 2;
		int y = (height - h) / 2;
		int alpha = Math.round(255 * Math.min(1.0F, edge.alpha()));
		int tint = alpha << 24 | 0xFFFFFF;
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, w, h, TEXTURE_SIZE, TEXTURE_SIZE,
				TEXTURE_SIZE, TEXTURE_SIZE, tint);
		if (x > 0 || y > 0) {
			int black = alpha << 24;
			graphics.fill(0, 0, width, Math.max(0, y), black);
			graphics.fill(0, y + h, width, height, black);
			graphics.fill(0, Math.max(0, y), Math.max(0, x), y + h, black);
			graphics.fill(x + w, Math.max(0, y), width, y + h, black);
		}
	}
}
