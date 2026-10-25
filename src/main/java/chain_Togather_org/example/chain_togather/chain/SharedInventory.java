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
            ItemStack result = resolveSlot(group.items.get(slot), players, slot);
            if (result == null) continue;

            group.items.set(slot, result);
            changed = true;
            for (ServerPlayer player : players) setIfDifferent(player, slot, result);
        }
        return changed;
    }

    private static ItemStack resolveSlot(ItemStack base, List<ServerPlayer> players, int slot) {
        ItemStack replacement = null;
        int countDelta = 0;

        for (ServerPlayer player : players) {
            ItemStack current = player.getInventory().items.get(slot);
            if (ItemStack.matches(current, base)) continue;

            if (isCountChange(base, current)) {
                countDelta += current.getCount() - base.getCount();
            } else if (replacement == null) {
                replacement = current;
            }
        }

        if (replacement != null) return replacement.copy();
        if (countDelta == 0) return null;
        int count = Math.min(base.getCount() + countDelta, base.getMaxStackSize());
        return count > 0 ? base.copyWithCount(count) : ItemStack.EMPTY;
    }

    private static boolean isCountChange(ItemStack base, ItemStack current) {
        return !base.isEmpty() && (current.isEmpty() || ItemStack.isSameItemSameComponents(current, base));
    }

    private static void setIfDifferent(ServerPlayer player, int slot, ItemStack stack) {
        if (!ItemStack.matches(player.getInventory().items.get(slot), stack)) player.getInventory().setItem(slot, stack.copy());
    }

    public static void applyTo(ChainGroup group, ServerPlayer player) {
        for (int slot = 0; slot < ChainGroup.SHARED_SLOTS; slot++) setIfDifferent(player, slot, group.items.get(slot));
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
