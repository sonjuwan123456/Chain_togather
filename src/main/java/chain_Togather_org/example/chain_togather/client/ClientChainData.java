package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.chain.ChainLink;

import java.util.List;

public final class ClientChainData {
    private static List<ChainLink> links = List.of();

    private ClientChainData() {
    }

    public static List<ChainLink> links() {
        return links;
    }

    public static void setLinks(List<ChainLink> newLinks) {
        links = List.copyOf(newLinks);
    }
}
