package io.github.schluid.hardmodeplus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class ClientOptions {
	private static final String FILE_NAME = HardmodePlus.MOD_ID + "-client.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static boolean sprintFix;
	private static boolean showSaturation = true;
	private static boolean centerEffects = true;

	private ClientOptions() {
	}

	public static boolean sprintFix() {
		return sprintFix && HardmodeConfig.active();
	}

	public static boolean sprintFixSetting() {
		return sprintFix;
	}

	public static void setSprintFix(boolean value) {
		sprintFix = value;
	}

	public static boolean showSaturation() {
		return showSaturation && HardmodeConfig.active();
	}

	public static boolean showSaturationSetting() {
		return showSaturation;
	}

	public static void setShowSaturation(boolean value) {
		showSaturation = value;
	}

	public static boolean centerEffects() {
		return centerEffects && HardmodeConfig.active();
	}

	public static boolean centerEffectsSetting() {
		return centerEffects;
	}

	public static void setCenterEffects(boolean value) {
		centerEffects = value;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			sprintFix = root.has("sprint_fix") && root.get("sprint_fix").getAsBoolean();
			showSaturation = !root.has("show_saturation") || root.get("show_saturation").getAsBoolean();
			centerEffects = !root.has("center_effects") || root.get("center_effects").getAsBoolean();
		} catch (IOException | RuntimeException e) {
			HardmodePlus.LOGGER.error("Could not read config/{}, using defaults", FILE_NAME, e);
		}
	}

	public static void save() {
		JsonObject root = new JsonObject();
		root.addProperty("sprint_fix", sprintFix);
		root.addProperty("show_saturation", showSaturation);
		root.addProperty("center_effects", centerEffects);
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
		} catch (IOException e) {
			HardmodePlus.LOGGER.error("Could not write {}", path, e);
		}
	}
}
