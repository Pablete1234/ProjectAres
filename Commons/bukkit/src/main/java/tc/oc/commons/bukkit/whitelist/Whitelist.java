package tc.oc.commons.bukkit.whitelist;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ForwardingSet;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import tc.oc.api.bukkit.users.BukkitUserStore;
import tc.oc.api.bukkit.users.OnlinePlayers;
import tc.oc.api.docs.PlayerId;
import tc.oc.api.docs.Server;
import tc.oc.api.docs.UserId;
import tc.oc.api.minecraft.users.LocalUserDocument;
import tc.oc.commons.bukkit.chat.ComponentRenderContext;
import tc.oc.commons.bukkit.event.UserLoginEvent;
import tc.oc.commons.bukkit.event.WhitelistStateChangeEvent;
import tc.oc.commons.core.plugin.PluginFacet;

@Singleton
public class Whitelist extends ForwardingSet<PlayerId> implements PluginFacet, Listener {
    public static final String EDIT_PERM = "whitelist.edit";
    public static final String BYPASS_PERM = "whitelist.bypass";

    private final Server localServer;
    private final BukkitUserStore userStore;
    private final OnlinePlayers onlinePlayers;
    private final ComponentRenderContext renderer;
    private final WhitelistConfig config;

    private boolean enabled;
    private final Set<PlayerId> whitelist = new HashSet<>();

    @Inject Whitelist(Server localServer, BukkitUserStore userStore, OnlinePlayers onlinePlayers, ComponentRenderContext renderer, WhitelistConfig config) {
        this.localServer = localServer;
        this.userStore = userStore;
        this.onlinePlayers = onlinePlayers;
        this.renderer = renderer;
        this.config = config;
    }

    @Override
    protected Set<PlayerId> delegate() {
        return whitelist;
    }

    @Override
    public void enable() {
        reset(config.resetOnRestart());
        setEnabled(localServer.whitelist_enabled());
        Bukkit.setWhitelist(false); // Let's handle the whitelist ourselves
    }
      
    public void reset(boolean clearVanilla) {
        if (clearVanilla) clear(); // Clear both our whitelist and the vanilla one
        else whitelist.clear();    // Clear only our whitelist
        addAll(Bukkit.getWhitelistedPlayers().stream().map(LocalUserDocument::new).collect(Collectors.toSet()));
        if(localServer.team() != null) {
            addAll(localServer.team().members());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isWhitelisted(Player player) {
        return !enabled ||
               player.isOp() ||
               player.hasPermission(BYPASS_PERM) ||
               whitelist.contains(userStore.getUser(player));
    }

    public void setEnabled(boolean yes) {
        enabled = yes;
        Bukkit.getServer().getPluginManager().callEvent(new WhitelistStateChangeEvent(enabled));
    }

    public int addAllOnline() {
        int count = 0;
        for(Player player : onlinePlayers.all()) {
            if(!player.hasPermission(BYPASS_PERM) && add(userStore.getUser(player))) {
                count++;
            }
        }
        return count;
    }

    public int kickAll() {
        int count = 0;
        for(Player player : onlinePlayers.all()) {
            if(!isWhitelisted(player)) {
                player.kickPlayer(renderer.renderLegacy(new TranslatableComponent("whitelist.kicked"), player));
                count++;
            }
        }
        return count;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLogin(UserLoginEvent event) {
        if(!isWhitelisted(event.getPlayer())) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, new TranslatableComponent("whitelist.kicked"));
        }
    }
    
    /*
     * Methods for adding/removing players to/from the whitelist, therse will also update the vanilla whitelist
     */
    @Override
    public boolean add(@Nonnull PlayerId playerId) {
        boolean added = super.add(playerId);
        if (added) {
            Bukkit.getOfflinePlayer(UUID.fromString(playerId.player_id())).setWhitelisted(true);
        }
        return added;
    }
    
    @Override
    public boolean remove(@Nonnull Object playerId) {
        boolean removed = super.remove(playerId);
        if (removed && playerId instanceof UserId) {
            Bukkit.getOfflinePlayer(UUID.fromString(((UserId) playerId).player_id())).setWhitelisted(false);
        }
        return removed;
    }
    
    @Override
    public boolean addAll(@Nonnull Collection<? extends PlayerId> playerIds) {
        return standardAddAll(playerIds);
    }
    
    @Override
    public boolean removeAll(@Nonnull Collection<?> playerIds) {
        return standardRemoveAll(playerIds);
    }
    
    @Override
    public void clear() {
        Bukkit.getWhitelistedPlayers().forEach(pl -> pl.setWhitelisted(false));
        super.clear();
    }
        
}
