package io.github.schluid.hardmodeplus.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

// Item tooltips don't wrap by themselves
final class Tooltips {
	private static final int WIDTH = 38;

	private Tooltips() {
	}

	static void add(Consumer<Component> tooltip, String key) {
		String text = Language.getInstance().getOrDefault(key);
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (!line.isEmpty() && line.length() + 1 + word.length() > WIDTH) {
				tooltip.accept(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
				line.setLength(0);
			}
			if (!line.isEmpty()) {
				line.append(' ');
			}
			line.append(word);
		}
		if (!line.isEmpty()) {
			tooltip.accept(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
		}
	}
}
