package io.github.schluid.hardmodeplus.client;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;

public final class EffectsHud {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier BACKGROUND_AMBIENT = Identifier.withDefaultNamespace("hud/effect_background_ambient");
	private static final float SCALE = 1.08F;
	private static final int SIZE = 24;
	private static final int GAP = 1;
	private static final int TOP = 2;

	private EffectsHud() {
	}

	public static void register() {
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, vanilla -> (graphics, tick) -> {
			if (ClientOptions.centerEffects()) {
				draw(graphics);
			} else {
				vanilla.extractRenderState(graphics, tick);
			}
		});
	}

	private static void draw(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.gui.screen() != null && minecraft.gui.screen().showsActiveEffects()) {
			return;
		}
		List<MobEffectInstance> good = new ArrayList<>();
		List<MobEffectInstance> bad = new ArrayList<>();
		for (MobEffectInstance effect : player.getActiveEffects()) {
			if (effect.showIcon()) {
				(effect.getEffect().value().isBeneficial() ? good : bad).add(effect);
			}
		}
		good.sort(null);
		bad.sort(null);
		graphics.pose().pushMatrix();
		graphics.pose().translate(graphics.guiWidth() / 2.0F, TOP);
		graphics.pose().scale(SCALE, SCALE);
		row(graphics, good, 0);
		row(graphics, bad, good.isEmpty() ? 0 : SIZE + GAP);
		graphics.pose().popMatrix();
	}

	private static void row(GuiGraphicsExtractor graphics, List<MobEffectInstance> effects, int y) {
		int width = effects.size() * (SIZE + GAP) - GAP;
		int x = -width / 2;
		for (MobEffectInstance effect : effects) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, effect.isAmbient() ? BACKGROUND_AMBIENT : BACKGROUND, x, y, SIZE, SIZE);
			float alpha = 1.0F;
			if (!effect.isInfiniteDuration() && effect.endsWithin(200)) {
				// Blinks faster the closer the end is, like vanilla
				alpha = (effect.getDuration() / 5) % 2 == 0 ? 1.0F : 0.4F;
			}
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), x + 3, y + 3, 18, 18, alpha);
			x += SIZE + GAP;
		}
	}
}
