package tc.oc.pgm.kits;

import tc.oc.pgm.features.FeatureDefinition;
import tc.oc.pgm.features.FeatureInfo;
import tc.oc.pgm.match.MatchPlayer;

@FeatureInfo(name = "kit")
public interface Kit extends FeatureDefinition {
    /**
     * Apply this kit to the given player. If force is true, the player's state is made
     * to match the kit as strictly as possible, otherwise the kit may be given to the
     * player in a way that is more in their best interest. Subclasses will interpret
     * these concepts in their own way.
     *
     * A mutable List must be given, to which the Kit may add ItemStacks that could not
     * be applied normally, because the player's inventory was full. These stacks will
     * be given to the player using the natural give algorithm after ALL kits have been
     * applied. This phase must be deferred in this way so that overflow from one kit
     * does not displace stacks in another kit applied simultaneously. In this way, the
     * number of stacks that go to their proper slots is maximized.
     */
    void apply(MatchPlayer player, boolean force, ItemKitApplicator items);

    default void remove(MatchPlayer player) {
        throw new UnsupportedOperationException(this + " is not removable");
    }

    default boolean isRemovable() {
        return false;
    }

    abstract class Impl extends FeatureDefinition.Impl implements Kit {}
}
