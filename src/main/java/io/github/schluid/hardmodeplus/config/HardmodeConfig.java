package io.github.schluid.hardmodeplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.mobs.MobList;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class HardmodeConfig {
	public static final int PROFILE_COUNT = 3;
	private static final String FILE_NAME = HardmodePlus.MOD_ID + ".json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final double EPSILON = 1.0E-6;

	public enum Mode {
		BOTH("both"), PER_MOB("per_mob"), PERCENT("percent");

		public final String key;

		Mode(String key) {
			this.key = key;
		}

		public Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}

		static Mode byKey(String key) {
			for (Mode mode : values()) {
				if (mode.key.equals(key)) {
					return mode;
				}
			}
			return BOTH;
		}
	}

	public static final class Profile {
		public String name = "";
		public Map<String, Map<MobStat, Double>> mobs = new TreeMap<>();
		public Map<HardArea, Double> hard = new EnumMap<>(HardArea.class);
		public Map<FeatureSetting, Double> features = new EnumMap<>(FeatureSetting.class);
		public Mode mode = Mode.BOTH;
		public boolean bosses = true;
		public boolean saved;

		public boolean isEmpty() {
			return !saved;
		}

		public int changeCount() {
			int count = hard.size() + features.size();
			for (Map<MobStat, Double> values : mobs.values()) {
				count += values.size();
			}
			return count;
		}
	}

	private static volatile boolean enabled = true;
	private static volatile boolean bossesInHardMode = true;
	private static volatile Mode mode = Mode.BOTH;
	public static volatile boolean welcomeShown;
	private static final Map<String, Map<MobStat, Double>> MOBS = new ConcurrentHashMap<>();
	private static final Map<HardArea, Double> HARD = new ConcurrentHashMap<>();
	private static final Map<FeatureSetting, Double> FEATURES = new ConcurrentHashMap<>();
	private static volatile Map<FeatureSetting, Double> serverFeatures;
	private static volatile boolean serverEnabled;
	private static final Profile[] PROFILES = new Profile[PROFILE_COUNT];
	private static int saves;

	static {
		for (int i = 0; i < PROFILE_COUNT; i++) {
			PROFILES[i] = new Profile();
		}
	}

	private HardmodeConfig() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	public static boolean enabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean bossesInHardMode() {
		return bossesInHardMode;
	}

	public static void setBossesInHardMode(boolean value) {
		bossesInHardMode = value;
	}

	public static Mode mode() {
		return mode;
	}

	public static void setMode(Mode value) {
		mode = value;
	}

	public static boolean perMobActive() {
		return enabled && mode != Mode.PERCENT;
	}

	public static boolean hardModeActive() {
		return enabled && mode != Mode.PER_MOB;
	}

	public static Double getOverride(String mobId, MobStat stat) {
		Map<MobStat, Double> values = MOBS.get(mobId);
		return values == null ? null : values.get(stat);
	}

	public static double getEffective(MobList.Entry entry, MobStat stat) {
		Double override = getOverride(entry.id(), stat);
		return override != null ? override : MobList.vanilla(entry, stat);
	}

	public static void setOverride(MobList.Entry entry, MobStat stat, double value) {
		double clamped = Math.max(stat.min(), Math.min(stat.max(), value));
		if (Math.abs(clamped - MobList.vanilla(entry, stat)) < EPSILON) {
			Map<MobStat, Double> values = MOBS.get(entry.id());
			if (values != null) {
				values.remove(stat);
				if (values.isEmpty()) {
					MOBS.remove(entry.id());
				}
			}
			return;
		}
		MOBS.computeIfAbsent(entry.id(), id -> new ConcurrentHashMap<>()).put(stat, clamped);
	}

	public static void resetMob(String mobId, MobStat.Group group) {
		Map<MobStat, Double> values = MOBS.get(mobId);
		if (values == null) {
			return;
		}
		if (group == null) {
			MOBS.remove(mobId);
			return;
		}
		values.keySet().removeIf(stat -> stat.group == group);
		if (values.isEmpty()) {
			MOBS.remove(mobId);
		}
	}

	public static double getHard(HardArea area) {
		return HARD.getOrDefault(area, 0.0);
	}

	public static void setHard(HardArea area, double value) {
		double clamped = Math.max(HardArea.MIN, Math.min(HardArea.MAX, value));
		if (Math.abs(clamped) < EPSILON) {
			HARD.remove(area);
		} else {
			HARD.put(area, clamped);
		}
	}

	public static double hardBonusPercent(HardArea area) {
		if (area == HardArea.GENERAL) {
			return getHard(HardArea.GENERAL);
		}
		return Math.max(-95, getHard(HardArea.GENERAL) + getHard(area));
	}

	public static double get(FeatureSetting setting) {
		Map<FeatureSetting, Double> server = serverFeatures;
		return (server != null ? server : FEATURES).getOrDefault(setting, setting.defaultValue);
	}

	public static double own(FeatureSetting setting) {
		return FEATURES.getOrDefault(setting, setting.defaultValue);
	}

	public static boolean isOn(FeatureSetting setting) {
		return active() && get(setting) >= 0.5;
	}

	public static boolean active() {
		return serverFeatures != null ? serverEnabled : enabled;
	}

	static void useServerValues(boolean enabled, Map<FeatureSetting, Double> features) {
		serverEnabled = enabled;
		serverFeatures = features;
	}

	static void useOwnValues() {
		serverFeatures = null;
	}

	public static boolean isChanged(FeatureSetting setting) {
		return FEATURES.containsKey(setting);
	}

	public static void set(FeatureSetting setting, double value) {
		double clamped = Math.max(setting.min, Math.min(setting.max, value));
		if (setting.type != FeatureSetting.Type.NUMBER) {
			clamped = Math.round(clamped);
		}
		if (Math.abs(clamped - setting.defaultValue) < EPSILON) {
			FEATURES.remove(setting);
		} else {
			FEATURES.put(setting, clamped);
		}
	}

	public static void resetFeatures(FeatureSetting.Group group) {
		FEATURES.keySet().removeIf(setting -> setting.group == group);
	}

	public static int changeCount() {
		int count = HARD.size() + FEATURES.size();
		for (Map<MobStat, Double> values : MOBS.values()) {
			count += values.size();
		}
		return count;
	}

	public static void resetAll() {
		MOBS.clear();
		HARD.clear();
		FEATURES.clear();
	}

	public static String signature() {
		return enabled + "|" + bossesInHardMode + "|" + mode + "|" + new TreeMap<>(MOBS) + "|" + new TreeMap<>(HARD)
				+ "|" + new TreeMap<>(FEATURES);
	}

	public static int saveCount() {
		return saves;
	}

	public static Profile profile(int slot) {
		return PROFILES[slot];
	}

	public static void saveToProfile(int slot) {
		Profile profile = PROFILES[slot];
		profile.mobs = copyMobs(MOBS);
		profile.hard = new EnumMap<>(HardArea.class);
		profile.hard.putAll(HARD);
		profile.features = new EnumMap<>(FeatureSetting.class);
		profile.features.putAll(FEATURES);
		profile.mode = mode;
		profile.bosses = bossesInHardMode;
		profile.saved = true;
	}

	public static void loadProfile(int slot) {
		Profile profile = PROFILES[slot];
		if (profile.isEmpty()) {
			return;
		}
		replaceValues(profile.mobs, profile.hard);
		replaceFeatures(profile.features);
		mode = profile.mode;
		bossesInHardMode = profile.bosses;
	}

	public static void clearProfile(int slot) {
		PROFILES[slot] = new Profile();
	}

	private static void replaceValues(Map<String, Map<MobStat, Double>> mobs, Map<HardArea, Double> hard) {
		MOBS.clear();
		for (Map.Entry<String, Map<MobStat, Double>> e : mobs.entrySet()) {
			MOBS.put(e.getKey(), new ConcurrentHashMap<>(e.getValue()));
		}
		HARD.clear();
		HARD.putAll(hard);
	}

	private static void replaceFeatures(Map<FeatureSetting, Double> features) {
		FEATURES.clear();
		FEATURES.putAll(features);
	}

	private static Map<String, Map<MobStat, Double>> copyMobs(Map<String, Map<MobStat, Double>> source) {
		Map<String, Map<MobStat, Double>> copy = new TreeMap<>();
		for (Map.Entry<String, Map<MobStat, Double>> e : source.entrySet()) {
			Map<MobStat, Double> values = new EnumMap<>(MobStat.class);
			values.putAll(e.getValue());
			copy.put(e.getKey(), values);
		}
		return copy;
	}

	private static final DateTimeFormatter EXPORT_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

	public static Path exportFolder() {
		return FabricLoader.getInstance().getConfigDir().resolve(HardmodePlus.MOD_ID).resolve("profiles");
	}

	public static String exportCurrent() {
		JsonObject root = new JsonObject();
		root.addProperty("hardmodeplusExport", 1);
		root.add("mobs", writeMobs(MOBS));
		root.add("hard_mode", writeHard(HARD));
		root.add("features", writeFeatures(FEATURES));
		root.addProperty("mode", mode.key);
		root.addProperty("bosses_in_hard_mode", bossesInHardMode);
		String name = HardmodePlus.MOD_ID + "_" + LocalDateTime.now().format(EXPORT_NAME) + ".json";
		try {
			Files.createDirectories(exportFolder());
			try (Writer writer = Files.newBufferedWriter(exportFolder().resolve(name), StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			return name;
		} catch (IOException e) {
			HardmodePlus.LOGGER.error("Could not export the settings", e);
			return null;
		}
	}

	public static List<String> listExports() {
		Path folder = exportFolder();
		if (!Files.isDirectory(folder)) {
			return List.of();
		}
		try (Stream<Path> files = Files.list(folder)) {
			return files.filter(path -> path.getFileName().toString().endsWith(".json") && Files.isRegularFile(path))
					.sorted(Comparator.comparing(HardmodeConfig::modified).reversed())
					.map(path -> path.getFileName().toString())
					.toList();
		} catch (IOException e) {
			HardmodePlus.LOGGER.error("Could not list the exported files", e);
			return List.of();
		}
	}

	private static long modified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException e) {
			return 0;
		}
	}

	public static boolean deleteExport(String fileName) {
		if (!listExports().contains(fileName)) {
			return false;
		}
		try {
			return Files.deleteIfExists(exportFolder().resolve(fileName));
		} catch (IOException e) {
			HardmodePlus.LOGGER.error("Could not delete {}", fileName, e);
			return false;
		}
	}

	private static int ignored;

	public static boolean importFile(String fileName) {
		if (!listExports().contains(fileName)) {
			return false;
		}
		try (Reader reader = Files.newBufferedReader(exportFolder().resolve(fileName), StandardCharsets.UTF_8)) {
			JsonReader json = new JsonReader(reader);
			json.setStrictness(Strictness.STRICT);
			JsonObject root = JsonParser.parseReader(json).getAsJsonObject();
			if (json.peek() != JsonToken.END_DOCUMENT || !root.has("mobs") && !root.has("hard_mode")) {
				return false;
			}
			ignored = 0;
			Map<String, Map<MobStat, Double>> mobs = readMobs(root.getAsJsonObject("mobs"));
			Map<HardArea, Double> hard = readHard(root.getAsJsonObject("hard_mode"));
			Map<FeatureSetting, Double> features = readFeatures(root.getAsJsonObject("features"));
			if (ignored > 0) {
				HardmodePlus.LOGGER.error("Could not import {}: {} invalid entries", fileName, ignored);
				return false;
			}
			replaceValues(mobs, hard);
			replaceFeatures(features);
			if (root.has("mode")) {
				mode = Mode.byKey(root.get("mode").getAsString());
			}
			if (root.has("bosses_in_hard_mode")) {
				bossesInHardMode = root.get("bosses_in_hard_mode").getAsBoolean();
			}
			return true;
		} catch (IOException | RuntimeException e) {
			HardmodePlus.LOGGER.error("Could not import {}", fileName, e);
			return false;
		}
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			enabled = !root.has("enabled") || root.get("enabled").getAsBoolean();
			bossesInHardMode = !root.has("bosses_in_hard_mode") || root.get("bosses_in_hard_mode").getAsBoolean();
			mode = root.has("mode") ? Mode.byKey(root.get("mode").getAsString()) : Mode.BOTH;
			welcomeShown = root.has("welcome_shown") && root.get("welcome_shown").getAsBoolean();

			replaceValues(readMobs(root.getAsJsonObject("mobs")), readHard(root.getAsJsonObject("hard_mode")));
			replaceFeatures(readFeatures(root.getAsJsonObject("features")));

			JsonArray profiles = root.has("profiles") ? root.getAsJsonArray("profiles") : new JsonArray();
			for (int i = 0; i < PROFILE_COUNT; i++) {
				PROFILES[i] = new Profile();
				if (i < profiles.size() && profiles.get(i).isJsonObject()) {
					JsonObject p = profiles.get(i).getAsJsonObject();
					Profile profile = PROFILES[i];
					profile.name = p.has("name") ? p.get("name").getAsString() : "";
					profile.saved = p.has("saved") && p.get("saved").getAsBoolean();
					profile.mobs = new TreeMap<>(readMobs(p.getAsJsonObject("mobs")));
					profile.hard = new EnumMap<>(HardArea.class);
					profile.hard.putAll(readHard(p.getAsJsonObject("hard_mode")));
					profile.features = new EnumMap<>(FeatureSetting.class);
					profile.features.putAll(readFeatures(p.getAsJsonObject("features")));
					profile.mode = p.has("mode") ? Mode.byKey(p.get("mode").getAsString()) : Mode.BOTH;
					profile.bosses = !p.has("bosses_in_hard_mode") || p.get("bosses_in_hard_mode").getAsBoolean();
				}
			}
		} catch (IOException | RuntimeException e) {
			HardmodePlus.LOGGER.error("Could not read config/{}, using vanilla values", FILE_NAME, e);
			backupBrokenFile(path);
			MOBS.clear();
			HARD.clear();
			FEATURES.clear();
		}
	}

	private static void backupBrokenFile(Path path) {
		try {
			Files.copy(path, path.resolveSibling(FILE_NAME + ".broken"), StandardCopyOption.REPLACE_EXISTING);
			HardmodePlus.LOGGER.warn("Your old config was saved as {}.broken", FILE_NAME);
		} catch (IOException e) {
			HardmodePlus.LOGGER.error("Could not back up the broken config file", e);
		}
	}

	private static Map<String, Map<MobStat, Double>> readMobs(JsonObject json) {
		Map<String, Map<MobStat, Double>> result = new TreeMap<>();
		if (json == null) {
			return result;
		}
		for (Map.Entry<String, JsonElement> mob : json.entrySet()) {
			if (!mob.getValue().isJsonObject()) {
				ignored++;
				continue;
			}
			Map<MobStat, Double> values = new ConcurrentHashMap<>();
			for (Map.Entry<String, JsonElement> value : mob.getValue().getAsJsonObject().entrySet()) {
				MobStat stat = MobStat.byKey(value.getKey());
				if (stat == null || !value.getValue().isJsonPrimitive()) {
					HardmodePlus.LOGGER.warn("Ignoring unknown value {} for {}", value.getKey(), mob.getKey());
					ignored++;
					continue;
				}
				double number = value.getValue().getAsDouble();
				values.put(stat, Math.max(stat.min(), Math.min(stat.max(), number)));
			}
			if (!values.isEmpty()) {
				result.put(mob.getKey(), values);
			}
		}
		return result;
	}

	private static Map<HardArea, Double> readHard(JsonObject json) {
		Map<HardArea, Double> result = new EnumMap<>(HardArea.class);
		if (json == null) {
			return result;
		}
		for (Map.Entry<String, JsonElement> value : json.entrySet()) {
			HardArea area = HardArea.byKey(value.getKey());
			if (area == null || !value.getValue().isJsonPrimitive()) {
				ignored++;
				continue;
			}
			double number = Math.max(HardArea.MIN, Math.min(HardArea.MAX, value.getValue().getAsDouble()));
			if (Math.abs(number) >= EPSILON) {
				result.put(area, number);
			}
		}
		return result;
	}

	private static Map<FeatureSetting, Double> readFeatures(JsonObject json) {
		Map<FeatureSetting, Double> result = new EnumMap<>(FeatureSetting.class);
		if (json == null) {
			return result;
		}
		for (Map.Entry<String, JsonElement> value : json.entrySet()) {
			FeatureSetting setting = FeatureSetting.byKey(value.getKey());
			if (setting == null || !value.getValue().isJsonPrimitive()) {
				ignored++;
				continue;
			}
			JsonElement element = value.getValue();
			double number = element.getAsJsonPrimitive().isBoolean() ? (element.getAsBoolean() ? 1 : 0) : element.getAsDouble();
			number = Math.max(setting.min, Math.min(setting.max, number));
			if (Math.abs(number - setting.defaultValue) >= EPSILON) {
				result.put(setting, number);
			}
		}
		return result;
	}

	public static void save() {
		saves++;
		JsonObject root = new JsonObject();
		if (!enabled) {
			root.addProperty("enabled", false);
		}
		if (!bossesInHardMode) {
			root.addProperty("bosses_in_hard_mode", false);
		}
		if (mode != Mode.BOTH) {
			root.addProperty("mode", mode.key);
		}
		if (welcomeShown) {
			root.addProperty("welcome_shown", true);
		}
		if (!MOBS.isEmpty()) {
			root.add("mobs", writeMobs(MOBS));
		}
		if (!HARD.isEmpty()) {
			root.add("hard_mode", writeHard(HARD));
		}
		if (!FEATURES.isEmpty()) {
			root.add("features", writeFeatures(FEATURES));
		}

		JsonArray profiles = new JsonArray();
		boolean anyProfile = false;
		for (Profile profile : PROFILES) {
			JsonObject p = new JsonObject();
			if (!profile.name.isBlank()) {
				p.addProperty("name", profile.name);
				anyProfile = true;
			}
			if (profile.saved) {
				p.addProperty("saved", true);
				if (!profile.mobs.isEmpty()) {
					p.add("mobs", writeMobs(profile.mobs));
				}
				if (!profile.hard.isEmpty()) {
					p.add("hard_mode", writeHard(profile.hard));
				}
				if (!profile.features.isEmpty()) {
					p.add("features", writeFeatures(profile.features));
				}
				if (profile.mode != Mode.BOTH) {
					p.addProperty("mode", profile.mode.key);
				}
				if (!profile.bosses) {
					p.addProperty("bosses_in_hard_mode", false);
				}
				anyProfile = true;
			}
			profiles.add(p);
		}
		if (anyProfile) {
			root.add("profiles", profiles);
		}

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

	private static JsonObject writeMobs(Map<String, Map<MobStat, Double>> mobs) {
		JsonObject json = new JsonObject();
		for (Map.Entry<String, Map<MobStat, Double>> mob : new TreeMap<>(mobs).entrySet()) {
			JsonObject values = new JsonObject();
			Map<MobStat, Double> sorted = new EnumMap<>(MobStat.class);
			sorted.putAll(mob.getValue());
			for (Map.Entry<MobStat, Double> value : sorted.entrySet()) {
				values.addProperty(value.getKey().key, round(value.getValue()));
			}
			json.add(mob.getKey(), values);
		}
		return json;
	}

	private static JsonObject writeHard(Map<HardArea, Double> hard) {
		JsonObject json = new JsonObject();
		Map<HardArea, Double> sorted = new EnumMap<>(HardArea.class);
		sorted.putAll(hard);
		for (Map.Entry<HardArea, Double> value : sorted.entrySet()) {
			json.addProperty(value.getKey().key, round(value.getValue()));
		}
		return json;
	}

	private static JsonObject writeFeatures(Map<FeatureSetting, Double> features) {
		JsonObject json = new JsonObject();
		Map<FeatureSetting, Double> sorted = new EnumMap<>(FeatureSetting.class);
		sorted.putAll(features);
		for (Map.Entry<FeatureSetting, Double> value : sorted.entrySet()) {
			if (value.getKey().type == FeatureSetting.Type.SWITCH) {
				json.addProperty(value.getKey().key, value.getValue() >= 0.5);
			} else {
				json.addProperty(value.getKey().key, round(value.getValue()));
			}
		}
		return json;
	}

	private static double round(double value) {
		return Math.round(value * 10000.0) / 10000.0;
	}
}
