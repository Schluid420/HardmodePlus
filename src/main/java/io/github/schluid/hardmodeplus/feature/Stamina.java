package io.github.schluid.hardmodeplus.feature;

import com.mojang.serialization.Codec;
import io.github.schluid.hardmodeplus.HardmodePlus;
import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

// Stamina bar: sprinting and actions use it, walking and standing refill it
public final class Stamina {
	public static final AttachmentType<Integer> STAMINA = AttachmentRegistry.<Integer>builder()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "stamina"));

	public static final AttachmentType<Integer> HIGHEST_LEVEL = AttachmentRegistry.<Integer>builder()
			.persistent(Codec.INT)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.buildAndRegister(Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "highest_level"));

	private static final Identifier MINE_ID = Identifier.fromNamespaceAndPath(HardmodePlus.MOD_ID, "exhaustion_mining");
	private static final double LAST_PART = 1.0 / 3.0;
	private static final float STONE = 1.5F;
	private static final double STANDING = 0.0004;

	private static final Map<UUID, double[]> STATE = new HashMap<>();

	private Stamina() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			STATE.clear();
		});
		// "Only with enough stamina": an action that costs more than is left doesn't happen
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) ->
				canAfford(player, breakCost(player, level.getBlockState(pos), level, pos))
						? InteractionResult.PASS : InteractionResult.FAIL);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) ->
				canAfford(player, attackCost(player.getAttackStrengthScale(0.5F)))
						? InteractionResult.PASS : InteractionResult.FAIL);
		UseBlockCallback.EVENT.register((player, level, hand, hit) ->
				player.getItemInHand(hand).getItem() instanceof BlockItem
						&& !canAfford(player, HardmodeConfig.get(FeatureSetting.EXHAUST_PLACE))
						? InteractionResult.FAIL : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
				canAfford(player, breakCost(player, state, level, pos)));
	}

	public static boolean exhausted(Player player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		return speed != null && speed.getModifier(Features.EXHAUST_ID) != null;
	}

	// Enough stamina left for an action of this cost (percent of the bar), works on both sides
	public static boolean canAfford(Player player, double percent) {
		if (!HardmodeConfig.isOn(FeatureSetting.EXHAUST) || !HardmodeConfig.isOn(FeatureSetting.EXHAUST_ACTIONS)
				|| !HardmodeConfig.isOn(FeatureSetting.EXHAUST_ENOUGH) || player.isCreative() || player.isSpectator()
				|| percent <= 0) {
			return true;
		}
		double part = get(player) / 100.0;
		return part * 100 >= percent * useFactor(part);
	}

	public static double jumpCost() {
		return HardmodeConfig.get(FeatureSetting.EXHAUST_JUMPING);
	}

	private static double attackCost(float charge) {
		return HardmodeConfig.get(FeatureSetting.EXHAUST_ATTACK) * Math.max(0.2, charge);
	}

	private static double breakCost(Player player, BlockState state, BlockGetter level, BlockPos pos) {
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness <= 0) {
			return 0;
		}
		return HardmodeConfig.get(FeatureSetting.EXHAUST_BREAK) * Math.min(3, hardness / STONE)
				* toolFactor(player.getMainHandItem().getDestroySpeed(state));
	}

	public static int get(Player player) {
		Integer value = ((AttachmentTarget) player).getAttached(STAMINA);
		return value != null ? value : 100;
	}

	public static int highestLevel(Player player) {
		Integer value = ((AttachmentTarget) player).getAttached(HIGHEST_LEVEL);
		return value != null ? value : 0;
	}

	static void tick(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			update(player);
		}
	}

	private static void update(ServerPlayer player) {
		UUID id = player.getUUID();
		rememberLevel(player);
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (!HardmodeConfig.isOn(FeatureSetting.EXHAUST) || player.isCreative() || player.isSpectator()
				|| !player.isAlive()) {
			if (STATE.remove(id) != null) {
				((AttachmentTarget) player).removeAttached(STAMINA);
			}
			removeModifier(speed);
			removeMining(player);
			return;
		}
		double max = maxTicks(player);
		int jumps = player.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP));
		double[] state = STATE.computeIfAbsent(id, key -> new double[] {max, 0, jumps, player.getX(), player.getZ(), 0, 0});
		int newJumps = (int) (jumps - state[2]);
		double dx = player.getX() - state[3];
		double dz = player.getZ() - state[4];
		state[2] = jumps;
		state[3] = player.getX();
		state[4] = player.getZ();

		if (!player.isSprinting() && newJumps > 0) {
			action(player, HardmodeConfig.get(FeatureSetting.EXHAUST_JUMPING) * newJumps);
		}
		if (player.isSprinting() && state[1] <= 0) {
			double use = 1 + Math.max(0, newJumps) * HardmodeConfig.get(FeatureSetting.EXHAUST_JUMP) * 20;
			state[0] -= use * useFactor(state[0] / max);
		} else if (state[6] > 0) {
			// Right after mining, building or a hit there is no recovery
			state[6]--;
		} else {
			boolean standing = dx * dx + dz * dz < STANDING;
			double seconds = HardmodeConfig.get(standing ? FeatureSetting.EXHAUST_STAND : FeatureSetting.EXHAUST_WALK);
			double regen = max / Math.max(20, seconds * 20);
			if (Features.tired(player)) {
				regen *= 1 - HardmodeConfig.get(FeatureSetting.TIRED_REGEN) / 100.0;
			}
			// Low thirst slows the recovery too
			regen *= Thirst.staminaFactor(player);
			state[0] += regen;
		}
		state[0] = Math.max(0, Math.min(max, state[0]));

		// Exhausted until the bar is back to the set share of the maximum
		if (state[1] > 0) {
			if (state[0] >= max * HardmodeConfig.get(FeatureSetting.EXHAUST_REST) / 100.0) {
				state[1] = 0;
			}
		} else if (state[0] <= 0) {
			state[1] = 1;
			state[5] = Math.round(HardmodeConfig.get(FeatureSetting.EXHAUST_MINE_SECONDS) * 20);
			Thirst.exhausted(player);
		}
		updateMining(player, state);

		if (speed != null) {
			if (state[1] > 0) {
				double slow = HardmodeConfig.get(FeatureSetting.EXHAUST_SLOW) / 100.0;
				AttributeModifier current = speed.getModifier(Features.EXHAUST_ID);
				if (current == null || Math.abs(current.amount() + slow) > 1.0E-4) {
					speed.addOrUpdateTransientModifier(new AttributeModifier(Features.EXHAUST_ID, -slow,
							AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				}
			} else {
				removeModifier(speed);
			}
		}

		int percent = (int) Math.round(state[0] / max * 100);
		if (percent != get(player)) {
			((AttachmentTarget) player).setAttached(STAMINA, percent);
		}
	}

	private static double maxTicks(ServerPlayer player) {
		if (!HardmodeConfig.isOn(FeatureSetting.EXHAUST_LEVELS)) {
			return Math.max(20, HardmodeConfig.get(FeatureSetting.EXHAUST_SECONDS) * 20);
		}
		int highest = rememberLevel(player);
		double seconds = HardmodeConfig.get(FeatureSetting.EXHAUST_LEVEL_BASE)
				+ highest * HardmodeConfig.get(FeatureSetting.EXHAUST_LEVEL_STEP);
		seconds = Math.min(seconds, Math.max(HardmodeConfig.get(FeatureSetting.EXHAUST_LEVEL_BASE),
				HardmodeConfig.get(FeatureSetting.EXHAUST_LEVEL_MAX)));
		return Math.max(20, seconds * 20);
	}

	private static int rememberLevel(ServerPlayer player) {
		Integer stored = ((AttachmentTarget) player).getAttached(HIGHEST_LEVEL);
		int highest = Math.max(stored != null ? stored : 0, player.experienceLevel);
		if (stored == null || highest > stored) {
			((AttachmentTarget) player).setAttached(HIGHEST_LEVEL, highest);
		}
		return highest;
	}

	public static void blockBroken(ServerPlayer player, float hardness, float toolSpeed) {
		if (hardness <= 0) {
			return;
		}
		double factor = Math.min(3, hardness / STONE) * toolFactor(toolSpeed);
		action(player, HardmodeConfig.get(FeatureSetting.EXHAUST_BREAK) * factor);
	}

	private static double toolFactor(float speed) {
		double share = Math.min(1, Math.max(0, (speed - 2) / 7.0));
		return 1 - share * HardmodeConfig.get(FeatureSetting.EXHAUST_TOOL) / 100.0;
	}

	public static void blockPlaced(ServerPlayer player) {
		action(player, HardmodeConfig.get(FeatureSetting.EXHAUST_PLACE));
	}

	public static void attacked(ServerPlayer player, float charge) {
		action(player, attackCost(charge));
	}

	private static void action(ServerPlayer player, double percent) {
		if (!HardmodeConfig.isOn(FeatureSetting.EXHAUST) || !HardmodeConfig.isOn(FeatureSetting.EXHAUST_ACTIONS)
				|| player.isCreative() || player.isSpectator() || percent <= 0) {
			return;
		}
		double[] state = STATE.get(player.getUUID());
		if (state == null) {
			return;
		}
		double max = maxTicks(player);
		state[0] = Math.max(0, state[0] - max * percent / 100.0 * useFactor(state[0] / max));
		state[6] = Math.round(HardmodeConfig.get(FeatureSetting.EXHAUST_PAUSE) * 20);
	}

	private static double useFactor(double part) {
		if (part >= LAST_PART) {
			return 1;
		}
		double low = HardmodeConfig.get(FeatureSetting.EXHAUST_LOW) / 100.0;
		double depth = 1 - part / LAST_PART;
		if ((int) HardmodeConfig.get(FeatureSetting.EXHAUST_CURVE) == 1) {
			depth *= depth;
		}
		return 1 - (1 - low) * depth;
	}

	private static void updateMining(ServerPlayer player, double[] state) {
		AttributeInstance mining = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
		if (mining == null) {
			return;
		}
		double slow = HardmodeConfig.get(FeatureSetting.EXHAUST_MINE) / 100.0;
		if (state[5] > 0 && slow > 0) {
			state[5]--;
			if (mining.getModifier(MINE_ID) == null) {
				mining.addOrUpdateTransientModifier(new AttributeModifier(MINE_ID, -slow,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			}
		} else {
			state[5] = 0;
			removeMining(player);
		}
	}

	private static void removeMining(ServerPlayer player) {
		AttributeInstance mining = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
		if (mining != null && mining.getModifier(MINE_ID) != null) {
			mining.removeModifier(MINE_ID);
		}
	}

	private static void removeModifier(AttributeInstance speed) {
		if (speed != null && speed.getModifier(Features.EXHAUST_ID) != null) {
			speed.removeModifier(Features.EXHAUST_ID);
		}
	}
}
