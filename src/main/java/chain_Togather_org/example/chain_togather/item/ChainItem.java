package chain_Togather_org.example.chain_togather.item;

import chain_Togather_org.example.chain_togather.chain.ChainData;
import chain_Togather_org.example.chain_togather.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChainItem extends Item {
    public ChainItem(Properties properties) {
        super(properties);
    }

    public static void link(ServerPlayer self, ServerPlayer target) {
        switch (ChainData.get(self.server).link(self, target)) {
            case LINKED -> announceLink(self, target);
            case ALREADY_LINKED -> refuse(self, "message.chain_togather.already_linked", target);
            case NOT_LINE_END -> refuse(self, "message.chain_togather.not_line_end", target);
            case WOULD_CLOSE_LOOP -> refuse(self, "message.chain_togather.would_close_loop", target);
        }
    }

    private static void refuse(ServerPlayer self, String key, ServerPlayer target) {
        self.displayClientMessage(Component.translatable(key, target.getDisplayName()), true);
    }

    private static void announceLink(ServerPlayer self, ServerPlayer target) {
        self.displayClientMessage(Component.translatable("message.chain_togather.linked", target.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        target.displayClientMessage(Component.translatable("message.chain_togather.linked", self.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        self.level().playSound(null, target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
        ModNetwork.syncAll(self.server);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.chain_togather.binding_chain").withStyle(ChatFormatting.GRAY));
    }
}
