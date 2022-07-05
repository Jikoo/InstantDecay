package digi.instantDecay;

import com.github.jikoo.instantdecay.DecayListener;
import com.github.jikoo.instantdecay.InstantDecayCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class InstantDecay extends JavaPlugin {

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
