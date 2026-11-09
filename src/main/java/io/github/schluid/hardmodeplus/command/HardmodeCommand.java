package io.github.schluid.hardmodeplus.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.Presets;
import java.util.Arrays;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class HardmodeCommand {
	private HardmodeCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> build(dispatcher));
	}

	private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(HardmodePlus.MOD_ID)
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(HardmodeCommand::status)
				.then(Commands.literal("status").executes(HardmodeCommand::status))
				.then(Commands.literal("reload").executes(HardmodeCommand::reload))
				.then(Commands.literal("on").executes(context -> setEnabled(context, true)))
				.then(Commands.literal("off").executes(context -> setEnabled(context, false)))
				.then(Commands.literal("preset")
						.then(Commands.argument("name", StringArgumentType.word())
								.suggests((context, builder) -> SharedSuggestionProvider.suggest(
										Arrays.stream(Presets.values()).map(preset -> preset.key), builder))
								.executes(HardmodeCommand::preset))));
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		String key = HardmodeConfig.enabled() ? "hardmodeplus.command.status.on" : "hardmodeplus.command.status.off";
		context.getSource().sendSuccess(() -> Component.translatable(key, HardmodeConfig.changeCount()), false);
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		HardmodeConfig.load();
		HardmodePlus.settingsChanged();
		context.getSource().sendSuccess(() -> Component.translatable("hardmodeplus.command.reload"), true);
		return 1;
	}

	private static int setEnabled(CommandContext<CommandSourceStack> context, boolean value) {
		HardmodeConfig.setEnabled(value);
		HardmodeConfig.save();
		HardmodePlus.settingsChanged();
		String key = value ? "hardmodeplus.command.on" : "hardmodeplus.command.off";
		context.getSource().sendSuccess(() -> Component.translatable(key), true);
		return 1;
	}

	private static int preset(CommandContext<CommandSourceStack> context) {
		String name = StringArgumentType.getString(context, "name");
		for (Presets preset : Presets.values()) {
			if (preset.key.equals(name)) {
				preset.apply();
				HardmodeConfig.save();
				HardmodePlus.settingsChanged();
				context.getSource().sendSuccess(() -> Component.translatable("hardmodeplus.command.preset",
						Component.translatable("hardmodeplus.preset." + preset.key)), true);
				return 1;
			}
		}
		context.getSource().sendFailure(Component.translatable("hardmodeplus.command.preset.unknown", name));
		return 0;
	}
}
