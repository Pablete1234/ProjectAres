package tc.oc.api.tourney;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import tc.oc.api.docs.Tournament;
import tc.oc.api.docs.team;


/**
 * Utility class for creating Tournament documents from simple Json
 */
class LocalTournaments {

    // Simple Json format for defining Tournament s
    public static class SimpleTournament {
        private String id;
        private String name;
        private String[] accepted_teams;
        private int min_players_per_match;
        private int max_players_per_match;

        public LocalTournament toLocalTournament() {
            return new LocalTournament(id, name, Arrays.stream(accepted_teams).map(LocalTeams.TeamId::new)
                    .collect(Collectors.toList()), min_players_per_match, max_players_per_match);
        }
    }

    // Basic implementation of a Tournament for a local enviorment, containing the minimum required data
    public static class LocalTournament implements Tournament {
        private String id;
        private String name;
        private List<team.Id> accepted_teams;
        private int min_players_per_match;
        private int max_players_per_match;

        LocalTournament(String id, String name, List<team.Id> accepted_teams, int min_players, int max_players) {
            this.id = id;
            this.name = name;
            this.accepted_teams = accepted_teams;
            this.min_players_per_match = min_players;
            this.max_players_per_match = max_players;
        }

        public String _id() {
            return id;
        }

        public String name() {
            return name;
        }

        // Start and end are never used, started at EPOCH and ended at MAX seem good enough defaults
        public Instant start() {
            return Instant.EPOCH;
        }

        public Instant end() {
            return Instant.MAX;
        }

        public int min_players_per_match() {
            return min_players_per_match;
        }

        public int max_players_per_match() {
            return max_players_per_match;
        }

        public List<team.Id> accepted_teams() {
            return accepted_teams;
        }

        public List<MapClassification> map_classifications() {
            return Collections.emptyList();
        }
    }
}
