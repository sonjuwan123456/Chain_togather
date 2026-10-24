package chain_Togather_org.example.chain_togather.chain;

import java.util.UUID;

public record ChainLink(UUID a, UUID b) {
    public static ChainLink of(UUID x, UUID y) {
        return x.compareTo(y) < 0 ? new ChainLink(x, y) : new ChainLink(y, x);
    }
}
