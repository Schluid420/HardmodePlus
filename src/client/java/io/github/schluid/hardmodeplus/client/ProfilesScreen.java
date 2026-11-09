package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.Presets;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ProfilesScreen extends TipScreen {
	private static final int MAX_ROW_HEIGHT = 24;
	private static final int MIN_ROW_HEIGHT = 21;
	private static final int GAP = 4;
	private static final int PRESET_HEADER = 42;
	private static final int PRESET_TOP = 54;
	private static final int GREY = 0xFFA0A0A0;
	private static final int WHITE = 0xFFFFFFFF;
	private static final SystemToast.SystemToastId EXPORT_TOAST = new SystemToast.SystemToastId(5000L);

	private final Screen parent;

	private int rowWidth;
	private int nameWidth;
	private int infoWidth;
	private int buttonWidth;
	private int rowHeight = MAX_ROW_HEIGHT;

	public ProfilesScreen(Screen parent) {
		super(Component.translatable("hardmodeplus.profiles.title"));
		this.parent = parent;
	}

	private int left() {
		return this.width / 2 - rowWidth / 2;
	}

	private int profileHeader() {
		return PRESET_TOP + Presets.values().length * rowHeight + 4;
	}

	private int profileTop() {
		return profileHeader() + 12;
	}

	private void layout() {
		rowWidth = Math.min(450, this.width - 20);
		boolean narrow = rowWidth < 450;
		int infoText = this.font.width(Component.translatable("hardmodeplus.profiles.count", 999)) + 8;
		infoWidth = narrow ? infoText : Math.max(90, infoText);
		buttonWidth = Math.clamp((rowWidth - 160) / 3 - GAP, 54, 70);
		nameWidth = rowWidth - infoWidth - 3 * (buttonWidth + GAP);
		int rows = Presets.values().length + HardmodeConfig.PROFILE_COUNT;
		int free = this.height - 30 - PRESET_TOP - 16;
		rowHeight = Math.clamp(free / rows, MIN_ROW_HEIGHT, MAX_ROW_HEIGHT);
	}

	@Override
	protected void init() {
		clearTips();
		layout();
		int left = left();
		int right = left + rowWidth;

		Presets[] presets = Presets.values();
		for (int i = 0; i < presets.length; i++) {
			Presets preset = presets[i];
			int y = PRESET_TOP + i * rowHeight;
			String key = "hardmodeplus.preset." + preset.key;
			Component name = Component.translatable(key);
			this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.profiles.load"),
					b -> confirm(name, preset::apply))
					.bounds(right - buttonWidth, y, buttonWidth, 20).build(),
					name, Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY)));
		}

		// Own profiles
		for (int i = 0; i < HardmodeConfig.PROFILE_COUNT; i++) {
			int slot = i;
			int y = profileTop() + i * rowHeight;
			HardmodeConfig.Profile profile = HardmodeConfig.profile(slot);

			EditBox name = new EditBox(this.font, left, y, nameWidth - 6, 20,
					Component.translatable("hardmodeplus.profiles.name"));
			name.setMaxLength(24);
			name.setValue(profile.name);
			name.setHint(Component.translatable("hardmodeplus.profiles.default", slot + 1));
			name.setResponder(text -> HardmodeConfig.profile(slot).name = text);
			this.addRenderableWidget(name);

			int bx = left + nameWidth + infoWidth;
			Component profileName = profile.name.isBlank()
					? Component.translatable("hardmodeplus.profiles.default", slot + 1)
					: Component.literal(profile.name);
			Runnable saveAction = () -> {
				HardmodeConfig.saveToProfile(slot);
				HardmodeConfig.save();
			};
			this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.profiles.save"), b -> {
				if (profile.isEmpty()) {
					saveAction.run();
					this.rebuildWidgets();
				} else {
					ask(Component.translatable("hardmodeplus.profiles.save.confirm.title", profileName),
							Component.translatable("hardmodeplus.profiles.save.confirm.message"), saveAction);
				}
			}).bounds(bx, y, buttonWidth, 20).build(),
					Component.translatable("hardmodeplus.profiles.save.tip")));

			Button load = Button.builder(Component.translatable("hardmodeplus.profiles.load"),
					b -> confirm(profileName, () -> HardmodeConfig.loadProfile(slot)))
					.bounds(bx + buttonWidth + GAP, y, buttonWidth, 20).build();
			load.active = !profile.isEmpty();
			this.addRenderableWidget(tip(load, Component.translatable("hardmodeplus.profiles.load.tip")));

			Button clear = Button.builder(Component.translatable("hardmodeplus.profiles.clear"),
					b -> ask(Component.translatable("hardmodeplus.profiles.clear.confirm.title", profileName),
							Component.translatable("hardmodeplus.profiles.clear.confirm.message"), () -> {
								HardmodeConfig.clearProfile(slot);
								HardmodeConfig.save();
							})).bounds(bx + 2 * (buttonWidth + GAP), y, buttonWidth, 20).build();
			clear.active = !profile.isEmpty() || !profile.name.isBlank();
			this.addRenderableWidget(tip(clear, Component.translatable("hardmodeplus.profiles.clear.tip")));
		}

		// Export, import and back in one row at the bottom
		int bottomWidth = 100;
		int bottomLeft = this.width / 2 - (3 * bottomWidth + 2 * GAP) / 2;
		int by = this.height - 26;
		this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.export"), b -> export())
				.bounds(bottomLeft, by, bottomWidth, 20).build(),
				Component.translatable("hardmodeplus.export"),
				Component.translatable("hardmodeplus.export.tip").withStyle(ChatFormatting.GRAY)));
		this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.import"),
				b -> this.minecraft.setScreenAndShow(new ImportScreen(this, this.parent)))
				.bounds(bottomLeft + bottomWidth + GAP, by, bottomWidth, 20).build(),
				Component.translatable("hardmodeplus.import"),
				Component.translatable("hardmodeplus.import.tip").withStyle(ChatFormatting.GRAY)));
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
				.bounds(bottomLeft + 2 * (bottomWidth + GAP), by, bottomWidth, 20).build());
	}

	private void export() {
		HardmodeConfig.save();
		String file = HardmodeConfig.exportCurrent();
		Component title = Component.translatable(file != null ? "hardmodeplus.export.done" : "hardmodeplus.export.failed");
		Component message = Component.literal(file != null ? "config/hardmodeplus/profiles/" + file : "");
		SystemToast.add(this.minecraft.gui.toastManager(), EXPORT_TOAST, title, message);
	}

	private void ask(Component title, Component message, Runnable action) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				action.run();
			}
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer, title, message,
				Component.translatable("gui.yes"), Component.translatable("gui.no")));
	}

	private void confirm(Component name, Runnable load) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				load.run();
				HardmodeConfig.save();
			}
			this.minecraft.setScreenAndShow(yes ? this.parent : this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("hardmodeplus.load.confirm.title", name),
				Component.translatable("hardmodeplus.load.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, WHITE);
		graphics.centeredText(this.font, Component.translatable("hardmodeplus.profiles.current",
				HardmodeConfig.changeCount()), this.width / 2, 24, GREY);

		int left = left();
		int descriptionLeft = left + 90;
		int descriptionWidth = rowWidth - 90 - buttonWidth - 8;

		graphics.text(this.font, Component.translatable("hardmodeplus.preset.header").withStyle(ChatFormatting.UNDERLINE),
				left, PRESET_HEADER, GREY);
		Presets[] presets = Presets.values();
		List<Component> hovered = null;
		for (int i = 0; i < presets.length; i++) {
			int rowTop = PRESET_TOP + i * rowHeight;
			int y = rowTop + (rowHeight - 8) / 2;
			String key = "hardmodeplus.preset." + presets[i].key;
			graphics.text(this.font, Component.translatable(key), left, y, WHITE);
			drawFitted(graphics, Component.translatable(key + ".short"), descriptionLeft, y, descriptionWidth, GREY);
			if (mouseX >= left && mouseX < descriptionLeft + descriptionWidth && mouseY >= rowTop && mouseY < rowTop + 20) {
				hovered = List.of(Component.translatable(key),
						Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
			}
		}

		graphics.text(this.font, Component.translatable("hardmodeplus.profiles.header").withStyle(ChatFormatting.UNDERLINE),
				left, profileHeader(), GREY);
		for (int i = 0; i < HardmodeConfig.PROFILE_COUNT; i++) {
			HardmodeConfig.Profile profile = HardmodeConfig.profile(i);
			Component info = profile.isEmpty()
					? Component.translatable("hardmodeplus.profiles.empty")
					: Component.translatable("hardmodeplus.profiles.count", profile.changeCount());
			graphics.text(this.font, info, left + nameWidth, profileTop() + i * rowHeight + (rowHeight - 8) / 2,
					profile.isEmpty() ? 0xFF808080 : 0xFFE0E0E0);
		}
		if (hovered != null) {
			showTip(graphics, hovered, mouseX, mouseY);
		} else {
			drawTips(graphics, mouseX, mouseY);
		}
	}

	@Override
	public void onClose() {
		HardmodeConfig.save();
		this.minecraft.setScreenAndShow(this.parent);
	}
}
