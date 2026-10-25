package chain_Togather_org.example.chain_togather;

import chain_Togather_org.example.chain_togather.chain.ChainMode;
import net.minecraft.advancements.AdvancementType;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = ChainTogather.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.DoubleValue CHAIN_LENGTH = BUILDER.defineInRange("chainLength", 6.0, 2.0, 64.0);

    private static final ForgeConfigSpec.DoubleValue PULL_STRENGTH = BUILDER.defineInRange("pullStrength", 0.3, 0.01, 2.0);

    private static final ForgeConfigSpec.DoubleValue TASK_DAMAGE = BUILDER.defineInRange("taskDamage", 0.5, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue GOAL_DAMAGE = BUILDER.defineInRange("goalDamage", 2.0, 0.0, 100.0);

    private static final ForgeConfigSpec.DoubleValue CHALLENGE_DAMAGE = BUILDER.defineInRange("challengeDamage", 5.0, 0.0, 100.0);

    private static final ForgeConfigSpec.EnumValue<ChainMode> CHAIN_MODE = BUILDER.defineEnum("chainMode", ChainMode.FREE);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static double chainLength;
    private static double pullStrength;
    private static double taskDamage;
    private static double goalDamage;
    private static double challengeDamage;
    private static ChainMode chainMode = ChainMode.FREE;

    private Config() {
    }

    public static double getChainLength() {
        return chainLength;
    }

    public static double getPullStrength() {
        return pullStrength;
    }

    public static ChainMode getChainMode() {
        return chainMode;
    }

    public static double damageFor(AdvancementType type) {
        return switch (type) {
            case TASK -> taskDamage;
            case GOAL -> goalDamage;
            case CHALLENGE -> challengeDamage;
        };
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        chainLength = CHAIN_LENGTH.get();
        pullStrength = PULL_STRENGTH.get();
        taskDamage = TASK_DAMAGE.get();
        goalDamage = GOAL_DAMAGE.get();
        challengeDamage = CHALLENGE_DAMAGE.get();
        chainMode = CHAIN_MODE.get();
    }
}
