package io.github.schluid.hardmodeplus.client;

import com.mojang.blaze3d.Blaze3D;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ImportScreen extends TipScreen {
	private static final int TOP = 44;
	private static final int ROW = 24;
	private static final int WIDTH = 300;
	private static final int GREY = 0xFFA0A0A0;
	private static final int RED = 0xFFFF5555;

	private final Screen parent;
	private final Screen afterImport;
	private List<String> files = List.of();
	private int scroll;
	private Component error;

	public ImportScreen(Screen parent, Screen afterImport) {
		super(Component.translatable("hardmodeplus.import.title"));
		this.parent = parent;
		this.afterImport = afterImport;
	}

	private int visibleRows() {
		return Math.max(1, (this.height - TOP - 50) / ROW);
	}

	@Override
	protected void init() {
		clearTips();
		files = HardmodeConfig.listExports();
		scroll = Math.max(0, Math.min(scroll, files.size() - visibleRows()));
		int x = this.width / 2 - WIDTH / 2;
		int deleteWidth = 50;
		for (int i = 0; i < visibleRows() && scroll + i < files.size(); i++) {
			String file = files.get(scroll + i);
			int y = TOP + i * ROW;
			this.addRenderableWidget(tip(Button.builder(Component.literal(file), b -> confirm(file))
					.bounds(x, y, WIDTH - deleteWidth - 4, 20).build(),
					Component.translatable("hardmodeplus.import.file.tip")));
			this.addRenderableWidget(Button.builder(Component.translatable("hardmodeplus.import.delete"),
					b -> confirmDelete(file)).bounds(x + WIDTH - deleteWidth, y, deleteWidth, 20).build());
		}

		int bottomWidth = 150;
		int bottomLeft = this.width / 2 - (2 * bottomWidth + 4) / 2;
		this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.import.folder"), b -> {
			try {
				Files.createDirectories(HardmodeConfig.exportFolder());
			} catch (IOException ignored) {
			}
			Blaze3D.openPath(HardmodeConfig.exportFolder());
		}).bounds(bottomLeft, this.height - 26, bottomWidth, 20).build(),
				Component.literal(HardmodeConfig.exportFolder().toString())));
		this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), b -> this.onClose())
				.bounds(bottomLeft + bottomWidth + 4, this.height - 26, bottomWidth, 20).build());
	}

	private void confirm(String file) {
		BooleanConsumer answer = yes -> {
			if (yes && HardmodeConfig.importFile(file)) {
				HardmodeConfig.save();
				this.minecraft.setScreenAndShow(afterImport);
				return;
			}
			error = yes ? Component.translatable("hardmodeplus.import.failed") : null;
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("hardmodeplus.load.confirm.title", file),
				Component.translatable("hardmodeplus.load.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	private void confirmDelete(String file) {
		BooleanConsumer answer = yes -> {
			if (yes) {
				error = HardmodeConfig.deleteExport(file) ? null : Component.translatable("hardmodeplus.import.delete.failed");
			}
			this.minecraft.setScreenAndShow(this);
		};
		this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
				Component.translatable("hardmodeplus.import.delete.confirm.title", file),
				Component.translatable("hardmodeplus.import.delete.confirm.message"),
				Component.translatable("gui.yes"),
				Component.translatable("gui.no")));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int max = Math.max(0, files.size() - visibleRows());
		int next = Math.max(0, Math.min(max, scroll + (scrollY > 0 ? -1 : 1)));
		if (scrollY != 0 && next != scroll) {
			scroll = next;
			this.rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);
		graphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.literal("config/hardmodeplus/profiles/"), this.width / 2, 26, GREY);
		if (files.isEmpty()) {
			graphics.centeredText(this.font, Component.translatable("hardmodeplus.import.empty"), this.width / 2, TOP + 6, GREY);
		}
		if (error != null) {
			graphics.centeredText(this.font, error, this.width / 2, this.height - 40, RED);
		}
		drawTips(graphics, mouseX, mouseY);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
