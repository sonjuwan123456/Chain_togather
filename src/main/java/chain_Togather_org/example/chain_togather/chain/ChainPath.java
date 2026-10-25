package chain_Togather_org.example.chain_togather.chain;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayList;
import java.util.List;

public final class ChainPath {
    private static final int MAX_PIVOTS = 8;
    private static final double PIVOT_OFFSET = 0.05;
    private static final double MIN_PIVOT_GAP = 0.1;

    private final List<Vec3> pivots = new ArrayList<>();
    private Level level;
    private Vec3 start = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;

    public void update(Level level, Vec3 start, Vec3 end) {
        if (level != this.level) pivots.clear();
        this.level = level;
        this.start = start;
        this.end = end;

        while (!pivots.isEmpty() && isClear(level, start, point(2))) pivots.removeFirst();
        while (!pivots.isEmpty() && isClear(level, end, point(pivots.size() - 1))) pivots.removeLast();
        wrap(level, start, point(1), 0);
        wrap(level, end, point(pivots.size()), pivots.size());
    }

    public double length() {
        double total = 0.0;
        Vec3 previous = start;
        for (Vec3 pivot : pivots) {
            total += previous.distanceTo(pivot);
            previous = pivot;
        }
        return total + previous.distanceTo(end);
    }

    public Vec3 directionFromStart() {
        return point(1).subtract(start).normalize();
    }

    public Vec3 directionFromEnd() {
        return point(pivots.size()).subtract(end).normalize();
    }

    private Vec3 point(int index) {
        if (index <= 0) return start;
        if (index > pivots.size()) return end;
        return pivots.get(index - 1);
    }

    private void wrap(Level level, Vec3 from, Vec3 to, int index) {
        if (pivots.size() >= MAX_PIVOTS) return;
        BlockHitResult hit = clip(level, from, to);
        if (hit.getType() == HitResult.Type.MISS) return;
        Vec3 pivot = hit.getLocation().add(Vec3.atLowerCornerOf(hit.getDirection().getNormal()).scale(PIVOT_OFFSET));
        if (pivot.distanceTo(from) > MIN_PIVOT_GAP && pivot.distanceTo(to) > MIN_PIVOT_GAP) pivots.add(index, pivot);
    }

    private static boolean isClear(Level level, Vec3 from, Vec3 to) {
        return clip(level, from, to).getType() == HitResult.Type.MISS;
    }

    private static BlockHitResult clip(Level level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
    }
}
