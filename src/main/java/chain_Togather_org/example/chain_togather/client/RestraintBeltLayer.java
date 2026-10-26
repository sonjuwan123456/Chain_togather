package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.ChainTogather;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = ChainTogather.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class RestraintBeltLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ModelResourceLocation MODEL = new ModelResourceLocation(
            ResourceLocation.fromNamespaceAndPath(ChainTogather.MODID, "entity/iron_belt"), "standalone");

    private static final float WAIST_Y = 10.5F;
    private static final float RING_DROP = 0.5F;
    private static final float RING_BACK = 5.0F;
    private static final float ARMORED_SCALE = 1.2F;

    public RestraintBeltLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    private static float horizontalScale(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem ? ARMORED_SCALE : 1.0F;
    }

    public static Vec3 ringPosition(Player player, float partialTick) {
        float bodyY = player.isCrouching() ? 3.2F : 0.0F;
        float bodyPitch = player.isCrouching() ? 0.5F : 0.0F;
        float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        double backX = Mth.sin(yaw);
        double backZ = -Mth.cos(yaw);

        float y = WAIST_Y + RING_DROP;
        float z = RING_BACK * horizontalScale(player);
        double modelY = (bodyY + y * Mth.cos(bodyPitch) - z * Mth.sin(bodyPitch)) / 16.0 - 1.501;
        double modelZ = (y * Mth.sin(bodyPitch) + z * Mth.cos(bodyPitch)) / 16.0;
        double height = -modelY * 0.9375;
        double behind = modelZ * 0.9375;
        return player.getPosition(partialTick).add(backX * behind, height, backZ * behind);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffers, int light, @NotNull AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || !ClientChainData.isChained(player.getUUID())) return;

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getModelManager().getModel(MODEL);
        float scale = horizontalScale(player);
        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, WAIST_Y / 16.0F, 0.0F);
        poseStack.scale(-scale, -1.0F, scale);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        minecraft.getBlockRenderer().getModelRenderer().renderModel(poseStack.last(), buffers.getBuffer(Sheets.cutoutBlockSheet()), null, model,
                1.0F, 1.0F, 1.0F, light, LivingEntityRenderer.getOverlayCoords(player, 0.0F), ModelData.EMPTY, null);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getPlayerSkin(skin);
            if (renderer != null) renderer.addLayer(new RestraintBeltLayer(renderer));
        }
    }
}
