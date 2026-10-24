package chain_Togather_org.example.chain_togather.chain;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.Config;
import chain_Togather_org.example.chain_togather.item.ChainItem;
import chain_Togather_org.example.chain_togather.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID)
public final class ChainEvents {
    private static boolean sharingEffect;
    private static boolean dyingTogether;
    private static boolean followingDimension;

    private ChainEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getItemStack().is(Chain_togather.BINDING_CHAIN.get()) || !(event.getTarget() instanceof LivingEntity target)) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer self) {
            ChainItem.link(self, target);
            event.setCancellationResult(InteractionResult.CONSUME);
        } else {
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ChainData data = ChainData.get(server);
        for (ChainGroup group : data.groups()) {
            if (SharedInventory.sync(group, group.livingPlayers(server))) data.setDirty();
            pullTogether(group, server);
        }
    }

    private static void pullTogether(ChainGroup group, MinecraftServer server) {
        double maxLength = Config.chainLength;
        for (ChainLink link : group.links) {
            LivingEntity a = ChainGroup.find(server, link.a());
            LivingEntity b = ChainGroup.find(server, link.b());
            if (a == null || b == null || !a.isAlive() || !b.isAlive() || a.isSpectator() || b.isSpectator()) continue;
            if (a.level() != b.level()) continue;

            double distance = a.distanceTo(b);
            if (distance > maxLength - 0.5) {
                catchFall(a, b);
                catchFall(b, a);
            }
            if (distance <= maxLength) continue;

            if (distance > maxLength * 4) {
                teleport(b, a);
                continue;
            }

            if (!(a instanceof Player)) pull(a, b, maxLength);
            if (!(b instanceof Player)) pull(b, a, maxLength);
        }
    }

    private static void pull(LivingEntity entity, LivingEntity other, double maxLength) {
        double share = ChainPhysics.share(entity, other);
        entity.setDeltaMovement(ChainPhysics.constrain(entity.getDeltaMovement(), entity.position(), other.position(), share, maxLength, Config.pullStrength));
        entity.hurtMarked = true;
    }

    private static void catchFall(LivingEntity hanging, LivingEntity holder) {
        if (hanging.getY() < holder.getY() - 0.5) hanging.resetFallDistance();
    }

    private static void teleport(LivingEntity entity, LivingEntity destination) {
        entity.teleportTo((ServerLevel) destination.level(), destination.getX(), destination.getY(), destination.getZ(), Set.of(), entity.getYRot(), entity.getXRot());
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ChainData data = ChainData.get(player.server);
        ChainGroup group = data.groupOf(player.getUUID());
        AdvancementHolder advancement = event.getAdvancement();
        DisplayInfo display = advancement.value().display().orElse(null);
        if (group == null || display == null) return;
        if (!group.countedAdvancements.add(advancement.id())) return;

        double amount = Config.damageFor(display.getType());
        boolean broken = data.damage(group, amount);
        Component message = Component.translatable("message.chain_togather.advancement", player.getDisplayName(), display.getTitle(),
                String.format("%.1f", amount), String.format("%.1f", group.durability())).withStyle(ChatFormatting.YELLOW);
        List<ServerPlayer> online = group.livingPlayers(player.server);
        online.forEach(member -> member.sendSystemMessage(message));

        if (broken) {
            data.dissolve(group);
            for (ServerPlayer member : online) {
                member.sendSystemMessage(Component.translatable("message.chain_togather.broken").withStyle(ChatFormatting.GREEN));
                member.level().playSound(null, member.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            ModNetwork.syncAll(player.server);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dyingTogether || event.isCanceled() || !(dead.level() instanceof ServerLevel level)) return;
        ChainData data = ChainData.get(level.getServer());
        ChainGroup group = data.groupOf(dead.getUUID());
        if (group == null) return;

        List<LivingEntity> others = group.livingMembers(level.getServer());
        others.remove(dead);
        ServerPlayer itemCarrier = dead instanceof ServerPlayer player ? player : null;
        for (LivingEntity member : others) {
            if (itemCarrier == null && member instanceof ServerPlayer player) itemCarrier = player;
        }

        boolean keepInventory = level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
        if (!keepInventory) group.items.clear();
        data.setDirty();

        dyingTogether = true;
        try {
            for (LivingEntity member : others) {
                if (member instanceof ServerPlayer player) {
                    if (!keepInventory && player != itemCarrier) player.getInventory().items.clear();
                    player.sendSystemMessage(Component.translatable("message.chain_togather.died_together", dead.getDisplayName()).withStyle(ChatFormatting.RED));
                }
                member.kill();
            }
        } finally {
            dyingTogether = false;
        }
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player || !(event.getLevel() instanceof ServerLevel level)) return;
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason == null || !reason.shouldDestroy()) return;

        ChainData data = ChainData.get(level.getServer());
        if (data.groupOf(entity.getUUID()) == null) return;
        data.removeMember(entity.getUUID());
        ModNetwork.syncAll(level.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (sharingEffect || !(entity.level() instanceof ServerLevel level)) return;
        ChainGroup group = ChainData.get(level.getServer()).groupOf(entity.getUUID());
        if (group == null) return;

        sharingEffect = true;
        try {
            for (LivingEntity member : group.livingMembers(level.getServer())) {
                if (member != entity) member.addEffect(new MobEffectInstance(event.getEffectInstance()), event.getEffectSource());
            }
        } finally {
            sharingEffect = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        if (sharingEffect || event.isCanceled() || !(entity.level() instanceof ServerLevel level)) return;
        ChainGroup group = ChainData.get(level.getServer()).groupOf(entity.getUUID());
        if (group == null) return;

        MobEffect effect = event.getEffect();
        Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect);
        sharingEffect = true;
        try {
            for (LivingEntity member : group.livingMembers(level.getServer())) {
                if (member != entity) member.removeEffect(holder);
            }
        } finally {
            sharingEffect = false;
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ChainGroup group = ChainData.get(player.server).groupOf(player.getUUID());
        if (group != null) SharedInventory.applyTo(group, player);
        ModNetwork.syncTo(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.isEndConquered() || !(event.getEntity() instanceof ServerPlayer player)) return;
        ChainGroup group = ChainData.get(player.server).groupOf(player.getUUID());
        if (group == null) return;

        SharedInventory.applyTo(group, player);
        for (LivingEntity member : group.livingMembers(player.server)) {
            if (member != player) {
                teleport(player, member);
                break;
            }
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (followingDimension || !(event.getEntity() instanceof ServerPlayer player)) return;
        ChainGroup group = ChainData.get(player.server).groupOf(player.getUUID());
        if (group == null) return;

        followingDimension = true;
        try {
            for (LivingEntity member : group.livingMembers(player.server)) {
                if (member != player && member.level() != player.level()) teleport(member, player);
            }
        } finally {
            followingDimension = false;
        }
    }

    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (followingDimension || entity instanceof Player || !(entity.level() instanceof ServerLevel level)) return;
        if (ChainData.get(level.getServer()).groupOf(entity.getUUID()) != null) event.setCanceled(true);
    }
}
