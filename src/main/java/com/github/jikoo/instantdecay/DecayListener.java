package com.github.jikoo.instantdecay;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void blockBreakEvent(@NotNull BlockBreakEvent event) {
		Block block = event.getBlock();
		Material broken = block.getType();
		if (!Tag.LOGS.isTagged(broken) && !Tag.LEAVES.isTagged(broken)) {
			return;
		}

		plugin.getServer().getScheduler().runTaskLater(plugin, () -> decayLeavesAround(block), 10L);
	}

	private void decayLeavesAround(@NotNull Block block) {
		World world = block.getWorld();

		int range = 8;

		int minX = block.getX() - range;
		int maxX = block.getX() + range;
		int minZ = block.getZ() - range;
		int maxZ = block.getZ() + range;
		// Clamp Y to world bounds.
		int minY = Math.max(block.getY() - range, world.getMinHeight());
		int maxY = Math.min(block.getY() + range, world.getMaxHeight());

		int lastChunkX = minX >> 4;
		int highestChunkZ = minZ >> 4 - 1;

		for (int x = minX; x <= maxX; ++x) {
			int currentChunkX = x >> 4;
			// If the X change results in interaction with new chunks, reset max chunk Z.
			if (currentChunkX != lastChunkX) {
				lastChunkX = currentChunkX;
				highestChunkZ = minZ >> 4 - 1;
			}

			for (int z = minZ; z <= maxZ; ++z) {
				int currentChunkZ = z >> 4;
				// If the new chunk Z exceeds the old, this is a new chunk. Check that it is loaded.
				if (currentChunkZ > highestChunkZ) {
					highestChunkZ = currentChunkZ;

					// If chunk is unloaded, skip to next chunk.
					if (!world.isChunkLoaded(currentChunkX, currentChunkZ)) {
						// We add 15 instead of 16 for a full chunk because the loop will add another 1.
						// We convert back from current chunk to block coordinates in case minZ is not on the chunk border.
						z = currentChunkZ << 4 + 15;
						continue;
					}
				}

				for (int y = minY; y <= maxY; ++y) {
					// Handle decay for each block.
					decayLeaves(world.getBlockAt(x, y, z));
				}
			}
		}

	}

	private void decayLeaves(@NotNull Block block) {
		Material material = block.getType();
		BlockData data = block.getBlockData();

		if (!(data instanceof Leaves leaves) || isDecayPrevented(block, leaves) || !block.breakNaturally()) {
			return;
		}

		Location location = block.getLocation().add(0.5, 0.5, 0.5);
		block.getWorld().spawnParticle(Particle.BLOCK_CRACK, location, 3, data);

		if (random.nextInt(5) == 0) {
			block.getWorld().playSound(location, getSound(material), SoundCategory.BLOCKS, 1F, 1F);
		}
	}

	private boolean isDecayPrevented(@NotNull Block block, @NotNull Leaves leaves) {
		if (leaves.isPersistent() || leaves.getDistance() < 7) {
			return true;
		}

		LeavesDecayEvent event = new LeavesDecayEvent(block);
		plugin.getServer().getPluginManager().callEvent(event);

		return event.isCancelled();
	}

	private @NotNull String getSound(@NotNull Material material) {
		return material == Material.AZALEA_LEAVES || material == Material.FLOWERING_AZALEA_LEAVES
				? "block.azalea_leaves.break"
				: "block.grass.break";
	}

}
