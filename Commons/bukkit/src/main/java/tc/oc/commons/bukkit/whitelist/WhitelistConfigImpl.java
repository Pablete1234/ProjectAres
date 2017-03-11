package tc.oc.commons.bukkit.whitelist;

import com.google.inject.Inject;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

public class WhitelistConfigImpl implements WhitelistConfig {
    
    private ConfigurationSection config;
    
    @Inject WhitelistConfigImpl(Configuration config) {
        this.config = config.needSection("whitelist");
    }
    
    @Override
    public boolean resetOnRestart() {
        return config.getBoolean("reset-on-restart", false);
    }
}
