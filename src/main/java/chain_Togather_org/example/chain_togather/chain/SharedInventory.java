package chain_Togather_org.example.chain_togather.chain;

import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SharedInventory {
    private SharedInventory() {
    }

    public static boolean sync(ChainGroup group, List<ServerPlayer> players) {
        if (players.isEmpty()) return false;
        boolean changed = false;

        for (int slot = 0; slot < ChainGroup.SHARED_SLOTS; slot++) {
            ItemStack base = group.items.get(slot);
            ItemStack replacement = null;
            int countDelta = 0;

            for (ServerPlayer player : players) {
                ItemStack current = player.getInventory().items.get(slot);
                if (ItemStack.matches(current, base)) continue;

                if (!base.isEmpty() && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, base))) {
                    countDelta += current.getCount() - base.getCount();
                } else if (replacement == null) {
                    replacement = current;
                }
            }

            ItemStack result;
            if (replacement != null) {
                result = replacement.copy();
            } else if (countDelta != 0) {
                int count = Math.min(base.getCount() + countDelta, base.getMaxStackSize());
                result = count > 0 ? base.copyWithCount(count) : ItemStack.EMPTY;
            } else {
                continue;
            }

            group.items.set(slot, result);
            changed = true;
            for (ServerPlayer player : players) {
                if (!ItemStack.matches(player.getInventory().items.get(slot), result)) player.getInventory().setItem(slot, result.copy());
            }
        }
        return changed;
    }

    public static void applyTo(ChainGroup group, ServerPlayer player) {
        for (int slot = 0; slot < ChainGroup.SHARED_SLOTS; slot++) {
            ItemStack shared = group.items.get(slot);
            if (!ItemStack.matches(player.getInventory().items.get(slot), shared)) player.getInventory().setItem(slot, shared.copy());
        }
    }

    public static ItemStack insert(NonNullList<ItemStack> items, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (ItemStack existing : items) {
            if (remaining.isEmpty()) return ItemStack.EMPTY;
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)) {
                int moved = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                if (moved > 0) {
                    existing.grow(moved);
                    remaining.shrink(moved);
                }
            }
        }
        for (int slot = 0; slot < items.size() && !remaining.isEmpty(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, remaining);
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }
}
