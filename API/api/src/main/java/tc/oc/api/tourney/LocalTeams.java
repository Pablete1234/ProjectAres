package tc.oc.api.tourney;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

import tc.oc.api.docs.Entrant;
import tc.oc.api.docs.PlayerId;
import tc.oc.api.docs.SimplePlayerId;
import tc.oc.api.docs.team;
import tc.oc.api.docs.virtual.MatchDoc;

/**
 * Utility class for creating Team documents from simple Json
 */
class LocalTeams {

    // Simple Json format for defining Team s
    public static class SimpleTeam {
        protected String name;
        protected SimplePlayer[] members;

        public static class SimplePlayer {
            protected String uuid;
            protected String username;
        }

        public LocalTeam toLocalTeam() {
            return new LocalTeam(name, Arrays.stream(members).map(member ->
                    new SimplePlayerId(member.uuid, member.uuid, member.username)).collect(Collectors.toList()));
        }
    }

    // Represents a team.Id, used for LocalTournament, it holds a list of TeamId s
    public static class TeamId implements team.Id {
        protected String name;

        TeamId(String name) {
            this.name = name;
        }

        @Override
        public String _id() {
            return "_" + name_normalized();
        }

        @Override
        @Nonnull public String name() {
            return name;
        }

        @Override
        @Nonnull public String name_normalized() {
            return TeamUtils.normalizeName(name);
        }
    }

    /**
     * Full local team, it implements both team.Team and Entrant.
     * An entrant only needs a team.Team, list of members, and list of matches, so merging them into one is easy
     */
    public static class LocalTeam extends TeamId implements team.Team, Entrant {
        private List<PlayerId> players;

        LocalTeam(String name, List<PlayerId> players) {
            super(name);
            this.players = players;
        }

        // Team
        @Override
        @Nonnull public PlayerId leader() {
            return players.get(0);
        }

        // Both
        @Override
        @Nonnull public List<PlayerId> members() {
            return players;
        }

        // Entrant
        @Override
        @Nonnull public team.Team team() {
            return this;
        }

        @Nonnull
        @Override
        public List<MatchDoc> matches() {
            return Collections.emptyList();
        }
    }
}

