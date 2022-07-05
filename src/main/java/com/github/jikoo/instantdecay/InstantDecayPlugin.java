package com.github.jikoo.instantdecay;

import org.bukkit.plugin.java.JavaPlugin;

public class InstantDecayPlugin extends JavaPlugin {

	private boolean enabled = true;

	@Override
	public void onEnable() {
		var command = getCommand("instantdecay");
		if (command != null) {
			command.setExecutor(new InstantDecayCommand(this, () -> enabled, enabled -> this.enabled = enabled));
		}

		if (enabled) {
			getServer().getPluginManager().registerEvents(new DecayListener(this), this);
		}
	}

}
