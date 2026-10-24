package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.Chain_togather;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID, value = Dist.CLIENT)
public final class ClientChainPhysics {
    private static final Map<ChainLink, RopeSimulation> ropes = new HashMap<>();

    private ClientChainPhysics() {
    }

    public static RopeSimulation rope(ChainLink link) {
        return ropes.get(link);
    }

    public static Map<UUID, Entity> findEntities(ClientLevel level, List<ChainLink> links) {
        Set<UUID> wanted = new HashSet<>();
        for (ChainLink link : links) {
            wanted.add(link.a());
            wanted.add(link.b());
        }
        Map<UUID, Entity> entities = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (wanted.contains(entity.getUUID())) entities.put(entity.getUUID(), entity);
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

        Map<UUID, Entity> entities = findEntities(minecraft.level, links);
        double maxLength = ClientChainData.chainLength();
        Vec3 velocity = self.getDeltaMovement();
        boolean changed = false;

        for (ChainLink link : links) {
            UUID otherId;
            if (link.a().equals(self.getUUID())) otherId = link.b();
            else if (link.b().equals(self.getUUID())) otherId = link.a();
            else continue;
            Entity other = entities.get(otherId);
            if (other == null || !other.isAlive()) continue;

            double share = ChainPhysics.share(self, other);
            velocity = ChainPhysics.constrain(velocity, self.position(), other.position(), share, maxLength, ClientChainData.pullStrength());
            if (self.input.jumping && ChainPhysics.canClimb(self, other, maxLength)) {
                velocity = ChainPhysics.climb(velocity, self.position(), other.position());
            }
            changed = true;
        }
        if (changed) self.setDeltaMovement(velocity);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<ChainLink> links = ClientChainData.links();
        if (level == null || minecraft.isPaused()) return;

        ropes.keySet().retainAll(links);
        if (links.isEmpty()) return;
        Map<UUID, Entity> entities = findEntities(level, links);
        double length = ClientChainData.chainLength();

        for (ChainLink link : links) {
            Entity a = entities.get(link.a());
            Entity b = entities.get(link.b());
            if (a == null || b == null) {
                ropes.remove(link);
                continue;
            }
            ropes.computeIfAbsent(link, key -> new RopeSimulation()).tick(level, anchor(a, 1.0F), anchor(b, 1.0F), length);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientChainData.clear();
        ropes.clear();
    }
}
