package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.Config;
import chain_Togather_org.example.chain_togather.chain.ChainLink;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID, value = Dist.CLIENT)
public final class ChainRenderer {
    private static final double LINK_LENGTH = 0.75;
    private static final double MAX_SAG = 1.5;
    private static final int MAX_SEGMENTS = 96;

    private ChainRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<ChainLink> links = ClientChainData.links();
        if (level == null || links.isEmpty()) return;

        Set<UUID> wanted = new HashSet<>();
        for (ChainLink link : links) {
            wanted.add(link.a());
            wanted.add(link.b());
        }
        Map<UUID, Entity> entities = new HashMap<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (wanted.contains(entity.getUUID())) entities.put(entity.getUUID(), entity);
        }

        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        BlockState chain = Blocks.CHAIN.defaultBlockState();
        PoseStack poseStack = new PoseStack();

        for (ChainLink link : links) {
            Entity a = entities.get(link.a());
            Entity b = entities.get(link.b());
            if (a == null || b == null) continue;
            renderChain(poseStack, buffers, blockRenderer, chain, level, camera, anchor(a, partialTick), anchor(b, partialTick));
        }
        buffers.endBatch();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientChainData.setLinks(List.of());
    }

    private static Vec3 anchor(Entity entity, float partialTick) {
        return entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.55, 0.0);
    }

    private static void renderChain(PoseStack poseStack, MultiBufferSource buffers, BlockRenderDispatcher blockRenderer, BlockState chain,
                                    ClientLevel level, Vec3 camera, Vec3 from, Vec3 to) {
        double distance = from.distanceTo(to);
        double length = Config.chainLength;
        double sag = distance < length ? Math.min(Math.sqrt(length * length - distance * distance) / 2.0, MAX_SAG) : 0.0;
        int segments = Mth.clamp((int) Math.ceil(Math.max(distance, length * 0.5) / LINK_LENGTH), 1, MAX_SEGMENTS);

        Vec3 previous = from;
        for (int i = 1; i <= segments; i++) {
            double t = (double) i / segments;
            Vec3 next = from.lerp(to, t).add(0.0, -sag * 4.0 * t * (1.0 - t), 0.0);
            Vec3 direction = next.subtract(previous);
            float segmentLength = (float) direction.length();
            if (segmentLength > 1.0E-4F) {
                int light = LevelRenderer.getLightColor(level, BlockPos.containing(previous.lerp(next, 0.5)));
                poseStack.pushPose();
                poseStack.translate(previous.x - camera.x, previous.y - camera.y, previous.z - camera.z);
                poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) direction.x, (float) direction.y, (float) direction.z));
                poseStack.mulPose(new Quaternionf().rotationY(i % 2 == 0 ? 0.0F : Mth.HALF_PI));
                poseStack.scale(1.0F, segmentLength, 1.0F);
                poseStack.translate(-0.5F, 0.0F, -0.5F);
                blockRenderer.renderSingleBlock(chain, poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
                poseStack.popPose();
            }
            previous = next;
        }
    }
}
