package tc.oc.pgm.kits;

import org.bukkit.Physical;
import org.bukkit.inventory.ImItemStack;
import org.bukkit.inventory.ItemStack;
import tc.oc.commons.core.inspect.Inspectable;

public interface ItemFactory<T extends Physical> {

    ItemStack createItem(T context);

}

class StaticItemFactory extends Inspectable.Impl implements ItemFactory<Physical> {

    @Inspect
    protected final ImItemStack item;

    StaticItemFactory(ItemStack item) {
        this(item.immutableCopy());
    }

    StaticItemFactory(ImItemStack item) {
        this.item = item;
    }

    @Override
    public ItemStack createItem(Physical context) {
        return item;
    }

}
