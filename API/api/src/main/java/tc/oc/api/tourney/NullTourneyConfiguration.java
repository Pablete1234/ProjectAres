package tc.oc.api.tourney;

import javax.annotation.Nullable;

public class NullTourneyConfiguration implements TourneyConfiguration {

    @Override
    public @Nullable
    String tourneyUrl() {
        return null;
    }

    @Override
    public @Nullable String entrantUrl(String tourneyId, String teamId) {
        return null;
    }

}
