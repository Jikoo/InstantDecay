package com.github.jikoo.instantdecay;

import digi.instantDecay.InstantDecay;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InstantDecayCommand implements TabExecutor {

	private final List<String> subcommands = List.of("disable", "enable");
	private final @NotNull InstantDecay plugin;
	private final @NotNull Supplier<Boolean> isEnabled;
	private final @NotNull Consumer<Boolean> setEnabled;


	public InstantDecayCommand(
			@NotNull InstantDecay plugin,
			@NotNull Supplier<Boolean> isEnabled,
			@NotNull Consumer<Boolean> setEnabled) {
		this.plugin = plugin;
		this.isEnabled = isEnabled;
		this.setEnabled = setEnabled;
	}

	@Override
	public boolean onCommand(
			@NotNull CommandSender sender,
			@NotNull Command command,
			@NotNull String label,
			@NotNull String @NotNull [] args) {
		if (args.length > 0) {

			if (args[0].equalsIgnoreCase("disable")) {
				if (isEnabled.get()) {
					HandlerList.unregisterAll((Plugin) plugin);
					setEnabled.accept(false);
				}
				sender.sendMessage("InstantDecay is disabled! Use /instantdecay enable to re-enable.");
				return true;
			}

			if (args[0].equalsIgnoreCase("enable")) {
				if (!isEnabled.get()) {
					plugin.getServer().getPluginManager().registerEvents(plugin, plugin);
					setEnabled.accept(true);
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

	@Override
	public @Nullable List<String> onTabComplete(
			@NotNull CommandSender sender,
			@NotNull Command command,
			@NotNull String alias,
			@NotNull String[] args) {

		if (args.length > 1) {
			return List.of();
		}

		if (args.length == 0) {
			return subcommands;
		}

		String completing = args[0];

		List<String> completable = new ArrayList<>();
		for (String subcommand : subcommands) {
			if (StringUtil.startsWithIgnoreCase(subcommand, completing)) {
				completable.add(subcommand);
			}
		}

		return completable;
	}

}
