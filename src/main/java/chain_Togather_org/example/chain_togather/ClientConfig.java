package chain_Togather_org.example.chain_togather;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue CHAIN_SOUNDS = BUILDER.define("chainSounds", true);

    private static final ForgeConfigSpec.DoubleValue CHAIN_SOUND_VOLUME = BUILDER.defineInRange("chainSoundVolume", 1.0, 0.0, 1.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean chainSounds = true;
    private static float chainSoundVolume = 1.0F;

    private ClientConfig() {
    }

    public static boolean isChainSoundsEnabled() {
        return chainSounds;
    }

    public static float getChainSoundVolume() {
        return chainSoundVolume;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        chainSounds = CHAIN_SOUNDS.get();
        chainSoundVolume = CHAIN_SOUND_VOLUME.get().floatValue();
    }
}
