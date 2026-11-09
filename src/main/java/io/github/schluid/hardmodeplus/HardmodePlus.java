package io.github.schluid.hardmodeplus;

import io.github.schluid.hardmodeplus.command.HardmodeCommand;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import io.github.schluid.hardmodeplus.config.ServerSettings;
import io.github.schluid.hardmodeplus.effect.ModEffects;
import io.github.schluid.hardmodeplus.feature.Features;
import io.github.schluid.hardmodeplus.feature.Injuries;
import io.github.schluid.hardmodeplus.feature.Stamina;
import io.github.schluid.hardmodeplus.feature.Thirst;
import io.github.schluid.hardmodeplus.item.ModItems;
import io.github.schluid.hardmodeplus.mobs.Growth;
import io.github.schluid.hardmodeplus.mobs.MobApplier;
import io.github.schluid.hardmodeplus.mobs.Variants;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HardmodePlus implements ModInitializer {
	public static final String MOD_ID = "hardmodeplus";
	public static final Logger LOGGER = LoggerFactory.getLogger("HardmodePlus");

	private static volatile MinecraftServer server;
	private static Difficulty lastDifficulty;
	private static String lastSignature = "";

	@Override
	public void onInitialize() {
		HardmodeConfig.load();
		lastSignature = HardmodeConfig.signature();
		ModEffects.register();
		ModItems.register();
		Injuries.register();
		Thirst.register();
		Stamina.register();
		HardmodeCommand.register();
		ServerSettings.register();

		ServerLifecycleEvents.SERVER_STARTED.register(s -> {
			server = s;
			lastDifficulty = s.getWorldData().getDifficulty();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
			server = null;
			Growth.reset();
		});

		ServerEntityEvents.ENTITY_LOAD.register(MobApplier::apply);
		Variants.register();
		Features.register();

		ServerTickEvents.END_SERVER_TICK.register(s -> {
			Features.tick(s);
			Growth.tick(s);
			if (s.getTickCount() % 20 != 0) {
				return;
			}
			Difficulty difficulty = s.getWorldData().getDifficulty();
			if (difficulty != lastDifficulty) {
				lastDifficulty = difficulty;
				MobApplier.applyAll(s);
			}
		});
	}

	public static void settingsChanged() {
		String signature = HardmodeConfig.signature();
		if (signature.equals(lastSignature)) {
			return;
		}
		lastSignature = signature;
		MinecraftServer running = server;
		if (running != null) {
			running.execute(() -> {
				MobApplier.applyAll(running);
				Variants.applyAll(running);
			});
		}
	}
}
