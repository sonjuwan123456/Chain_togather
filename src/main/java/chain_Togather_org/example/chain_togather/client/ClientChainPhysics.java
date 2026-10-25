package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.ChainTogather;
import chain_Togather_org.example.chain_togather.chain.ChainLink;
import chain_Togather_org.example.chain_togather.chain.ChainPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ChainTogather.MODID, value = Dist.CLIENT)
public final class ClientChainPhysics {
    private static final Map<ChainLink, RopeSimulation> ROPES = new HashMap<>();
    private static ClientLevel trackedLevel;
    private static Map<UUID, Entity> trackedEntities = Map.of();

    private ClientChainPhysics() {
    }

    public static RopeSimulation rope(ChainLink link) {
        return ROPES.get(link);
    }

    public static Map<UUID, Entity> trackedEntities(ClientLevel level) {
        return level == trackedLevel ? trackedEntities : Map.of();
    }

    private static Map<UUID, Entity> findEntities(ClientLevel level, List<ChainLink> links) {
        if (links.isEmpty()) return Map.of();
        Map<UUID, Entity> entities = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (ClientChainData.isChained(entity.getUUID())) entities.put(entity.getUUID(), entity);
        }
        return entities;
    }

    public static Vec3 anchor(Entity entity, float partialTick) {
        if (entity instanceof Player player) {
            Pose pose = player.getPose();
            if (pose == Pose.STANDING || pose == Pose.CROUCHING) return RestraintBeltLayer.ringPosition(player, partialTick);
            return player.getPosition(partialTick).add(0.0, player.getBbHeight() * 0.5, 0.0);
        }
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.55, 0.0);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer self = minecraft.player;
        List<ChainLink> links = ClientChainData.links();
        if (self == null || event.player != self || minecraft.level == null || links.isEmpty() || self.isSpectator()) return;

        Map<UUID, Entity> entities = trackedEntities(minecraft.level);
        double maxLength = ClientChainData.chainLength();
        Vec3 velocity = self.getDeltaMovement();
        boolean changed = false;

        for (ChainLink link : links) {
            Entity other = linkedOther(link, self.getUUID(), entities);
            if (other == null) continue;

            double share = ChainPhysics.share(self, other);
            velocity = ChainPhysics.constrain(velocity, self.position(), other.position(), share, maxLength, ClientChainData.pullStrength());
            if (self.input.jumping && ChainPhysics.canClimb(self, other, maxLength)) {
                velocity = ChainPhysics.climb(velocity, self.position(), other.position());
            }
            changed = true;
        }
        if (changed) self.setDeltaMovement(velocity);
    }

    @Nullable
    private static Entity linkedOther(ChainLink link, UUID selfId, Map<UUID, Entity> entities) {
        UUID otherId;
        if (link.a().equals(selfId)) otherId = link.b();
        else if (link.b().equals(selfId)) otherId = link.a();
        else return null;
        Entity other = entities.get(otherId);
        return other != null && other.isAlive() ? other : null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) return;

        List<ChainLink> links = ClientChainData.links();
        trackedLevel = level;
        trackedEntities = findEntities(level, links);
        ROPES.keySet().retainAll(links);
        double length = ClientChainData.chainLength();

        for (ChainLink link : links) {
            Entity a = trackedEntities.get(link.a());
            Entity b = trackedEntities.get(link.b());
            if (a == null || b == null) {
                ROPES.remove(link);
                continue;
            }
            ROPES.computeIfAbsent(link, key -> new RopeSimulation()).tick(level, anchor(a, 1.0F), anchor(b, 1.0F), length);
        }
        ChainSounds.tick(level, links, trackedEntities);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientChainData.clear();
        ROPES.clear();
        trackedLevel = null;
        trackedEntities = Map.of();
        ChainSounds.clear();
    }
}
