package digi.instantDecay;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class InstantDecay extends JavaPlugin implements Listener {

	private final Random rand = new Random();
	private boolean disabled = false;

	@Override
	public void onEnable() {
		if (!disabled) {
			getServer().getPluginManager().registerEvents(this, this);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0) {

			if (args[0].equalsIgnoreCase("disable")) {
				if (!disabled) {
					HandlerList.unregisterAll((JavaPlugin) this);
					disabled = true;
				}
				sender.sendMessage("InstantDecay is disabled! Use /instantdecay enable to re-enable.");
				return true;
			}

			if (args[0].equalsIgnoreCase("enable")) {
				if (disabled) {
					getServer().getPluginManager().registerEvents(this, this);
					disabled = false;
				}
				sender.sendMessage("InstantDecay is enabled! Use /instantdecay disable to disable.");
				return true;
			}
		}

		sender.sendMessage(ChatColor.GRAY + "Available subcommands:");
		sender.sendMessage(ChatColor.WHITE + "/instantdecay disable" + ChatColor.GRAY + " - turns the plugin off");
		sender.sendMessage(ChatColor.WHITE + "/instantdecay enable" + ChatColor.GRAY + " - turns the plugin on");

		return true;
	}

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {

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

		new BukkitRunnable() {
			public void run() {
				for (int offX = -range; offX <= range; offX++) {
					for (int offY = -range; offY <= range; offY++) {
						for (int offZ = -range; offZ <= range; offZ++) {
							Block blockLeaves = world.getBlockAt(x + offX, y + offY, z + offZ);
							BlockData data = blockLeaves.getBlockData();
							if (!(data instanceof Leaves)) {
								continue;
							}
							Leaves leaves = (Leaves) data;
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
		}.runTask(this);
	}

	private boolean validChunk(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
		if (maxY >= 0 && minY < world.getMaxHeight()) {
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

		return false;
	}
}
