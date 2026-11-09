package io.github.schluid.hardmodeplus.config;

import io.github.schluid.hardmodeplus.HardmodePlus;
import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

// Sends the server's feature values to each player (format "1;THIRST=1.0;...")
public final class ServerSettings {
	public static final AttachmentType<String> SETTINGS = AttachmentRegistry.<String>builder()
			.syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "server_settings"));

	private static String encodedFor = "";
	private static String encoded = "";
	private static String lastRead;

	private ServerSettings() {
	}

	public static void register() {
	}

	public static void update(ServerPlayer player) {
		String text = current();
		if (!text.equals(((AttachmentTarget) player).getAttached(SETTINGS))) {
			((AttachmentTarget) player).setAttached(SETTINGS, text);
		}
	}

	private static String current() {
		String signature = HardmodeConfig.signature();
		if (!signature.equals(encodedFor)) {
			StringBuilder text = new StringBuilder(HardmodeConfig.enabled() ? "1" : "0");
			for (FeatureSetting setting : FeatureSetting.values()) {
				if (HardmodeConfig.isChanged(setting)) {
					text.append(';').append(setting.name()).append('=').append(HardmodeConfig.own(setting));
				}
			}
			encoded = text.toString();
			encodedFor = signature;
		}
		return encoded;
	}

	public static void read(String text) {
		if (text == null) {
			lastRead = null;
			HardmodeConfig.useOwnValues();
			return;
		}
		if (text.equals(lastRead)) {
			return;
		}
		lastRead = text;
		String[] parts = text.split(";");
		Map<FeatureSetting, Double> features = new EnumMap<>(FeatureSetting.class);
		for (int i = 1; i < parts.length; i++) {
			int split = parts[i].indexOf('=');
			if (split <= 0) {
				continue;
			}
			try {
				features.put(FeatureSetting.valueOf(parts[i].substring(0, split)),
						Double.parseDouble(parts[i].substring(split + 1)));
			} catch (IllegalArgumentException ignored) {
				// Unknown setting from another mod version
			}
		}
		HardmodeConfig.useServerValues(parts[0].equals("1"), features);
	}
}
