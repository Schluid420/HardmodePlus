package io.github.schluid.hardmodeplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardArea;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.MobStat;
import io.github.schluid.hardmodeplus.mobs.Variants;
import io.github.schluid.hardmodeplus.mobs.MobList;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;

public class ConfigScreen extends TipScreen {
	private static final int MAIN_TAB_Y = 22;
	private static final int SUB_TAB_Y = 44;
	private static final int ROW_HEIGHT = 22;
	private static final int BOTTOM_SPACE = 50;
	private static final int ICON_WIDTH = 20;
	private static final int NAME_WIDTH = 112;
	private static final int COLUMN_WIDTH = 54;
	private static final int BOX_WIDTH = 48;
	private static final int MAX_COLUMNS = 5;
	private static final int RESET_WIDTH = 26;
	private static final int TABLE_WIDTH = ICON_WIDTH + NAME_WIDTH + MAX_COLUMNS * COLUMN_WIDTH + RESET_WIDTH + 4;
	private static final int BAR_LEFT = 3;
	private static final int BAR_WIDTH = 4;
	private static final int BAR_GRAB = 10;

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GREY = 0xFFA0A0A0;
	private static final int DARK_GREY = 0xFF606060;
	private static final int RED = 0xFFFF5555;
	private static final int YELLOW = 0xFFFFE066;
	private static final int GREEN = 0xFF66E066;
	private static final int ROW_DARK = 0x30000000;
	private static final int ROW_LIGHT = 0x18FFFFFF;
	private static final int SHADOW = 0xFF3F3F3F;
	private static final int LOCKED = 0xFF707070;

	private static final String[] RESET_ICON = {
			"..###.#",
			".#...##",
			"#...###",
			"#......",
			"#.....#",
			".#...#.",
			"..###.."};
	private static final int RESET_PIXEL = 2;

	private enum MainTab {
		MOBS, FEATURES, MULTIPLIER, VARIANTS;

		boolean featureLike() {
			return this == FEATURES || this == VARIANTS;
		}

		MainTab button() {
			return this == VARIANTS ? MOBS : this;
		}

		String key() {
			return "hardmodeplus.tab." + name().toLowerCase(Locale.ROOT);
		}
	}

	private record Cell(String id, String headerKey, DoubleSupplier current, double vanilla, double min, double max,
			DoubleConsumer setter, BooleanSupplier changed, String unit) {
	}

	private record Row(String iconItem, Component name, Component kindHint, List<Cell> cells, Runnable reset,
			FeatureSetting setting, String section, int indent) {
		Row(String iconItem, Component name, Component kindHint, List<Cell> cells, Runnable reset, FeatureSetting setting) {
			this(iconItem, name, kindHint, cells, reset, setting, null, 0);
		}

		boolean heading() {
			return section != null;
		}
	}

	private static final int INDENT = 12;
	private static final int HEADING = 0x50000000;
	private static final Set<String> OPEN = new HashSet<>();

	private static final int FEATURE_COLUMN = 2;

	private static MainTab mainTab = MainTab.MOBS;
	private static MobStat.Group group = MobStat.Group.COMBAT;
	private static FeatureSetting.Group featureGroup = FeatureSetting.Group.HEALTH;
	private static final Map<String, Integer> SCROLL = new HashMap<>();

	private final Map<String, Double> saved = new HashMap<>();
	private int savedAt = -1;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private List<String> headers = List.of();
	private int visibleRows;
	private final List<int[]> resetButtons = new ArrayList<>();
	private boolean draggingBar;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("hardmodeplus.config.title"));
		this.parent = parent;
	}

	private String viewKey() {
		return switch (mainTab) {
			case MOBS -> "mobs." + group.name();
			case FEATURES -> "features." + featureGroup.name();
			case MULTIPLIER, VARIANTS -> mainTab.name();
		};
	}

	private int headerY() {
		return mainTab == MainTab.MULTIPLIER ? 50 : 72;
	}

	private int firstColumn() {
		return mainTab.featureLike() ? FEATURE_COLUMN : 0;
	}

	private int tableTop() {
		return headerY() + 12;
	}

	private int tableLeft() {
		return this.width / 2 - TABLE_WIDTH / 2;
	}

	private int tableRight() {
		return tableLeft() + TABLE_WIDTH;
	}

	private int columnX(int column) {
		return tableLeft() + ICON_WIDTH + NAME_WIDTH + column * COLUMN_WIDTH;
	}

	private int scroll() {
		return SCROLL.getOrDefault(viewKey(), 0);
	}

	private void setScroll(int value) {
		SCROLL.put(viewKey(), Math.max(0, Math.min(maxScroll(), value)));
	}

	private int maxScroll() {
		return Math.max(0, rows.size() - visibleRows);
	}

	private int trackHeight() {
		return visibleRows * ROW_HEIGHT;
	}

	private int barHeight() {
		return Math.max(10, trackHeight() * visibleRows / Math.max(1, rows.size()));
	}

	private static FeatureSetting blocker(Row row) {
		if (row.setting() != null || row.section() == null) {
			return blocker(row.setting());
		}
		for (FeatureSetting setting : FeatureSetting.values()) {
			if (setting.section().equals(row.section())) {
				return blocker(setting);
			}
		}
		return null;
	}

	private static FeatureSetting blocker(FeatureSetting setting) {
		if (setting == null) {
			return null;
		}
		FeatureSetting blocker = null;
		for (FeatureSetting parent = setting.parent(); parent != null; parent = parent.parent()) {
			if (HardmodeConfig.own(parent) < 0.5) {
				blocker = parent;
			}
		}
		return blocker;
	}

	private static Component lockedTip(FeatureSetting blocker) {
		return Component.translatable("hardmodeplus.config.locked",
				Component.translatable("hardmodeplus.feature." + blocker.key)).withStyle(ChatFormatting.GOLD);
	}

	private void buildRows() {
		if (savedAt != HardmodeConfig.saveCount()) {
			saved.clear();
			savedAt = HardmodeConfig.saveCount();
		}
		rows.clear();
		if (mainTab.featureLike()) {
			headers = List.of("hardmodeplus.config.column.value");
			Map<String, List<FeatureSetting>> sections = new LinkedHashMap<>();
			FeatureSetting.Group shown = mainTab == MainTab.VARIANTS ? FeatureSetting.Group.VARIANTS : featureGroup;
			for (FeatureSetting setting : FeatureSetting.inGroup(shown)) {
				sections.computeIfAbsent(setting.section(), key -> new ArrayList<>()).add(setting);
			}
			for (Map.Entry<String, List<FeatureSetting>> section : sections.entrySet()) {
				addSection(section.getKey(), section.getValue());
			}
			return;
		}
		if (mainTab == MainTab.MULTIPLIER) {
			headers = List.of("hardmodeplus.config.column.percent");
			for (HardArea area : HardArea.values()) {
				List<Cell> cells = new ArrayList<>();
				cells.add(cell("hard." + area.key, "hardmodeplus.config.column.percent",
						() -> HardmodeConfig.getHard(area), 0, HardArea.MIN, HardArea.MAX,
						value -> HardmodeConfig.setHard(area, value),
						() -> HardmodeConfig.getHard(area) != 0, "%"));
				rows.add(new Row(area.iconItem, Component.translatable("hardmodeplus.hard." + area.key),
						Component.translatable("hardmodeplus.hard." + area.key + ".tip"),
						cells, () -> HardmodeConfig.setHard(area, 0), null));
			}
			return;
		}

		List<MobStat> stats = MobStat.inGroup(group);
		List<String> keys = new ArrayList<>();
		for (MobStat stat : stats) {
			keys.add("hardmodeplus.stat." + stat.key);
		}
		headers = keys;
		MobStat.Group resetGroup = group;
		for (MobList.Entry entry : MobList.all()) {
			List<Cell> cells = new ArrayList<>();
			for (MobStat stat : stats) {
				if (!MobList.has(entry, stat)) {
					cells.add(null);
					continue;
				}
				cells.add(cell(entry.id() + "." + stat.key, "hardmodeplus.stat." + stat.key,
						() -> HardmodeConfig.getEffective(entry, stat),
						MobList.vanilla(entry, stat), stat.min(), stat.max(),
						value -> HardmodeConfig.setOverride(entry, stat, value),
						() -> HardmodeConfig.getOverride(entry.id(), stat) != null, ""));
			}
			rows.add(new Row(MobList.iconItem(entry), entry.type().getDescription(),
					Component.translatable("hardmodeplus.kind." + entry.kind().name().toLowerCase(Locale.ROOT)),
					cells, () -> HardmodeConfig.resetMob(entry.id(), resetGroup), null));
		}
	}

	private void addSection(String section, List<FeatureSetting> settings) {
		FeatureSetting main = FeatureSetting.sectionSwitch(section);
		Component name = main != null ? Component.translatable("hardmodeplus.feature." + main.key)
				: Component.translatable("hardmodeplus.section." + section);
		Component hint = hint(main != null ? "hardmodeplus.feature." + main.key : "hardmodeplus.section." + section);
		List<Cell> none = new ArrayList<>();
		none.add(null);
		rows.add(new Row(null, name, hint, none, () -> settings.forEach(s -> HardmodeConfig.set(s, s.defaultValue)),
				main, section, 0));
		if (!OPEN.contains(section)) {
			return;
		}
		for (FeatureSetting setting : settings) {
			if (setting == main) {
				continue;
			}
			int indent = setting.parent() != null && setting.parent() != main && setting.parent().section().equals(section)
					? 2 : 1;
			String key = "hardmodeplus.feature." + setting.key;
			List<Cell> cells = new ArrayList<>();
			cells.add(setting.type != FeatureSetting.Type.NUMBER ? null : cell("feature." + setting.key,
					key, () -> HardmodeConfig.own(setting), setting.defaultValue, setting.min, setting.max,
					value -> HardmodeConfig.set(setting, value), () -> HardmodeConfig.isChanged(setting), ""));
			rows.add(new Row(null, Component.translatable(key), hint(key),
					cells, () -> HardmodeConfig.set(setting, setting.defaultValue), setting, null, indent));
		}
	}

	private static Component hint(String key) {
		Component tip = Component.translatable(key + ".tip");
		if (!Language.getInstance().has(key + ".warn")) {
			return tip;
		}
		return tip.copy().append(" ").append(Component.translatable(key + ".warn").withStyle(ChatFormatting.RED));
	}

	private static Component liveHint(Row row) {
		Component tip = row.kindHint().copy().withStyle(ChatFormatting.GRAY);
		FeatureSetting setting = row.setting();
		if (setting == FeatureSetting.FAST_WEIGHT || setting == FeatureSetting.TOUGH_WEIGHT
				|| setting == FeatureSetting.STRONG_WEIGHT || setting == FeatureSetting.RANDOM_WEIGHT) {
			String share = String.format(Locale.ROOT, "%.1f", Variants.sharePercent(setting));
			return tip.copy().append(" ").append(Component.translatable("hardmodeplus.feature.variant_weight.share", share)
					.withStyle(ChatFormatting.YELLOW));
		}
		return tip;
	}

	private static boolean hasSettings(String section) {
		FeatureSetting main = FeatureSetting.sectionSwitch(section);
		for (FeatureSetting setting : FeatureSetting.values()) {
			if (setting.section().equals(section) && setting != main) {
				return true;
			}
		}
		return false;
	}

	private int nameLeft(Row row) {
		return tableLeft() + (row.setting() != null || row.heading() ? 4 + row.indent() * INDENT : ICON_WIDTH);
	}

	private Cell cell(String id, String headerKey, DoubleSupplier current, double vanilla, double min, double max,
			DoubleConsumer setter, BooleanSupplier changed, String unit) {
		saved.putIfAbsent(id, current.getAsDouble());
		return new Cell(id, headerKey, current, vanilla, min, max, setter, changed, unit);
	}

	private int color(Cell cell) {
		if (!cell.changed().getAsBoolean()) {
			return WHITE;
		}
		Double inFile = saved.get(cell.id());
		return inFile != null && Math.abs(inFile - cell.current().getAsDouble()) < 1.0E-6 ? GREEN : YELLOW;
	}

	@Override
	protected void init() {
		clearTips();
		buildRows();
		visibleRows = Math.max(3, (this.height - tableTop() - BOTTOM_SPACE) / ROW_HEIGHT);
		setScroll(scroll());

		// Main tabs
		MainTab[] mains = {MainTab.MOBS, MainTab.FEATURES, MainTab.MULTIPLIER};
		int mainWidth = Math.min(120, (this.width - 20) / mains.length - 4);
		int mainLeft = this.width / 2 - (mains.length * (mainWidth + 4) - 4) / 2;
		for (int i = 0; i < mains.length; i++) {
			MainTab t = mains[i];
			Button button = Button.builder(Component.translatable(t.key()), b -> {
				mainTab = t;
				this.rebuildWidgets();
			}).bounds(mainLeft + i * (mainWidth + 4), MAIN_TAB_Y, mainWidth, 20).build();
			button.active = t != mainTab.button();
			this.addRenderableWidget(tip(button, Component.translatable(t.key()),
					Component.translatable(t.key() + ".tip").withStyle(ChatFormatting.GRAY)));
		}

		// Sub tabs of the mob settings and of the features
		if (mainTab == MainTab.MOBS || mainTab == MainTab.VARIANTS) {
			MobStat.Group[] groups = MobStat.Group.values();
			int count = groups.length + 1;
			for (int i = 0; i < groups.length; i++) {
				MobStat.Group g = groups[i];
				subTab(i, count, "hardmodeplus.tab." + g.name().toLowerCase(Locale.ROOT),
						mainTab == MainTab.MOBS && g == group, () -> {
							mainTab = MainTab.MOBS;
							group = g;
						});
			}
			subTab(groups.length, count, MainTab.VARIANTS.key(), mainTab == MainTab.VARIANTS, () -> mainTab = MainTab.VARIANTS);
		} else if (mainTab == MainTab.FEATURES) {
			List<FeatureSetting.Group> groups = new ArrayList<>();
			for (FeatureSetting.Group g : FeatureSetting.Group.values()) {
				if (g.isFeatureTab()) {
					groups.add(g);
				}
			}
			for (int i = 0; i < groups.size(); i++) {
				FeatureSetting.Group g = groups.get(i);
				subTab(i, groups.size(), "hardmodeplus.features." + g.name().toLowerCase(Locale.ROOT), g == featureGroup,
						() -> featureGroup = g);
			}
		}

		// Table rows
		resetButtons.clear();
		for (int r = 0; r < visibleRows; r++) {
			int index = scroll() + r;
			if (index >= rows.size()) {
				break;
			}
			Row row = rows.get(index);
			int y = tableTop() + r * ROW_HEIGHT;
			// Grey and locked while the main switch of this setting is off
			FeatureSetting blocker = blocker(row);
			boolean locked = blocker != null;

			if (row.setting() != null && row.setting().type != FeatureSetting.Type.NUMBER) {
				Button button = featureButton(row.setting(), columnX(firstColumn()), y + 1, locked);
				if (locked) {
					this.addRenderableWidget(tip(button, row.name(),
							row.kindHint().copy().withStyle(ChatFormatting.GRAY), lockedTip(blocker)));
				} else {
					this.addRenderableWidget(tip(button, row.name(), row.kindHint().copy().withStyle(ChatFormatting.GRAY)));
				}
			}
			for (int c = 0; c < row.cells().size(); c++) {
				Cell cell = row.cells().get(c);
				if (cell == null) {
					continue;
				}
				EditBox box = new EditBox(this.font, columnX(firstColumn() + c), y + 2, BOX_WIDTH, ROW_HEIGHT - 4,
						Component.translatable(cell.headerKey()));
				box.setMaxLength(9);
				box.setValue(format(cell.current().getAsDouble()));
				box.setTextColor(color(cell));
				if (locked) {
					box.setEditable(false);
					box.setTextColorUneditable(LOCKED);
				}
				box.setResponder(text -> {
					Double parsed = parse(text);
					if (parsed == null || parsed < cell.min() || parsed > cell.max()) {
						box.setTextColor(RED);
						return;
					}
					cell.setter().accept(parsed);
					box.setTextColor(color(cell));
				});
				Component first = Component.translatable(cell.headerKey());
				Component standard = Component.translatable(row.setting() != null ? "hardmodeplus.config.default"
						: "hardmodeplus.config.vanilla", format(cell.vanilla()) + cell.unit());
				Component range = Component.translatable("hardmodeplus.config.range",
						format(cell.min()) + cell.unit(), format(cell.max()) + cell.unit()).withStyle(ChatFormatting.GRAY);
				if (locked) {
					this.addRenderableWidget(tip(box, first, standard, range, lockedTip(blocker)));
				} else {
					this.addRenderableWidget(tip(box, first, standard, range));
				}
			}

			Button reset = Button.builder(Component.empty(), b -> {
				row.reset().run();
				this.rebuildWidgets();
			}).bounds(columnX(MAX_COLUMNS) + 2, y, RESET_WIDTH, ROW_HEIGHT).build();
			reset.active = !locked;
			Component resetTip = Component.translatable(mainTab == MainTab.MULTIPLIER ? "hardmodeplus.config.reset_row_hard"
					: row.heading() ? "hardmodeplus.config.reset_section"
					: row.setting() != null ? "hardmodeplus.config.reset_feature" : "hardmodeplus.config.reset_row");
			if (locked) {
				this.addRenderableWidget(tip(reset, resetTip, lockedTip(blocker)));
			} else {
				this.addRenderableWidget(tip(reset, resetTip));
			}
			resetButtons.add(new int[] {columnX(MAX_COLUMNS) + 2, y, locked ? 1 : 0});
		}

		// Bottom buttons
		int buttonWidth = 100;
		int bottomLeft = this.width / 2 - (4 * (buttonWidth + 4) - 4) / 2;
		int by = this.height - 28;
		this.addRenderableWidget(tip(Button.builder(Component.translatable("hardmodeplus.config.all_vanilla"), b -> {
			// Ask first: this removes every change at once
			BooleanConsumer answer = yes -> {
				if (yes) {
					HardmodeConfig.resetAll();
				}
				this.minecraft.setScreenAndShow(this);
			};
			this.minecraft.setScreenAndShow(new ConfirmScreen(answer,
					Component.translatable("hardmodeplus.reset.title"),
					Component.translatable("hardmodeplus.reset.message"),
					Component.translatable("gui.yes"),
					Component.translatable("gui.no")));
		}).bounds(bottomLeft, by, buttonWidth, 20).build(),
				Component.translatable("hardmodeplus.config.all_vanilla"),
				Component.translatable("hardmodeplus.config.all_vanilla.tip").withStyle(ChatFormatting.GRAY)));
		this.addRenderableWidget(Button.builder(Component.translatable("hardmodeplus.config.profiles"),
				b -> this.minecraft.setScreenAndShow(new ProfilesScreen(this)))
				.bounds(bottomLeft + (buttonWidth + 4), by, buttonWidth, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("hardmodeplus.config.options"),
				b -> this.minecraft.setScreenAndShow(new OptionsScreen(this)))
				.bounds(bottomLeft + 2 * (buttonWidth + 4), by, buttonWidth, 20).build());
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
				.bounds(bottomLeft + 3 * (buttonWidth + 4), by, buttonWidth, 20).build());
	}

	private void subTab(int index, int count, String key, boolean open, Runnable select) {
		int subWidth = Math.min(90, (this.width - 20) / count - 4);
		int subLeft = this.width / 2 - (count * (subWidth + 4) - 4) / 2;
		Button button = Button.builder(Component.translatable(key), b -> {
			select.run();
			this.rebuildWidgets();
		}).bounds(subLeft + index * (subWidth + 4), SUB_TAB_Y, subWidth, 20).build();
		button.active = !open;
		this.addRenderableWidget(tip(button, Component.translatable(key),
				Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY)));
	}

	private Button featureButton(FeatureSetting setting, int x, int y, boolean locked) {
		Component label;
		if (setting.type == FeatureSetting.Type.SWITCH) {
			boolean on = HardmodeConfig.own(setting) >= 0.5;
			label = Component.translatable(on ? "options.on" : "options.off")
					.withStyle(locked ? ChatFormatting.GRAY : on ? ChatFormatting.GREEN : ChatFormatting.RED);
		} else {
			label = Component.translatable("hardmodeplus.feature." + setting.key + "." + (int) HardmodeConfig.own(setting))
					.withStyle(locked ? ChatFormatting.GRAY : ChatFormatting.YELLOW);
		}
		Button button = Button.builder(label, b -> {
			double next = HardmodeConfig.own(setting) + 1;
			HardmodeConfig.set(setting, next > setting.max ? setting.min : next);
			this.rebuildWidgets();
		}).bounds(x, y, 2 * COLUMN_WIDTH - 6, ROW_HEIGHT - 2).build();
		button.active = !locked;
		return button;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0 && maxScroll() > 0) {
			int next = Math.max(0, Math.min(maxScroll(), scroll() + (scrollY > 0 ? -1 : 1)));
			if (next != scroll()) {
				setScroll(next);
				this.rebuildWidgets();
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private boolean overBar(double mouseX, double mouseY) {
		int left = tableRight() + 1;
		return maxScroll() > 0 && mouseX >= left && mouseX < left + BAR_GRAB
				&& mouseY >= tableTop() && mouseY < tableTop() + trackHeight();
	}

	private void dragBarTo(double mouseY) {
		int free = trackHeight() - barHeight();
		if (free <= 0) {
			return;
		}
		double fraction = (mouseY - tableTop() - barHeight() / 2.0) / free;
		int next = (int) Math.round(Math.max(0, Math.min(1, fraction)) * maxScroll());
		if (next != scroll()) {
			setScroll(next);
			this.rebuildWidgets();
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && overBar(event.x(), event.y())) {
			draggingBar = true;
			dragBarTo(event.y());
			return true;
		}
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && mainTab.featureLike()) {
			int r = (int) Math.floor((event.y() - tableTop()) / ROW_HEIGHT);
			int index = scroll() + r;
			if (event.y() >= tableTop() && r < visibleRows && index < rows.size()) {
				Row row = rows.get(index);
				if (row.heading() && hasSettings(row.section()) && event.x() >= tableLeft()
						&& event.x() < columnX(firstColumn()) - 4) {
					if (!OPEN.remove(row.section())) {
						OPEN.add(row.section());
					}
					this.rebuildWidgets();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (draggingBar) {
			dragBarTo(event.y());
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingBar && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			draggingBar = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		if (draggingBar) {
			dragBarTo(mouseY);
		}

		int left = tableLeft();
		int right = tableRight();
		int top = tableTop();

		// Row stripes behind the widgets
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			int y = top + r * ROW_HEIGHT;
			boolean heading = rows.get(scroll() + r).heading();
			graphics.fill(left, y, right, y + ROW_HEIGHT, heading ? HEADING : r % 2 == 0 ? ROW_DARK : ROW_LIGHT);
		}

		super.extractRenderState(graphics, mouseX, mouseY, delta);

		Component title = this.title.copy().append(Component.translatable("hardmodeplus.config.changes",
				HardmodeConfig.changeCount()).withStyle(ChatFormatting.GRAY));
		if (!HardmodeConfig.enabled()) {
			title = title.copy().append(Component.translatable("hardmodeplus.config.disabled").withStyle(ChatFormatting.RED));
		}
		graphics.centeredText(this.font, title, this.width / 2, 8, WHITE);

		if (mainTab.featureLike() && rows.isEmpty()) {
			drawFittedCentered(graphics, Component.translatable("hardmodeplus.features.coming"),
					this.width / 2, tableTop() + 8, TABLE_WIDTH, GREY);
			drawNote(graphics);
			drawTips(graphics, mouseX, mouseY);
			return;
		}

		// Column headers (hover for an explanation)
		int headerY = headerY();
		Component firstColumn = Component.translatable(switch (mainTab) {
			case MOBS -> "hardmodeplus.config.column.mob";
			case FEATURES, VARIANTS -> "hardmodeplus.config.column.setting";
			case MULTIPLIER -> "hardmodeplus.config.column.area";
		});
		graphics.text(this.font, firstColumn, left + ICON_WIDTH, headerY, GREY);
		List<Component> hoveredTip = null;
		for (int c = 0; c < headers.size(); c++) {
			String key = headers.get(c);
			int x = columnX(firstColumn() + c);
			graphics.text(this.font, Component.translatable(key + ".short"), x, headerY, GREY);
			if (mouseX >= x && mouseX < x + COLUMN_WIDTH && mouseY >= headerY - 2 && mouseY < headerY + 10) {
				hoveredTip = List.of(Component.translatable(key),
						Component.translatable(key + ".tip").withStyle(ChatFormatting.GRAY));
			}
		}
		graphics.horizontalLine(left, right - 1, top - 2, DARK_GREY);

		// Icons, names and dashes for values a mob does not have
		for (int r = 0; r < visibleRows && scroll() + r < rows.size(); r++) {
			Row row = rows.get(scroll() + r);
			int y = top + r * ROW_HEIGHT;
			if (row.iconItem() != null) {
				boolean bound = BuiltInRegistries.ITEM.get(Identifier.parse(row.iconItem()))
						.map(Holder.Reference::areComponentsBound).orElse(false);
				if (bound) {
					graphics.item(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(row.iconItem()))),
							left + 2, y + 3);
				}
			}
			FeatureSetting blocker = blocker(row);
			int nameX = nameLeft(row);
			int nameY = y + (ROW_HEIGHT - 8) / 2;
			int nameWidth = columnX(firstColumn()) - nameX - 4;
			Component shown = row.name();
			if (row.heading()) {
				String arrow = !hasSettings(row.section()) ? "  " : OPEN.contains(row.section()) ? "▼ " : "▶ ";
				shown = Component.literal(arrow).append(row.name()).withStyle(ChatFormatting.BOLD);
			}
			drawFitted(graphics, shown, nameX, nameY, nameWidth, blocker != null ? LOCKED : WHITE);
			if (mouseX >= nameX && mouseX < nameX + nameWidth && mouseY >= y && mouseY < y + ROW_HEIGHT) {
				Component hintShown = liveHint(row);
				hoveredTip = blocker != null
						? List.of(row.name(), hintShown, lockedTip(blocker))
						: List.of(row.name(), hintShown);
			}
			if (row.setting() == null && !row.heading()) {
				for (int c = 0; c < row.cells().size(); c++) {
					if (row.cells().get(c) == null) {
						graphics.text(this.font, Component.literal("–"), columnX(c) + BOX_WIDTH / 2 - 2, nameY, DARK_GREY);
					}
				}
			}
		}

		int iconSize = RESET_ICON.length * RESET_PIXEL;
		for (int[] pos : resetButtons) {
			int iconX = pos[0] + (RESET_WIDTH - iconSize) / 2;
			int iconY = pos[1] + (ROW_HEIGHT - iconSize) / 2;
			drawIcon(graphics, iconX + 1, iconY + 1, SHADOW);
			drawIcon(graphics, iconX, iconY, pos[2] == 1 ? LOCKED : WHITE);
		}

		// Scrollbar, can be dragged with the mouse
		if (maxScroll() > 0) {
			int barX = right + BAR_LEFT;
			int barY = top + (trackHeight() - barHeight()) * scroll() / maxScroll();
			boolean hot = draggingBar || overBar(mouseX, mouseY);
			graphics.fill(barX, top, barX + BAR_WIDTH, top + trackHeight(), 0x40FFFFFF);
			graphics.fill(barX, barY, barX + BAR_WIDTH, barY + barHeight(), hot ? WHITE : 0xFFC0C0C0);
		}

		drawNote(graphics);

		if (hoveredTip != null) {
			showTip(graphics, hoveredTip, mouseX, mouseY);
		} else {
			drawTips(graphics, mouseX, mouseY);
		}
	}

	private void drawNote(GuiGraphicsExtractor graphics) {
		drawFittedCentered(graphics, note(), this.width / 2, this.height - 42, this.width - 20, GREY);
	}

	private Component note() {
		if (mainTab.featureLike()) {
			return Component.translatable(HardmodeConfig.enabled() ? "hardmodeplus.config.note_features"
					: "hardmodeplus.config.note_features_off");
		}
		if (mainTab != MainTab.MULTIPLIER) {
			if (HardmodeConfig.mode() == HardmodeConfig.Mode.PERCENT) {
				return Component.translatable("hardmodeplus.config.per_mob_off").withStyle(ChatFormatting.GOLD);
			}
			return Component.translatable("hardmodeplus.config.note");
		}
		if (HardmodeConfig.mode() == HardmodeConfig.Mode.PER_MOB) {
			return Component.translatable("hardmodeplus.config.hard_off").withStyle(ChatFormatting.GOLD);
		}
		if (this.minecraft.level == null) {
			return Component.translatable("hardmodeplus.config.note_hard");
		}
		Difficulty difficulty = this.minecraft.level.getDifficulty();
		Component state = difficulty == Difficulty.HARD
				? Component.translatable("hardmodeplus.config.hard_active").withStyle(ChatFormatting.GREEN)
				: Component.translatable("hardmodeplus.config.hard_inactive", difficulty.getDisplayName())
						.withStyle(ChatFormatting.GOLD);
		return Component.translatable("hardmodeplus.config.note_hard").append(" ").append(state);
	}

	private static void drawIcon(GuiGraphicsExtractor graphics, int x, int y, int color) {
		for (int row = 0; row < RESET_ICON.length; row++) {
			String line = RESET_ICON[row];
			for (int col = 0; col < line.length(); col++) {
				if (line.charAt(col) == '#') {
					int px = x + col * RESET_PIXEL;
					int py = y + row * RESET_PIXEL;
					graphics.fill(px, py, px + RESET_PIXEL, py + RESET_PIXEL, color);
				}
			}
		}
	}

	@Override
	public void onClose() {
		HardmodeConfig.save();
		HardmodePlus.settingsChanged();
		this.minecraft.setScreenAndShow(this.parent);
	}

	static Double parse(String text) {
		try {
			double value = Double.parseDouble(text.trim().replace(',', '.'));
			return Double.isFinite(value) ? value : null;
		} catch (NumberFormatException e) {
			return null;
		}
	}

	static String format(double value) {
		double rounded = Math.round(value * 1000.0) / 1000.0;
		if (rounded == Math.rint(rounded)) {
			return String.valueOf((long) rounded);
		}
		return String.format(Locale.ROOT, "%.3f", rounded).replaceAll("0+$", "");
	}
}
