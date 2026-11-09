package io.github.schluid.hardmodeplus.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

public abstract class TipScreen extends Screen {
	public static final int TIP_WIDTH = 200;
	private static final int MARGIN = 6;

	public static final ClientTooltipPositioner INSIDE = (screenWidth, screenHeight, mouseX, mouseY, width, height) -> {
		int x = mouseX + 12;
		int y = mouseY - 12;
		if (x + width + MARGIN > screenWidth) {
			x = mouseX - 12 - width;
		}
		x = Math.max(MARGIN, Math.min(x, screenWidth - width - MARGIN));
		y = Math.max(MARGIN, Math.min(y, screenHeight - height - MARGIN));
		return new Vector2i(x, y);
	};

	private final Map<AbstractWidget, List<Component>> tips = new LinkedHashMap<>();

	protected TipScreen(Component title) {
		super(title);
	}

	protected void clearTips() {
		tips.clear();
	}

	protected <T extends AbstractWidget> T tip(T widget, Component... lines) {
		tips.put(widget, List.of(lines));
		return widget;
	}

	protected void drawTips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		for (Map.Entry<AbstractWidget, List<Component>> entry : tips.entrySet()) {
			AbstractWidget widget = entry.getKey();
			if (widget.visible && widget.isHovered()) {
				showTip(graphics, entry.getValue(), mouseX, mouseY);
				return;
			}
		}
	}

	private static final float MIN_SCALE = 0.7F;

	protected boolean drawFitted(GuiGraphicsExtractor graphics, Component text, int x, int y, int maxWidth, int color) {
		int width = this.font.width(text);
		if (width <= maxWidth) {
			graphics.text(this.font, text, x, y, color);
			return false;
		}
		float scale = Math.max(MIN_SCALE, (float) maxWidth / width);
		Component shown = text;
		boolean cut = false;
		if (width * scale > maxWidth) {
			String plain = this.font.plainSubstrByWidth(text.getString(), (int) (maxWidth / scale) - this.font.width("…"));
			shown = Component.literal(plain + "…").withStyle(text.getStyle());
			cut = true;
		}
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y + 8 * (1 - scale) / 2);
		graphics.pose().scale(scale, scale);
		graphics.text(this.font, shown, 0, 0, color);
		graphics.pose().popMatrix();
		return cut;
	}

	protected void drawFittedCentered(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int maxWidth,
			int color) {
		int width = this.font.width(text);
		float scale = width <= maxWidth ? 1.0F : Math.max(MIN_SCALE, (float) maxWidth / width);
		int shownWidth = (int) Math.min(maxWidth, Math.ceil(width * scale));
		drawFitted(graphics, text, centerX - shownWidth / 2, y, maxWidth, color);
	}

	protected void showTip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
		List<FormattedCharSequence> wrapped = new ArrayList<>();
		for (Component line : lines) {
			wrapped.addAll(this.font.split(line, TIP_WIDTH));
		}
		if (!wrapped.isEmpty()) {
			graphics.setTooltipForNextFrame(this.font, wrapped, INSIDE, mouseX, mouseY, false);
		}
	}
}
