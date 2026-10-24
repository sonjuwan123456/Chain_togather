package chain_Togather_org.example.chain_togather.network;

import chain_Togather_org.example.chain_togather.chain.ChainLink;
import chain_Togather_org.example.chain_togather.client.ClientChainData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public record ChainSyncPacket(List<ChainLink> links) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(links.size());
        for (ChainLink link : links) {
            buf.writeUUID(link.a());
            buf.writeUUID(link.b());
        }
    }

    public static ChainSyncPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ChainLink> links = new ArrayList<>(size);
        for (int i = 0; i < size; i++) links.add(ChainLink.of(buf.readUUID(), buf.readUUID()));
        return new ChainSyncPacket(links);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ClientChainData.setLinks(links);
        context.setPacketHandled(true);
    }
}
