package chain_Togather_org.example.chain_togather.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RopeSimulation {
    private static final double NODE_SPACING = 0.5;
    private static final double GRAVITY = 0.04;
    private static final double DAMPING = 0.96;
    private static final int ITERATIONS = 10;
    private static final double RESET_DISTANCE = 8.0;

    private Vec3[] current = new Vec3[0];
    private Vec3[] previous = new Vec3[0];

    public int size() {
        return current.length;
    }

    public Vec3 node(int index, float partialTick) {
        return previous[index].lerp(current[index], partialTick);
    }

    public void tick(ClientLevel level, Vec3 start, Vec3 end, double length) {
        int count = Mth.clamp((int) Math.ceil(length / NODE_SPACING) + 1, 6, 33);
        if (current.length != count || current[0].distanceTo(start) > RESET_DISTANCE || current[count - 1].distanceTo(end) > RESET_DISTANCE) {
            reset(start, end, count);
            return;
        }

        for (int i = 1; i < count - 1; i++) {
            Vec3 velocity = current[i].subtract(previous[i]).scale(DAMPING);
            previous[i] = current[i];
            current[i] = current[i].add(velocity).add(0.0, -GRAVITY, 0.0);
        }
        previous[0] = current[0];
        previous[count - 1] = current[count - 1];
        current[0] = start;
        current[count - 1] = end;

        double restLength = Math.max(length, start.distanceTo(end)) / (count - 1);
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            satisfyConstraints(count, restLength);
            for (int i = 1; i < count - 1; i++) collide(level, i);
        }
    }

    private void satisfyConstraints(int count, double restLength) {
        for (int i = 0; i < count - 1; i++) {
            Vec3 delta = current[i + 1].subtract(current[i]);
            double distance = delta.length();
            if (distance >= 1.0E-6) applyFix(i, delta.scale((distance - restLength) / distance), count);
        }
    }

    private void applyFix(int i, Vec3 fix, int count) {
        if (i == 0) {
            current[i + 1] = current[i + 1].subtract(fix);
        } else if (i + 1 == count - 1) {
            current[i] = current[i].add(fix);
        } else {
            current[i] = current[i].add(fix.scale(0.5));
            current[i + 1] = current[i + 1].subtract(fix.scale(0.5));
        }
    }

    private void collide(ClientLevel level, int index) {
        Vec3 node = current[index];
        BlockPos pos = BlockPos.containing(node);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) return;
        double top = pos.getY() + shape.max(Direction.Axis.Y) + 0.03;
        if (node.y < top) {
            current[index] = new Vec3(node.x, top, node.z);
            previous[index] = new Vec3(previous[index].x, top, previous[index].z);
        }
    }

    private void reset(Vec3 start, Vec3 end, int count) {
        current = new Vec3[count];
        previous = new Vec3[count];
        for (int i = 0; i < count; i++) {
            current[i] = start.lerp(end, (double) i / (count - 1));
            previous[i] = current[i];
        }
    }
}
