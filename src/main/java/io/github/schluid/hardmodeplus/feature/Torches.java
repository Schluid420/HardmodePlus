package io.github.schluid.hardmodeplus.feature;

import io.github.schluid.hardmodeplus.config.FeatureSetting;
import io.github.schluid.hardmodeplus.config.HardmodeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class Torches {
	private Torches() {
	}

	private static boolean isTorch(BlockState state) {
		return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
	}

	public static void placed(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!HardmodeConfig.isOn(FeatureSetting.TORCHES) || !isTorch(state)) {
			return;
		}
		int ticks = (int) Math.max(20, Math.round(HardmodeConfig.get(FeatureSetting.TORCH_MINUTES) * 1200));
		level.scheduleTick(pos, state.getBlock(), ticks);
	}

	public static boolean burnOut(BlockState state, ServerLevel level, BlockPos pos) {
		if (!isTorch(state) || !HardmodeConfig.isOn(FeatureSetting.TORCHES)) {
			return false;
		}
		level.removeBlock(pos, false);
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.2F);
		Block.popResource(level, pos, new ItemStack(Items.STICK));
		return true;
	}
}
