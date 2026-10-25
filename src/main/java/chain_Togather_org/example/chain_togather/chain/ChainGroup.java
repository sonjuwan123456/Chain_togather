package chain_Togather_org.example.chain_togather.chain;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ChainGroup {
    public static final int SHARED_SLOTS = 36;

    final Set<UUID> members = new LinkedHashSet<>();
    final Set<ChainLink> links = new LinkedHashSet<>();
    final NonNullList<ItemStack> items = NonNullList.withSize(SHARED_SLOTS, ItemStack.EMPTY);
    final Set<ResourceLocation> countedAdvancements = new HashSet<>();
    double durability = 100.0;

    public double durability() {
        return durability;
    }

    int linkCount(UUID member) {
        int count = 0;
        for (ChainLink link : links) {
            if (link.a().equals(member) || link.b().equals(member)) count++;
        }
        return count;
    }

    public List<LivingEntity> livingMembers(MinecraftServer server) {
        List<LivingEntity> result = new ArrayList<>();
        for (UUID id : members) {
            LivingEntity member = find(server, id);
            if (member != null && member.isAlive() && !member.isSpectator()) result.add(member);
        }
        return result;
    }

    public List<ServerPlayer> livingPlayers(MinecraftServer server) {
        List<ServerPlayer> result = new ArrayList<>();
        for (LivingEntity member : livingMembers(server)) {
            if (member instanceof ServerPlayer player) result.add(player);
        }
        return result;
    }

    @Nullable
    public static LivingEntity find(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) return player;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living) return living;
        }
        return null;
    }

    CompoundTag save(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag memberList = new ListTag();
        members.forEach(id -> memberList.add(NbtUtils.createUUID(id)));
        tag.put("Members", memberList);

        ListTag linkList = new ListTag();
        for (ChainLink link : links) {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putUUID("A", link.a());
            linkTag.putUUID("B", link.b());
            linkList.add(linkTag);
        }
        tag.put("Links", linkList);

        ListTag advancementList = new ListTag();
        countedAdvancements.forEach(id -> advancementList.add(StringTag.valueOf(id.toString())));
        tag.put("CountedAdvancements", advancementList);

        tag.putDouble("Durability", durability);
        tag.put("Inventory", ContainerHelper.saveAllItems(new CompoundTag(), items, provider));
        return tag;
    }

    static ChainGroup load(CompoundTag tag, HolderLookup.Provider provider) {
        ChainGroup group = new ChainGroup();
        for (Tag member : tag.getList("Members", Tag.TAG_INT_ARRAY)) group.members.add(NbtUtils.loadUUID(member));
        for (Tag link : tag.getList("Links", Tag.TAG_COMPOUND)) {
            CompoundTag linkTag = (CompoundTag) link;
            group.links.add(ChainLink.of(linkTag.getUUID("A"), linkTag.getUUID("B")));
        }
        for (Tag id : tag.getList("CountedAdvancements", Tag.TAG_STRING)) group.countedAdvancements.add(ResourceLocation.parse(id.getAsString()));
        group.durability = tag.getDouble("Durability");
        ContainerHelper.loadAllItems(tag.getCompound("Inventory"), group.items, provider);
        return group;
    }
}
