package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.ClientConfig;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chain_togather.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DurabilityHud {
    private static final ItemStack ICON = new ItemStack(Items.CHAIN);
    private static final int X = 4;
    private static final int Y = 4;
    private static final int BAR_WIDTH = 60;
    private static final int BAR_HEIGHT = 5;

    private DurabilityHud() {
    }

    @SubscribeEvent
    public static void onAddLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().add(ForgeLayeredDraw.PRE_SLEEP_STACK, ResourceLocation.fromNamespaceAndPath(Chain_togather.MODID, "durability"), DurabilityHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        double durability = ClientChainData.durability();
        if (!ClientConfig.durabilityHud || durability < 0.0 || minecraft.player == null) return;
        if (minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) return;

        Font font = minecraft.font;
        float ratio = Mth.clamp((float) durability / 100.0F, 0.0F, 1.0F);
        int color = ratio > 0.5F ? 0xFF55FF55 : ratio > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;

        graphics.renderItem(ICON, X, Y);
        int barX = X + 20;
        int barY = Y + 4;
        graphics.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF000000);
        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF3A3A3A);
        graphics.fill(barX, barY, barX + Math.round(BAR_WIDTH * ratio), barY + BAR_HEIGHT, color);
        graphics.drawString(font, String.format("%.1f%%", durability), barX + BAR_WIDTH + 4, barY - 1, color, true);
    }
}
