package tc.oc.pgm.kits;

import org.bukkit.Physical;
import org.bukkit.inventory.ItemStack;
import tc.oc.pgm.match.MatchPlayer;

public class FreeItemKit extends Kit.Impl implements ItemKit {

    @Inspect(inline = true) protected final ItemFactory<Physical> itemFactory;

    public FreeItemKit(ItemStack item) {
        this(new StaticItemFactory(item));
    }

     public FreeItemKit(ItemFactory<Physical> itemFactory) {
        this.itemFactory = itemFactory;
    }

    @Override
    public ItemFactory itemFactory() {
        return itemFactory;
    }

    @Override
    public void apply(MatchPlayer player, boolean force, ItemKitApplicator items) {
        items.add(getItem(player));
    }

    protected ItemStack getItem(MatchPlayer player) {
        return itemFactory.createItem(player);
    }

}
