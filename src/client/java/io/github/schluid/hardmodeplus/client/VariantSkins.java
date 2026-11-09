package io.github.schluid.hardmodeplus.client;

import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.mobs.Variants;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public final class VariantSkins {
	public static final RenderStateDataKey<Integer> VARIANT = RenderStateDataKey.create(() -> "hardmodeplus:variant");
	public static final RenderStateDataKey<EntityType<?>> TYPE = RenderStateDataKey.create(() -> "hardmodeplus:type");

	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static final Identifier NONE = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "none");

	private VariantSkins() {
	}

	public static Identifier skin(EntityType<?> type, int variant, boolean baby) {
		if (variant <= Variants.NORMAL || variant >= Variants.NAMES.length) {
			return null;
		}
		String path = EntityType.getKey(type).getPath() + (baby ? "_baby_" : "_") + Variants.NAMES[variant];
		Identifier found = CACHE.computeIfAbsent(path, key -> {
			Identifier id = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "textures/entity/variant/" + key + ".png");
			return Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : NONE;
		});
		return found == NONE ? null : found;
	}

	public static Identifier eyes(EntityType<?> type, boolean baby) {
		String path = EntityType.getKey(type).getPath() + (baby ? "_baby" : "") + "_eyes";
		Identifier found = CACHE.computeIfAbsent(path, key -> {
			Identifier id = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "textures/entity/variant/" + key + ".png");
			return Minecraft.getInstance().getResourceManager().getResource(id).isPresent() ? id : NONE;
		});
		return found == NONE ? null : found;
	}
}
