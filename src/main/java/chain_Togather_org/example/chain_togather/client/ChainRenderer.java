package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.chain.ChainLink;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID, value = Dist.CLIENT)
public final class ChainRenderer {
    private ChainRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<ChainLink> links = ClientChainData.links();
        if (level == null || links.isEmpty()) return;

        Map<UUID, Entity> entities = ClientChainPhysics.trackedEntities(level);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        SegmentContext context = new SegmentContext(new PoseStack(), buffers, minecraft.getBlockRenderer(),
                Blocks.CHAIN.defaultBlockState(), level, event.getCamera().getPosition());

        for (ChainLink link : links) {
            Entity a = entities.get(link.a());
            Entity b = entities.get(link.b());
            RopeSimulation rope = ClientChainPhysics.rope(link);
            if (a == null || b == null || a.isRemoved() || b.isRemoved() || rope == null || rope.size() < 2) continue;

            int last = rope.size() - 1;
            Vec3 previous = ClientChainPhysics.anchor(a, partialTick);
            for (int i = 1; i <= last; i++) {
                Vec3 next = i == last ? ClientChainPhysics.anchor(b, partialTick) : rope.node(i, partialTick);
                renderSegment(context, previous, next, i % 2 == 0);
                previous = next;
            }
        }
        buffers.endBatch();
    }

    private record SegmentContext(PoseStack poseStack, MultiBufferSource buffers, BlockRenderDispatcher blockRenderer,
                                  BlockState chain, ClientLevel level, Vec3 camera) {
    }

    private static void renderSegment(SegmentContext context, Vec3 from, Vec3 to, boolean turned) {
        Vec3 direction = to.subtract(from);
        float length = (float) direction.length();
        if (length < 1.0E-4F) return;

        PoseStack poseStack = context.poseStack();
        Vec3 camera = context.camera();
        int light = LevelRenderer.getLightColor(context.level(), BlockPos.containing(from.lerp(to, 0.5)));
        poseStack.pushPose();
        poseStack.translate(from.x - camera.x, from.y - camera.y, from.z - camera.z);
        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) direction.x, (float) direction.y, (float) direction.z));
        poseStack.mulPose(new Quaternionf().rotationY(turned ? 0.0F : Mth.HALF_PI));
        poseStack.scale(1.0F, length, 1.0F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        context.blockRenderer().renderSingleBlock(context.chain(), poseStack, context.buffers(), light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, Sheets.cutoutBlockSheet());
        poseStack.popPose();
    }
}
