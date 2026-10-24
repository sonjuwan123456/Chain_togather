package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.ClientConfig;
import chain_Togather_org.example.chain_togather.chain.ChainLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
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
public final class ChainSounds {
    private static final Set<ChainLink> tautLinks = new HashSet<>();
    private static final Map<ChainLink, Long> lastHitTick = new HashMap<>();

    private ChainSounds() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<ChainLink> links = ClientChainData.links();
        if (level == null || minecraft.isPaused()) return;

        tautLinks.retainAll(links);
        lastHitTick.keySet().retainAll(links);
        if (links.isEmpty() || !ClientConfig.chainSounds || ClientConfig.chainSoundVolume <= 0.0F) return;

        Map<UUID, Entity> entities = ClientChainPhysics.findEntities(level, links);
        double maxLength = ClientChainData.chainLength();
        long tick = level.getGameTime();

        for (ChainLink link : links) {
            Entity a = entities.get(link.a());
            Entity b = entities.get(link.b());
            if (a == null || b == null || !a.isAlive() || !b.isAlive() || a.isSpectator() || b.isSpectator()) continue;

            double distance = a.distanceTo(b);
            if (distance < maxLength - 1.0) tautLinks.remove(link);
            if (distance <= maxLength || distance > maxLength * 4) continue;

            if (tautLinks.add(link)) {
                Long last = lastHitTick.get(link);
                if (last == null || tick - last >= 40) {
                    lastHitTick.put(link, tick);
                    play(level, a, b, SoundEvents.CHAIN_HIT, 0.35F);
                }
            } else if (tick % 50 == 0) {
                play(level, a, b, SoundEvents.CHAIN_STEP, 0.15F);
            }
        }
    }

    private static void play(ClientLevel level, Entity a, Entity b, SoundEvent sound, float volume) {
        Vec3 middle = ClientChainPhysics.anchor(a, 1.0F).add(ClientChainPhysics.anchor(b, 1.0F)).scale(0.5);
        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        level.playLocalSound(middle.x, middle.y, middle.z, sound, SoundSource.PLAYERS, volume * ClientConfig.chainSoundVolume, pitch, false);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        tautLinks.clear();
        lastHitTick.clear();
    }
}
