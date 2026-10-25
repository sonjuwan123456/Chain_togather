package chain_Togather_org.example.chain_togather.network;

import chain_Togather_org.example.chain_togather.chain.ChainLink;
import chain_Togather_org.example.chain_togather.client.ClientChainData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public record ChainSyncPacket(List<ChainLink> links, double chainLength, double pullStrength) {
    private static final int MAX_INITIAL_CAPACITY = 1024;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(links.size());
        for (ChainLink link : links) {
            buf.writeUUID(link.a());
            buf.writeUUID(link.b());
        }
        buf.writeDouble(chainLength);
        buf.writeDouble(pullStrength);
    }

    public static ChainSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ChainLink> links = new ArrayList<>(Mth.clamp(size, 0, MAX_INITIAL_CAPACITY));
        for (int i = 0; i < size; i++) links.add(ChainLink.of(buf.readUUID(), buf.readUUID()));
        return new ChainSyncPacket(links, buf.readDouble(), buf.readDouble());
    }

    public void handle(CustomPayloadEvent.Context context) {
        ClientChainData.set(links, chainLength, pullStrength);
        context.setPacketHandled(true);
    }
}
