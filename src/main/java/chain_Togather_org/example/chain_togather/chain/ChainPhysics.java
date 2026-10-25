package chain_Togather_org.example.chain_togather.chain;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class ChainPhysics {
    private static final double PLAYER_VOLUME = 0.6 * 0.6 * 1.8;
    private static final double ANCHOR_WEIGHT = 20.0;
    private static final double CLIMB_SPEED = 0.22;

    private ChainPhysics() {
    }

    private static boolean isAnchored(Entity entity) {
        return entity instanceof Player player && player.isCrouching() && player.onGround();
    }

    public static double weight(Entity entity) {
        if (entity instanceof Player) return isAnchored(entity) ? ANCHOR_WEIGHT : 1.0;
        double volume = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
        return Mth.clamp(volume / PLAYER_VOLUME, 0.25, 8.0);
    }

    public static double share(Entity self, Entity other) {
        double selfWeight = weight(self);
        double otherWeight = weight(other);
        return otherWeight / (selfWeight + otherWeight);
    }

    public static Vec3 constrain(Vec3 velocity, Vec3 self, Vec3 other, double share, double maxLength, double strength) {
        Vec3 offset = self.subtract(other);
        double distance = offset.length();
        if (distance <= maxLength || distance < 1.0E-6) return velocity;

        Vec3 outward = offset.scale(1.0 / distance);
        double radial = velocity.dot(outward);
        if (radial > 0.0) velocity = velocity.subtract(outward.scale(radial * share));
        double correction = Math.min((distance - maxLength) * strength, 1.0) * share;
        return velocity.subtract(outward.scale(correction));
    }

    public static boolean canClimb(Entity self, Entity other, double maxLength) {
        return !self.onGround() && other.getY() > self.getY() + 0.5 && self.distanceTo(other) > maxLength * 0.8;
    }

    public static Vec3 climb(Vec3 velocity, Vec3 self, Vec3 other) {
        Vec3 toward = other.subtract(self).normalize();
        return velocity.scale(0.5).add(toward.scale(CLIMB_SPEED));
    }
}
