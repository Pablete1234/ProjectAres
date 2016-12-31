package tc.oc.pgm.kits;

import javax.inject.Inject;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import tc.oc.pgm.itemmeta.ItemModifier;
import tc.oc.pgm.match.MatchPlayer;
import tc.oc.pgm.match.MatchPlayerFacet;
import tc.oc.pgm.match.MatchScope;

/**
 * This used to do more, but currently just calls Kit.apply.
 *
 * It may have more uses in the future, so we keep it around.
 */
public class KitPlayerFacet implements MatchPlayerFacet, Listener {

    @Inject private MatchPlayer player;
    @Inject private ItemModifier itemModifier;

    public void applyKit(Kit kit) {
        applyKit(kit, false);
    }

    public void applyKit(Kit kit, boolean force) {
        final ItemKitApplicator items = new ItemKitApplicator();
        kit.apply(player, force, items);
        items.apply(player, itemModifier);

        /**
         * When max health is lowered by an item attribute or potion effect, the client can
         * go into an inconsistent state that has strange effects, like the death animation
         * playing when the player isn't dead. It is probably related to this bug:
         *
         * https://bugs.mojang.com/browse/MC-19690
         *
         * This appears to fix the client state, for reasons that are unclear. The one tick
         * delay is necessary. Any less and getMaxHealth will not reflect whatever was
         * applied in the kit to modify it.
         */
        final Player bukkit = player.getBukkit();
        player.getMatch().getScheduler(MatchScope.LOADED).createDelayedTask(1, () -> {
            if(bukkit.isOnline() && !player.isDead() && bukkit.getMaxHealth() < 20) {
                bukkit.setHealth(Math.min(bukkit.getHealth(), bukkit.getMaxHealth()));
            }
        });
    }
}
