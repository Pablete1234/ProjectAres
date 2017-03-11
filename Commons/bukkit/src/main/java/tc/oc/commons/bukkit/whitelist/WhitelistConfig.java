package tc.oc.commons.bukkit.whitelist;

public interface WhitelistConfig {
    
    /**
     * If whitelist should reset on restart, if false, the whitelist will be loaded from the vanilla one
     */
    boolean resetOnRestart();
    
}
