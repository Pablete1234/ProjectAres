package tc.oc.commons.bukkit.whitelist;

import tc.oc.commons.core.inject.HybridManifest;
import tc.oc.commons.core.plugin.PluginFacetBinder;

public class WhitelistManifest extends HybridManifest {
    
    @Override
    public void configure() {
        bind(WhitelistConfig.class).to(WhitelistConfigImpl.class);
        
        final PluginFacetBinder facets = new PluginFacetBinder(binder());
        facets.register(Whitelist.class);
        facets.register(WhitelistCommands.class);
        facets.register(WhitelistCommands.Parent.class);
    }
    
}
