package com.github.jikoo.instantdecay;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class DecayListener implements Listener {


	private final Random random = new Random();
	private final @NotNull Plugin plugin;

	public DecayListener(@NotNull Plugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void blockBreakEvent(@NotNull BlockBreakEvent event) {

		if (!Tag.LOGS.isTagged(event.getBlock().getType()) && !Tag.LEAVES.isTagged(event.getBlock().getType())) {
			return;
		}

		Block block = event.getBlock();
		final World world = block.getWorld();
		final int x = block.getX();
		final int y = block.getY();
		final int z = block.getZ();
		final int range = 4;
		final int off = range + 1;

		if (!allChunksLoaded(world, x - off, y - off, z - off, x + off, y + off, z + off)) {
			return;
		}

		plugin.getServer().getScheduler().runTaskLater(
				plugin,
				() -> decayLeavesAround(world, x, y, z, range),
				1L);
	}

	private void decayLeavesAround(@NotNull World world, int x, int y, int z, int range) {
		for (int offX = -range; offX <= range; offX++) {
			for (int offZ = -range; offZ <= range; offZ++) {
				for (int offY = -range; offY <= range; offY++) {
					Block blockLeaves = world.getBlockAt(x + offX, y + offY, z + offZ);
					BlockData data = blockLeaves.getBlockData();
					if (!(data instanceof Leaves leaves)) {
						continue;
					}
					if (leaves.isPersistent() || leaves.getDistance() < 7) {
						continue;
					}
					LeavesDecayEvent event = new LeavesDecayEvent(blockLeaves);
					plugin.getServer().getPluginManager().callEvent(event);

					if (event.isCancelled()) {
						return;
					}

					blockLeaves.breakNaturally();

					if (random.nextInt(10) == 0) {
						world.playEffect(blockLeaves.getLocation(), Effect.STEP_SOUND, Material.OAK_LEAVES);
					}
				}
			}
		}
	}

	private boolean allChunksLoaded(
			@NotNull World world,
			int minX,
			int minY,
			int minZ,
			int maxX,
			int maxY,
			int maxZ) {
		if (maxY < world.getMinHeight() || minY > world.getMaxHeight()) {
			return false;
		}

		minX >>= 4;
		minZ >>= 4;
		maxX >>= 4;
		maxZ >>= 4;

		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!world.isChunkLoaded(x, z)) {
					return false;
				}
			}
		}

		return true;
	}

}
