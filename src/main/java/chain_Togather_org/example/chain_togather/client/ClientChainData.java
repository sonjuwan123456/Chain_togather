package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.chain.ChainLink;

import java.util.List;

public final class ClientChainData {
    private static List<ChainLink> links = List.of();
    private static double chainLength = 6.0;
    private static double pullStrength = 0.3;

    private ClientChainData() {
    }

    public static List<ChainLink> links() {
        return links;
    }

    public static double chainLength() {
        return chainLength;
    }

    public static double pullStrength() {
        return pullStrength;
    }

    public static void set(List<ChainLink> newLinks, double newChainLength, double newPullStrength) {
        links = List.copyOf(newLinks);
        chainLength = newChainLength;
        pullStrength = newPullStrength;
    }

    public static void clear() {
        links = List.of();
    }
}
