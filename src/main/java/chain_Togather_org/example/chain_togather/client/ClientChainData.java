package chain_Togather_org.example.chain_togather.client;

import chain_Togather_org.example.chain_togather.chain.ChainLink;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ClientChainData {
    private static List<ChainLink> links = List.of();
    private static Set<UUID> members = Set.of();
    private static double chainLength = 6.0;
    private static double pullStrength = 0.3;
    private static double durability = -1.0;

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

    public static boolean isChained(UUID id) {
        return members.contains(id);
    }

    public static double durability() {
        return durability;
    }

    public static void set(List<ChainLink> newLinks, double newChainLength, double newPullStrength, double newDurability) {
        links = List.copyOf(newLinks);
        Set<UUID> newMembers = new HashSet<>();
        for (ChainLink link : links) {
            newMembers.add(link.a());
            newMembers.add(link.b());
        }
        members = Set.copyOf(newMembers);
        chainLength = newChainLength;
        pullStrength = newPullStrength;
        durability = newDurability;
    }

    public static void clear() {
        links = List.of();
        members = Set.of();
        durability = -1.0;
    }
}
