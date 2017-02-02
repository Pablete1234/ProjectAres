package tc.oc.api.minecraft.tourney;

import javax.annotation.Nullable;
import javax.inject.Inject;

import tc.oc.api.tourney.TourneyConfiguration;
import tc.oc.minecraft.api.configuration.Configuration;
import tc.oc.minecraft.api.configuration.ConfigurationSection;

import static com.google.common.base.Preconditions.checkNotNull;

public class TourneyConfigurationImpl implements TourneyConfiguration {

    private static final String SECTION = "tourney";

    private static final String TOURNEY_URL = "tourney-url";
    private static final String ENTRANT_URL = "entrant-url";

    private static final String TOURNAMENT_REPLACE = "{tournament-id}";
    private static final String TEAM_REPLACE = "{team-id}";

    private final ConfigurationSection config;

    @Inject
    public TourneyConfigurationImpl(Configuration config) {
        this.config = checkNotNull(config.getSection(SECTION));
    }

    @Override
    public @Nullable String tourneyUrl() {
        return config.getString(TOURNEY_URL);
    }

    @Override
    public @Nullable String entrantUrl(String tourneyId, String teamId) {
        return config.getString(ENTRANT_URL) == null ? null :
                config.getString(ENTRANT_URL, "").replace(TOURNAMENT_REPLACE, tourneyId).replace(TEAM_REPLACE, teamId);
    }

}
