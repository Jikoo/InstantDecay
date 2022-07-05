package digi.instantDecay;

import com.github.jikoo.instantdecay.InstantDecayCommand;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class InstantDecay extends JavaPlugin implements Listener {

	private final Random rand = new Random();
	private boolean enabled = true;

	@Override
	public void onEnable() {
		var command = getCommand("instantdecay");
		if (command != null) {
			command.setExecutor(new InstantDecayCommand(this, () -> enabled, enabled -> this.enabled = enabled));
		}

		if (enabled) {
			getServer().getPluginManager().registerEvents(this, this);
		}
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

		if (!validChunk(world, x - off, y - off, z - off, x + off, y + off, z + off)) {
			return;
		}

		getServer().getScheduler().runTaskLater(this, () -> decayLeavesAround(world, x, y, z, range), 1L);
	}

	public void decayLeavesAround(@NotNull World world, int x, int y, int z, int range) {
		for (int offX = -range; offX <= range; offX++) {
			for (int offY = -range; offY <= range; offY++) {
				for (int offZ = -range; offZ <= range; offZ++) {
					Block blockLeaves = world.getBlockAt(x + offX, y + offY, z + offZ);
					BlockData data = blockLeaves.getBlockData();
					if (!(data instanceof Leaves leaves)) {
						continue;
					}
					if (leaves.isPersistent() || leaves.getDistance() < 7) {
						continue;
					}
					LeavesDecayEvent event = new LeavesDecayEvent(blockLeaves);
					getServer().getPluginManager().callEvent(event);

					if (event.isCancelled()) {
						return;
					}

					blockLeaves.breakNaturally();

					if (rand.nextInt(10) == 0) {
						world.playEffect(blockLeaves.getLocation(), Effect.STEP_SOUND, Material.OAK_LEAVES);
					}
				}
			}
		}
	}

	private boolean validChunk(@NotNull World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
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
