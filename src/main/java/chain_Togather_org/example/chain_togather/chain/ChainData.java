package chain_Togather_org.example.chain_togather.chain;

import chain_Togather_org.example.chain_togather.ChainTogather;
import chain_Togather_org.example.chain_togather.Config;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ChainData extends SavedData {
    private static final SavedData.Factory<ChainData> FACTORY = new SavedData.Factory<>(ChainData::new, ChainData::load, null);

    private final List<ChainGroup> groups = new ArrayList<>();
    private final Map<UUID, ChainGroup> byMember = new HashMap<>();

    public enum LinkResult { LINKED, ALREADY_LINKED, NOT_LINE_END, WOULD_CLOSE_LOOP }

    public static ChainData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, ChainTogather.MODID);
    }

    public List<ChainGroup> groups() {
        return Collections.unmodifiableList(groups);
    }

    @Nullable
    public ChainGroup groupOf(UUID player) {
        return byMember.get(player);
    }

    public List<ChainLink> allLinks() {
        List<ChainLink> result = new ArrayList<>();
        groups.forEach(group -> result.addAll(group.links));
        return result;
    }

    public LinkResult link(ServerPlayer self, ServerPlayer target) {
        ChainGroup selfGroup = byMember.get(self.getUUID());
        ChainGroup targetGroup = byMember.get(target.getUUID());
        ChainLink link = ChainLink.of(self.getUUID(), target.getUUID());
        boolean sameGroup = selfGroup != null && selfGroup == targetGroup;

        if (sameGroup && selfGroup.links.contains(link)) return LinkResult.ALREADY_LINKED;
        LinkResult blocked = checkLineRule(sameGroup, self, target, selfGroup, targetGroup);
        if (blocked != null) return blocked;

        if (sameGroup) {
            selfGroup.links.add(link);
            setDirty();
            return LinkResult.LINKED;
        }

        if (selfGroup != null) SharedInventory.sync(selfGroup, selfGroup.livingPlayers(self.server));
        if (targetGroup != null) SharedInventory.sync(targetGroup, targetGroup.livingPlayers(self.server));

        List<ItemStack> overflow = new ArrayList<>();
        ServerPlayer overflowOwner = selfGroup == null && targetGroup != null ? self : target;
        ChainGroup group = joinGroups(self, target, selfGroup, targetGroup, overflow);
        group.links.add(link);

        for (ServerPlayer member : group.livingPlayers(self.server)) SharedInventory.applyTo(group, member);
        for (ItemStack stack : overflow) overflowOwner.spawnAtLocation(stack);
        setDirty();
        return LinkResult.LINKED;
    }

    @Nullable
    private static LinkResult checkLineRule(boolean sameGroup, ServerPlayer self, ServerPlayer target,
                                            @Nullable ChainGroup selfGroup, @Nullable ChainGroup targetGroup) {
        if (Config.getChainMode() != ChainMode.LINE) return null;
        if (sameGroup) return LinkResult.WOULD_CLOSE_LOOP;
        if (linkCount(selfGroup, self) >= 2 || linkCount(targetGroup, target) >= 2) return LinkResult.NOT_LINE_END;
        return null;
    }

    private static int linkCount(@Nullable ChainGroup group, ServerPlayer player) {
        return group == null ? 0 : group.linkCount(player.getUUID());
    }

    private ChainGroup joinGroups(ServerPlayer self, ServerPlayer target, @Nullable ChainGroup selfGroup, @Nullable ChainGroup targetGroup,
                                  List<ItemStack> overflow) {
        if (selfGroup == null && targetGroup == null) return createGroup(self, target, overflow);
        if (targetGroup == null) {
            addTarget(selfGroup, target, overflow);
            return selfGroup;
        }
        if (selfGroup == null) {
            mergeItems(targetGroup, self.getInventory().items, overflow);
            addMember(targetGroup, self.getUUID());
            return targetGroup;
        }
        absorbGroup(selfGroup, targetGroup, overflow);
        return selfGroup;
    }

    private ChainGroup createGroup(ServerPlayer self, ServerPlayer target, List<ItemStack> overflow) {
        ChainGroup group = new ChainGroup();
        for (int slot = 0; slot < ChainGroup.SHARED_SLOTS; slot++) group.items.set(slot, self.getInventory().items.get(slot).copy());
        addMember(group, self.getUUID());
        addTarget(group, target, overflow);
        groups.add(group);
        return group;
    }

    private void addTarget(ChainGroup group, ServerPlayer target, List<ItemStack> overflow) {
        mergeItems(group, target.getInventory().items, overflow);
        addMember(group, target.getUUID());
    }

    private void absorbGroup(ChainGroup group, ChainGroup other, List<ItemStack> overflow) {
        mergeItems(group, other.items, overflow);
        group.durability = Math.min(group.durability, other.durability);
        group.countedAdvancements.addAll(other.countedAdvancements);
        group.links.addAll(other.links);
        for (UUID member : other.members) addMember(group, member);
        groups.remove(other);
    }

    public void removeMember(UUID member) {
        ChainGroup group = byMember.remove(member);
        if (group == null) return;
        group.members.remove(member);
        group.links.removeIf(link -> link.a().equals(member) || link.b().equals(member));
        if (group.links.isEmpty()) dissolve(group);
        setDirty();
    }

    public boolean removeNonPlayers(MinecraftServer server) {
        List<UUID> nonPlayers = new ArrayList<>();
        for (ChainGroup group : groups) {
            for (LivingEntity member : group.livingMembers(server)) {
                if (!(member instanceof Player)) nonPlayers.add(member.getUUID());
            }
        }
        nonPlayers.forEach(this::removeMember);
        return !nonPlayers.isEmpty();
    }

    public void dissolve(ChainGroup group) {
        groups.remove(group);
        group.members.forEach(byMember::remove);
        setDirty();
    }

    public boolean damage(ChainGroup group, double amount) {
        group.durability = Math.max(0.0, group.durability - amount);
        setDirty();
        return group.durability <= 0.0;
    }

    private void addMember(ChainGroup group, UUID member) {
        group.members.add(member);
        byMember.put(member, group);
    }

    private static void mergeItems(ChainGroup group, List<ItemStack> incoming, List<ItemStack> overflow) {
        for (ItemStack stack : incoming) {
            if (stack.isEmpty()) continue;
            ItemStack left = SharedInventory.insert(group.items, stack);
            if (!left.isEmpty()) overflow.add(left);
        }
    }

    @Override
    @NotNull
    public CompoundTag save(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        groups.forEach(group -> list.add(group.save(provider)));
        tag.put("Groups", list);
        return tag;
    }

    private static ChainData load(CompoundTag tag, HolderLookup.Provider provider) {
        ChainData data = new ChainData();
        for (Tag groupTag : tag.getList("Groups", Tag.TAG_COMPOUND)) {
            ChainGroup group = ChainGroup.load((CompoundTag) groupTag, provider);
            data.groups.add(group);
            group.members.forEach(member -> data.byMember.put(member, group));
        }
        return data;
    }
}
