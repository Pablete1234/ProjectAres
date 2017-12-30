package tc.oc.pgm.payload;


import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Rails;
import org.bukkit.util.Vector;
import tc.oc.commons.bukkit.util.BlockFaces;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class PayloadTrack {
    private final Payload payload;
    private int size = 0;
    private Path headPath, tailPath, currentPath;

    public PayloadTrack(Payload payload) {
        this.payload = payload;

        List<BlockState> trackBlocks = getTrack();
        if (trackBlocks == null) return; // There's no track?

        // First block (centered)
        add(toBlockVector(trackBlocks.get(0)), false);

        BlockState last = trackBlocks.get(0);
        trackBlocks.remove(0); // Remove first block
        for (BlockState loc : trackBlocks) {
            boolean isSpawn = payload.getSpawnLocation().coarseEquals(last.getPosition()), isCheckpoint = isCheckpoint(last);
            if (isSpawn || isCheckpoint) { // Special coordinates, block centered
                add(toBlockVector(last), isCheckpoint);
                if (isSpawn) setStart();
                else if (payload.getDefinition().hasFriendlyCheckpoints() ^ currentPath != null)
                    payload.enemyReachedCheckpoints.add(tailPath);
                else payload.friendlyReachedCheckpoints.add(tailPath);
            }
            Vector newPos = new Vector(last.getPosition().blockCenter().midway(loc.getPosition().blockCenter()));
            newPos.setY(Math.max(last.getY(), (last = loc).getY()));
            add(newPos, false); // Every block corner (non-centered)

        }
        // Last block (centered)
        add(toBlockVector(trackBlocks.get(trackBlocks.size() - 1)), false);
    }

    public void add(Vector position, boolean checkpoint) {
        if (size == 0) headPath = tailPath = currentPath = new Path(size++, position, checkpoint,null);
        else tailPath = tailPath.nextPath = new Path(size++, position, checkpoint, tailPath);
        if (checkpoint) payload.allCheckpoints.add(tailPath);
    }

    public Vector head() {
        return headPath;
    }
    public Vector tail() {
        return tailPath;
    }
    public Vector current() {
        return currentPath;
    }
    public void setStart() { // Will set the currently latest added node to be the start
        this.currentPath = tailPath;
    }
    public void forward() {
        if (hasNext()) currentPath = currentPath.nextPath;
    }
    public void backwards() {
        if (hasPrevious()) currentPath = currentPath.previousPath;
    }

    public boolean hasPrevious() {
        return currentPath.previousPath != null;
    }

    public Vector previous() {
        return currentPath.previousPath;
    }

    public boolean hasNext() {
        return currentPath.nextPath != null;
    }

    public Vector next() {
        return currentPath.nextPath;
    }

    public int getIndex() {
        return currentPath.index;
    }

    public Vector getPosition() {
        return currentPath;
    }

    public boolean isCheckpoint() {
        return currentPath.checkpoint;
    }

    public boolean isNextCheckpoint() {
        return currentPath.nextPath.checkpoint;
    }
    public boolean isPreviousCheckpoint() {
        return currentPath.previousPath.checkpoint;
    }

    private class Path extends Vector {
        private int index;
        private Path previousPath;
        private Path nextPath = null;
        private boolean checkpoint;

        private Path(int index, Vector position, boolean checkpoint, Path previousPath) {
            super(position);
            this.index = index;
            this.previousPath = previousPath;
            this.checkpoint = checkpoint;
        }

        @Override
        public String toString() {
            return "Path{" +
                    "index=" + index +
                    ", position=" + super.toString() +
                    ", checkpoint=" + checkpoint +
                    '}';
        }
    }

    private List<BlockState> getTrack() {
        BlockState state = payload.getStartingLocation().toLocation(payload.getMatch().getWorld()).getBlock().getState();

        //Payload must start on a straight and plain rail
        if (!isRails(state.getType())) return null;
        Rails rail = (Rails) state.getMaterialData();
        if (rail.isCurve() || rail.isOnSlope()) return null;

        // Faces to continue to from spawn
        BlockFace[] faces = Stream.of(rail.getDirection(), rail.getDirection().getOppositeFace()).filter(face ->
                isRails(BlockFaces.getRelative(state, face).getType())).toArray(BlockFace[]::new);

        if (faces.length != 1) return null; // Spawn isn't the tail of the track, has more than one connected track (or none)

        List<BlockState> railBlocks = new LinkedList<>();

        BlockState lastState = state;  // Last rail block used, null if no more rails exist
        BlockFace lastFace = faces[0]; // Last block face used (exit face in lastState)
        while (lastFace != null) {
            railBlocks.add(lastState); // Add last state to list

            Rails rails = (Rails) lastState.getMaterialData();
            boolean slopeUp = rails.isOnSlope() && rails.getDirection() == lastFace;

            lastState = BlockFaces.getRelative(lastState, lastFace); // Move to the next rail
            if (slopeUp) lastState = BlockFaces.getRelative(lastState, BlockFace.UP); // If it's a sloping up rail, move one up
            if (!isRails(lastState.getType())) { // If can't find a rail on the level, try find one directly down
                lastState = BlockFaces.getRelative(lastState, BlockFace.DOWN);
                if (!isRails(lastState.getType())) break; // Still can't find rail, end of the loop
            }
            lastFace = getOtherRailSide((Rails) lastState.getMaterialData(), lastFace.getOppositeFace());
        }
        railBlocks.add(lastState); // Add last block
        return railBlocks;
    }

    private static Vector toBlockVector(BlockState bs) {
        Vector vec = new Vector(bs.getPosition().blockCenter());
        Rails rails = (Rails) bs.getMaterialData();
        if (!rails.isOnSlope()) vec.setY(vec.getBlockY()); // If rail is a slope, center will be a .5 Y, otherwise .0
        return vec;
    }

    private static final Set<Material> CHECKPOINT_RAILS = ImmutableSet.<Material>builder().add(Material.POWERED_RAIL).add(Material.DETECTOR_RAIL).add(Material.POWERED_RAIL).build();
    private static final Set<Material> ALL_RAILS = ImmutableSet.<Material>builder().add(Material.RAILS).addAll(CHECKPOINT_RAILS).build();
    private static boolean isRails(Material material) {
        return ALL_RAILS.contains(material);
    }

    private boolean isCheckpoint(BlockState blockState) {
        Material material = blockState.getMaterial(); // TODO: use coordinates to define checkpoints not block types
        return payload.getDefinition().getCheckpointMaterial() == null ? CHECKPOINT_RAILS.contains(material) :
                material.equals(payload.getDefinition().getCheckpointMaterial().getMaterial());
    }

    // Takes the input of a rail block data, and the face you are entering, and will return the exit face
    // For example, a curved rail and SOUTH, will return null if the rail never connects south, or EAST / WEST
    private static BlockFace getOtherRailSide(Rails rails, BlockFace face) {
        boolean curve = rails.isCurve();
        BlockFace direction = rails.getDirection();
        if (curve) direction = direction.getOppositeFace();

        if (!(curve ? face.getModX() == direction.getModX() || face.getModZ() == direction.getModZ()
                : (face == direction || (face.getOppositeFace() == direction)))) {
            return null; // Rail doesn't connect to previous rail
        }
        return curve ? blockFaceOfMod(direction.getModX() - face.getModX(), direction.getModY() - face.getModY(),
                direction.getModZ() - face.getModZ()) : face == direction ? face.getOppositeFace() : direction;
    }

    private static BlockFace blockFaceOfMod(int x, int y, int z) {
        return Arrays.stream(BlockFace.values())
                .filter(f -> f.getModX() == x && f.getModY() == y && f.getModZ() == z).findFirst().orElse(null);
    }

}
