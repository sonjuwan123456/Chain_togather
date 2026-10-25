package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.ClientConfig;
import chain_Togather_org.example.chain_togather.chain.ChainLink;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChainSounds {
    private static final Set<ChainLink> tautLinks = new HashSet<>();
    private static final Map<ChainLink, Long> lastHitTick = new HashMap<>();

    private ChainSounds() {
    }

    static void tick(ClientLevel level, List<ChainLink> links, Map<UUID, Entity> entities) {
        tautLinks.retainAll(links);
        lastHitTick.keySet().retainAll(links);
        if (links.isEmpty() || !ClientConfig.isChainSoundsEnabled() || ClientConfig.getChainSoundVolume() <= 0.0F) return;

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
        level.playLocalSound(middle.x, middle.y, middle.z, sound, SoundSource.PLAYERS, volume * ClientConfig.getChainSoundVolume(), pitch, false);
    }

    static void clear() {
        tautLinks.clear();
        lastHitTick.clear();
    }
}
