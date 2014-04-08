package digi.instantDecay;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class InstantDecay extends JavaPlugin implements Listener {
	private final Random rand = new Random();
	private static boolean disabled = false;

	@Override
	public void onEnable() {
		if (!disabled) {
			getServer().getPluginManager().registerEvents(this, this);
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.testPermissionSilent(sender)) {
			sender.sendMessage(ChatColor.RED + "You don't have access to this command.");
			return true;
		}

		if (command.getLabel().equals("instantdecay")) {
			if (args.length > 0) {
				if (args[0].equalsIgnoreCase("disable")) {
					if (!disabled) {
						HandlerList.unregisterAll((Plugin) this);
						disabled = true;

						sender.sendMessage("Plugin has been disabled ! Use /instantdecay enable to enable it back.");
					} else {
						sender.sendMessage("Plugin not enabled.");
					}

					return true;
				} else if (args[0].equalsIgnoreCase("enable")) {
					if (disabled) {
						getServer().getPluginManager().registerEvents(this, this);
						disabled = false;

						sender.sendMessage("Plugin has been enabled !");
					} else {
						sender.sendMessage("Plugin not disabled.");
					}

					return true;
				}
			}

			sender.sendMessage("Available subcommands:");
			sender.sendMessage(ChatColor.GRAY + "/instantdecay " + ChatColor.WHITE + "disable" + ChatColor.GRAY + " - turns the plugin off");
			sender.sendMessage(ChatColor.GRAY + "/instantdecay " + ChatColor.WHITE + "enable" + ChatColor.GRAY + " - re-enables the plugin");
		}

		return true;
	}

	@EventHandler
	public void blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();

		if (block.getType() != Material.LOG && block.getType() != Material.LOG_2) {
			return;
		}

		Location loc = block.getLocation();
		final World world = loc.getWorld();
		final int x = loc.getBlockX();
		final int y = loc.getBlockY();
		final int z = loc.getBlockZ();
		final int range = 4;
		final int off = range + 1;

		if (!validChunk(world, x - off, y - off, z - off, x + off, y + off, z + off)) {
			return;
		}

		getServer().getScheduler().runTask(this, new Runnable() {
			@Override
			public void run() {
				for (int offX = -range; offX <= range; offX++) {
					for (int offY = -range; offY <= range; offY++) {
						for (int offZ = -range; offZ <= range; offZ++) {
							Material m = world.getBlockAt(x + offX, y + offY, z + offZ).getType();
							if (m == Material.LEAVES || m == Material.LEAVES_2) {
								breakLeaf(world, x + offX, y + offY, z + offZ);
							}
						}
					}
				}
			}
		});
	}

	private void breakLeaf(World world, int x, int y, int z) {
		Block block = world.getBlockAt(x, y, z);
		@SuppressWarnings("deprecation")
		byte data = block.getData();

		if ((data & 4) == 4) {
			return; // player placed leaf, ignore
		}

		byte range = 4;
		byte max = 32;
		int[] blocks = new int[max * max * max];
		int off = range + 1;
		int mul = max * max;
		int div = max / 2;

		if (validChunk(world, x - off, y - off, z - off, x + off, y + off, z + off)) {
			int offX;
			int offY;
			int offZ;
			Material type;

			for (offX = -range; offX <= range; offX++) {
				for (offY = -range; offY <= range; offY++) {
					for (offZ = -range; offZ <= range; offZ++) {
						type = world.getBlockAt(x + offX, y + offY, z + offZ).getType();
						blocks[(offX + div) * mul + (offY + div) * max + offZ + div] = (type == Material.LOG || type == Material.LOG_2 ? 0 : (type == Material.LEAVES || type == Material.LEAVES_2 ? -2 : -1));
					}
				}
			}

			int type1;
			for (offX = 1; offX <= 4; offX++) {
				for (offY = -range; offY <= range; offY++) {
					for (offZ = -range; offZ <= range; offZ++) {
						for (type1 = -range; type1 <= range; type1++) {
							if (blocks[(offY + div) * mul + (offZ + div) * max + type1 + div] == offX - 1) {
								if (blocks[(offY + div - 1) * mul + (offZ + div) * max + type1 + div] == -2)
									blocks[(offY + div - 1) * mul + (offZ + div) * max + type1 + div] = offX;

								if (blocks[(offY + div + 1) * mul + (offZ + div) * max + type1 + div] == -2)
									blocks[(offY + div + 1) * mul + (offZ + div) * max + type1 + div] = offX;

								if (blocks[(offY + div) * mul + (offZ + div - 1) * max + type1 + div] == -2)
									blocks[(offY + div) * mul + (offZ + div - 1) * max + type1 + div] = offX;

								if (blocks[(offY + div) * mul + (offZ + div + 1) * max + type1 + div] == -2)
									blocks[(offY + div) * mul + (offZ + div + 1) * max + type1 + div] = offX;

								if (blocks[(offY + div) * mul + (offZ + div) * max + (type1 + div - 1)] == -2)
									blocks[(offY + div) * mul + (offZ + div) * max + (type1 + div - 1)] = offX;

								if (blocks[(offY + div) * mul + (offZ + div) * max + type1 + div + 1] == -2)
									blocks[(offY + div) * mul + (offZ + div) * max + type1 + div + 1] = offX;
							}
						}
					}
				}
			}
		}

		if (blocks[div * mul + div * max + div] < 0) {
			LeavesDecayEvent event = new LeavesDecayEvent(block);
			getServer().getPluginManager().callEvent(event);

			if (event.isCancelled()) {
				return;
			}

			block.breakNaturally();

			if (10 > rand.nextInt(100)) {
				world.playEffect(block.getLocation(), Effect.STEP_SOUND, Material.LEAVES);
			}
		}
	}

	public boolean validChunk(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
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
