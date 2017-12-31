package tc.oc.pgm.payload;


import com.google.common.collect.ImmutableSet;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.material.Rails;
import org.bukkit.util.Vector;
import tc.oc.commons.bukkit.util.BlockFaces;
import tc.oc.pgm.module.ModuleLoadException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PayloadTrack {
    private final Payload payload;
    private int size = 0;
    private Path headPath, tailPath, currentPath;

    PayloadTrack(Payload payload) throws ModuleLoadException {
        this.payload = payload;

        BlockState state = payload.getStartingLocation().toLocation(payload.getMatch().getWorld()).getBlock().getState();

        if (!isRails(state.getType())) throw new ModuleLoadException("Payload start position is not a rail.");
        Rails rail = (Rails) state.getMaterialData();

        // Faces to continue to from spawn
        List<BlockFace> faces = Arrays.stream(BlockFaces.NEIGHBORS).filter(bf -> hasConnection(rail, bf)).filter(face ->
                isRails(BlockFaces.getRelative(state, face).getType())).collect(Collectors.toList());

        if (faces.size() != 1) throw new ModuleLoadException("First track has " + faces.size() + " connections, while it should have 1 (connected to : " + faces + ")");

        List<BlockState> trackBlocks = getTrack(state, faces.get(0));

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

        if (currentPath == null) throw new ModuleLoadException("Payload start position '" + payload.getStartingLocation() + "' was not found on the track");
        if (payload.allCheckpoints.size() != payload.getDefinition().getCheckpoints().size())
            throw new ModuleLoadException("Checkpoints don't match. Found " + payload.allCheckpoints.size() + " checkpoints in the track but there were " + payload.getDefinition().getCheckpoints().size() + " defined");
    }

    private void add(Vector position, boolean checkpoint) {
        if (size == 0) headPath = tailPath = new Path(size++, position, checkpoint,null);
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

    private List<BlockState> getTrack(BlockState currBlock, BlockFace currFace) {
        List<BlockState> railBlocks = new LinkedList<>();
        while (currFace != null) {
            railBlocks.add(currBlock); // Add last state to list

            Rails rails = (Rails) currBlock.getMaterialData();
            boolean slopeUp = rails.isOnSlope() && rails.getDirection() == currFace;

            currBlock = BlockFaces.getRelative(currBlock, currFace); // Move to the next rail
            if (slopeUp) currBlock = BlockFaces.getRelative(currBlock, BlockFace.UP); // If it's a sloping up rail, move one up
            if (!isRails(currBlock.getType())) { // If can't find a rail on the level, try find one directly down
                currBlock = BlockFaces.getRelative(currBlock, BlockFace.DOWN);
                if (!isRails(currBlock.getType())) break; // Still can't find rail, end of the loop
            }
            currFace = getOtherRailSide((Rails) currBlock.getMaterialData(), currFace.getOppositeFace());
        }
        return railBlocks;
    }

    private static Vector toBlockVector(BlockState bs) {
        Vector vec = new Vector(bs.getPosition().blockCenter());
        Rails rails = (Rails) bs.getMaterialData();
        if (!rails.isOnSlope()) vec.setY(vec.getBlockY()); // If rail is a slope, center will be a .5 Y, otherwise .0
        return vec;
    }

    private static final Set<Material> ALL_RAILS = ImmutableSet.<Material>builder().add(Material.RAILS).add(Material.POWERED_RAIL).add(Material.DETECTOR_RAIL).add(Material.POWERED_RAIL).build();
    private static boolean isRails(Material material) {
        return ALL_RAILS.contains(material);
    }

    private boolean isCheckpoint(BlockState blockState) {
        return payload.getDefinition().getCheckpoints().stream().anyMatch(check -> blockState.getPosition().coarseEquals(check));
    }

    // Takes the input of a rail block data, and the face you are entering, and will return the exit face
    // For example, a curved rail and SOUTH, will return null if the rail never connects south, or EAST / WEST
    private static BlockFace getOtherRailSide(Rails rails, BlockFace face) {
        if (!hasConnection(rails, face)) return null; // Rail doesn't connect to previous rail, can't find next
        BlockFace direction = rails.getDirection();
        if (rails.isCurve()) {
            direction = direction.getOppositeFace();
            return blockFaceOfMod(direction.getModX() - face.getModX(), direction.getModY() - face.getModY(), direction.getModZ() - face.getModZ());
        } else return face == direction ? face.getOppositeFace() : direction;
    }

    private static boolean hasConnection(Rails rails, BlockFace face) {
        BlockFace direction = rails.getDirection();
        if (rails.isCurve()) {
            direction = direction.getOppositeFace();
            return face.getModX() == direction.getModX() || face.getModZ() == direction.getModZ();
        } else return face == direction || face.getOppositeFace() == direction;
    }

    private static BlockFace blockFaceOfMod(int x, int y, int z) {
        return Arrays.stream(BlockFace.values())
                .filter(f -> f.getModX() == x && f.getModY() == y && f.getModZ() == z).findFirst().orElse(null);
    }

}
