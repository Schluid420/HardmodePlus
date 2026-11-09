package io.github.schluid.hardmodeplus.item;

import com.mojang.serialization.Codec;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.feature.Injuries;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModItems {
	public static final DataComponentType<Integer> SIPS = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
			id("sips"), DataComponentType.<Integer>builder().persistent(Codec.INT)
					.networkSynchronized(ByteBufCodecs.VAR_INT).build());

	public static final Item CANTEEN = register("canteen", CanteenItem::new);
	public static final Item WATER_BOWL = register("water_bowl", WaterBowlItem::new);
	public static final Item BANDAGE = register("bandage",
			p -> new TreatmentItem(p, Injuries.Treatment.BANDAGE, FeatureSetting.BANDAGE_SECONDS));
	public static final Item HERBAL_BANDAGE = register("herbal_bandage",
			p -> new TreatmentItem(p, Injuries.Treatment.HERBAL, FeatureSetting.HERBAL_SECONDS));
	public static final Item TOURNIQUET = register("tourniquet",
			p -> new TreatmentItem(p, Injuries.Treatment.TOURNIQUET, FeatureSetting.TOURNIQUET_SECONDS));
	public static final Item SPLINT = register("splint",
			p -> new TreatmentItem(p, Injuries.Treatment.SPLINT, FeatureSetting.SPLINT_SECONDS));
	public static final Item CAST = register("cast",
			p -> new TreatmentItem(p, Injuries.Treatment.CAST, FeatureSetting.CAST_SECONDS));

	private ModItems() {
	}

	private static Identifier id(String name) {
		return Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, name);
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
	}

	public static void register() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(output -> {
			output.accept(CANTEEN);
			output.accept(WATER_BOWL);
		});
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, id("medicine"), FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.hardmodeplus.medicine"))
				.icon(() -> new ItemStack(BANDAGE))
				.displayItems((parameters, output) -> {
					output.accept(BANDAGE);
					output.accept(HERBAL_BANDAGE);
					output.accept(TOURNIQUET);
					output.accept(SPLINT);
					output.accept(CAST);
				})
				.build());
	}
}
