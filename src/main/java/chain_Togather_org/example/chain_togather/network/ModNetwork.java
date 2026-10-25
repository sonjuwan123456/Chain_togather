package chain_Togather_org.example.chain_togather.network;

import chain_Togather_org.example.chain_togather.Chain_togather;
import chain_Togather_org.example.chain_togather.Config;
import chain_Togather_org.example.chain_togather.chain.ChainData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(Chain_togather.MODID, "main")).networkProtocolVersion(1).simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ChainSyncPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ChainSyncPacket::encode)
                .decoder(ChainSyncPacket::decode)
                .consumerMainThread(ChainSyncPacket::handle)
                .add();
    }

    public static void syncAll(MinecraftServer server) {
        CHANNEL.send(packet(server), PacketDistributor.ALL.noArg());
    }

    public static void syncTo(ServerPlayer player) {
        CHANNEL.send(packet(player.server), PacketDistributor.PLAYER.with(player));
    }

    private static ChainSyncPacket packet(MinecraftServer server) {
        return new ChainSyncPacket(ChainData.get(server).allLinks(), Config.getChainLength(), Config.getPullStrength());
    }
}
