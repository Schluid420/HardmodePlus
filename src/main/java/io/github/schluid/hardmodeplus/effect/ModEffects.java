package io.github.schluid.hardmodeplus.effect;

import io.github.schluid.hardmodeplus.HardmodePlus;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	public static final Holder<MobEffect> BLEEDING = register("bleeding", 0xB02020);
	public static final Holder<MobEffect> HEAVY_BLEEDING = register("heavy_bleeding", 0x6A0000);
	public static final Holder<MobEffect> FRACTURE = register("fracture", 0xD8D0B8);
	public static final Holder<MobEffect> IN_CAST = register("in_cast", 0xF0F0F0);
	public static final Holder<MobEffect> SPLINTED = register("splinted", 0xA07840);
	public static final Holder<MobEffect> DAZED = register("dazed", 0xE0D040);
	public static final Holder<MobEffect> TIRED = register("tired", 0x4A4A8A);

	private ModEffects() {
	}

	private static Holder<MobEffect> register(String name, int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
				Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, name), new Marker(color));
	}

	public static void register() {
	}

	private static final class Marker extends MobEffect {
		Marker(int color) {
			super(MobEffectCategory.HARMFUL, color);
		}
	}
}
