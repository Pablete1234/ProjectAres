package tc.oc.api.tourney;

public interface TourneyConfiguration {

    String tourneyUrl();

    String entrantUrl(String tourneyId, String teamId);

}
