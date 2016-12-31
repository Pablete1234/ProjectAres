package tc.oc.pgm.kits;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tc.oc.commons.bukkit.inventory.InventoryUtils;
import tc.oc.commons.bukkit.inventory.Slot;
import tc.oc.commons.bukkit.item.ItemUtils;
import tc.oc.pgm.itemmeta.ItemModifier;
import tc.oc.pgm.match.MatchPlayer;

public class ItemKitApplicator {

    private final Map<Slot, ItemStack> hardItems = new HashMap<>();
    private final Map<Slot, ItemStack> softItems = new HashMap<>();
    private final List<ItemStack> freeItems = new ArrayList<>();

    public void add(ItemStack item) {
        freeItems.add(item);
    }

    public void put(Slot slot, ItemStack item, boolean force) {
        (force ? hardItems : softItems).put(slot, item);
    }

    public boolean isEmpty(Slot slot) {
        return ItemUtils.isNothing(softItems.get(slot));
    }

    public void apply(MatchPlayer player, ItemModifier itemModifier) {
        final PlayerInventory inv = player.getInventory();

        // Place forced items first
        hardItems.forEach((slot, stack) -> player.giveItem(slot, itemModifier.modifyCopy(stack), false));

        final Map<Slot, ItemStack> softItems = new HashMap<>(Maps.transformValues(this.softItems, itemModifier::modifyCopy));
        final List<ItemStack> freeItems = new ArrayList<>(Lists.transform(this.freeItems, itemModifier::modifyCopy));
        final Iterable<ItemStack> kitItems = Iterables.concat(softItems.values(), freeItems);

        // Tools in the player's inv are repaired using matching tools in the kit with less damage
        for(ItemStack kitStack : kitItems) {
            for(ItemStack invStack : inv.contents()) {
                if(invStack != null) {
                    if(kitStack.getAmount() > 0 &&
                       kitStack.getType().getMaxDurability() > 0 &&
                       kitStack.getType().equals(invStack.getType()) &&
                       kitStack.getEnchantments().equals(invStack.getEnchantments()) &&
                       kitStack.getDurability() < invStack.getDurability()) {

                        invStack.setDurability(kitStack.getDurability());
                        kitStack.setAmount(0);
                        break;
                    }
                }
            }
        }

        // Items in the player's inv that stack with kit items are deducted from the kit
        for(ItemStack invStack : inv.contents()) {
            if(invStack != null) {
                int amount = invStack.getAmount();

                for(ItemStack kitStack : kitItems) {
                    if(amount <= 0) break;

                    if(kitStack.isSimilar(invStack)) {
                        int reduce = Math.min(amount, kitStack.getAmount());
                        if(reduce > 0) {
                            amount -= reduce;
                            ItemUtils.addAmount(kitStack, -reduce);
                        }
                    }
                }
            }
        }

        // Fill partial stacks of kit items that are already in the player's inv.
        // We must do this in a seperate pass so that kit stacks don't combine with
        // other kit stacks.
        for(ItemStack kitStack : kitItems) {
            InventoryUtils.similar(inv, kitStack).forEach(slot -> {
                final int quantity = slot.maxTransferrableIn(kitStack, inv);
                if(quantity > 0) {
                    player.giveItem(slot, kitStack, true);
                    ItemUtils.addAmount(kitStack, -quantity);
                }
            });
        }

        // Put the remaining kit slotted items into their designated inv slots.
        // If a slot is occupied, add the stack to freeItems.
        softItems.forEach((kitSlot, kitStack) -> {
            if(kitStack.getAmount() > 0) {
                if(kitSlot.isEmpty(inv)) {
                    player.giveItem(kitSlot, kitStack, false);
                } else {
                    freeItems.add(kitStack);
                }
            }
        });

        // Add free items to the inventory one at a time, firing an event
        // for each partial stack transferred.
        freeItems.forEach(player::giveItem);
    }

}
