package tc.oc.pgm.kits;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Physical;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;
import tc.oc.commons.bukkit.util.NMSHacks;

public class MapItemFactory extends StaticItemFactory {

    // Shared across MapItemFactory so they don't create maps with the same id
    private static int maxId = 1000;
    // The id of the last map created, will be reused if possible, so all players get the same map
    private short id;
    // The UUID of the world the last map belongs to, used to know if a new map should be generated
    private UUID lastWorld = null;
    @Inspect protected final int x, z;
    @Inspect protected final MapView.Scale scale;

    public MapItemFactory(ItemStack item, int x, int z, MapView.Scale scale) {
        super(item);
        this.x = x;
        this.z = z;
        this.scale = scale;
    }

    @Override
    public ItemStack createItem(Physical context) {
        if (lastWorld == null || context.getWorld().getUID() != lastWorld) {
            // Either this is the first time creating the item, or the world changed, so create a new map
            MapView view = Bukkit.getServer().createMap(context.getWorld(), maxId++);
            view.setScale(scale);
            view.setCenterX(x);
            view.setCenterZ(z);
            // Set that this id and world are the currently used ones
            this.id = view.getId();
            this.lastWorld = context.getWorld().getUID();
        }
        ItemStack item = super.createItem(context).clone();
        item.setDurability(id);
        return item;
    }

}
