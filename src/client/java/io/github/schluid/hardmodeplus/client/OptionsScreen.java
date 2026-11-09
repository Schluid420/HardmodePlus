package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class OptionsScreen extends TipScreen {
	private static final int WIDTH = 300;
	private static final int ROW = 26;

	private enum Page {
		MAIN, GENERAL, DISPLAY, LOOT;

		String key() {
			return "hardmodeplus.options.page." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	private final Screen parent;
	private final Page page;

	public OptionsScreen(Screen parent) {
		this(parent, Page.MAIN);
	}

	private OptionsScreen(Screen parent, Page page) {
		super(page == Page.MAIN ? Component.translatable("hardmodeplus.options.title")
				: Component.translatable("hardmodeplus.options.title").append(" – ").append(Component.translatable(page.key())));
		this.parent = parent;
		this.page = page;
	}

	private static Component onOff(String key, boolean value) {
		return Component.translatable(key).append(": ")
				.append(value ? CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN)
						: CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED));
	}

	private void toggle(int row, String key, boolean value, Runnable flip) {
		int x = this.width / 2 - WIDTH / 2;
		this.addRenderableWidget(tip(Button.builder(onOff(key, value), b -> {
			flip.run();
			this.rebuildWidgets();
		}).bounds(x, 50 + row * ROW, WIDTH, 20)
				.build(),
				Component.translatable(key),
				Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY)));
	}

	@Override
	protected void init() {
		clearTips();
		int x = this.width / 2 - WIDTH / 2;
		switch (page) {
			case MAIN -> {
				Page[] pages = {Page.GENERAL, Page.DISPLAY, Page.LOOT};
				for (int i = 0; i < pages.length; i++) {
					Page target = pages[i];
					this.addRenderableWidget(tip(Button.builder(Component.translatable(target.key()), b -> {
						HardmodeConfig.save();
						ClientOptions.save();
						this.minecraft.setScreenAndShow(new OptionsScreen(this, target));
					}).bounds(x, 50 + i * ROW, WIDTH, 20)
							.build(),
							Component.translatable(target.key()),
							Component.translatable(target.key() + ".tip").withStyle(ChatFormatting.GRAY)));
				}
			}
			case GENERAL -> {
				toggle(0, "hardmodeplus.options.enabled", HardmodeConfig.enabled(),
						() -> HardmodeConfig.setEnabled(!HardmodeConfig.enabled()));
				HardmodeConfig.Mode mode = HardmodeConfig.mode();
				this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.options.mode")
						.append(": ").append(Component.translatable("hardmodeplus.options.mode." + mode.key)
								.withStyle(ChatFormatting.YELLOW)), b -> {
					HardmodeConfig.setMode(HardmodeConfig.mode().next());
					this.rebuildWidgets();
				}).bounds(x, 50 + ROW, WIDTH, 20)
						.build(),
						Component.translatable("hardmodeplus.options.mode"),
						Component.translatable("hardmodeplus.options.mode.tip").withStyle(ChatFormatting.GRAY)));
				toggle(2, "hardmodeplus.options.bosses", HardmodeConfig.bossesInHardMode(),
						() -> HardmodeConfig.setBossesInHardMode(!HardmodeConfig.bossesInHardMode()));
			}
			case DISPLAY -> {
				toggle(0, "hardmodeplus.options.sprint_fix", ClientOptions.sprintFixSetting(),
						() -> ClientOptions.setSprintFix(!ClientOptions.sprintFixSetting()));
				toggle(1, "hardmodeplus.options.show_saturation", ClientOptions.showSaturationSetting(),
						() -> ClientOptions.setShowSaturation(!ClientOptions.showSaturationSetting()));
				toggle(2, "hardmodeplus.options.center_effects", ClientOptions.centerEffectsSetting(),
						() -> ClientOptions.setCenterEffects(!ClientOptions.centerEffectsSetting()));
			}
			case LOOT -> {
				FeatureSetting[] killLoot = {FeatureSetting.KILL_LOOT_MONSTERS, FeatureSetting.KILL_LOOT_GOLEMS,
						FeatureSetting.KILL_LOOT_ANIMALS};
				for (int i = 0; i < killLoot.length; i++) {
					FeatureSetting setting = killLoot[i];
					toggle(i, "hardmodeplus.feature." + setting.key, HardmodeConfig.own(setting) >= 0.5,
							() -> HardmodeConfig.set(setting, HardmodeConfig.own(setting) >= 0.5 ? 0 : 1));
				}
			}
		}

		this.addRenderableWidget(Button.builder(Component.translatable(page == Page.MAIN ? "gui.done" : "gui.back"),
				b -> this.onClose())
				.bounds(this.width / 2 - 75, this.height - 28, 150, 20).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
		drawTips(graphics, mouseX, mouseY);
	}

	@Override
	public void onClose() {
		HardmodeConfig.save();
		ClientOptions.save();
		this.minecraft.setScreenAndShow(this.parent);
	}
}
