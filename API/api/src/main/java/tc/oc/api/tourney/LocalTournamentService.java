package tc.oc.api.tourney;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import tc.oc.api.docs.Entrant;
import tc.oc.api.docs.PlayerId;
import tc.oc.api.docs.Tournament;
import tc.oc.api.exceptions.NotFound;
import tc.oc.api.http.HttpClient;
import tc.oc.api.message.types.FindMultiResponse;
import tc.oc.api.message.types.FindRequest;
import tc.oc.api.model.NullQueryService;
import tc.oc.commons.core.concurrent.FutureUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalTournamentService extends NullQueryService<Tournament> implements TournamentService {

    private final HttpClient http;
    private final TourneyConfiguration config;

    @Inject LocalTournamentService(HttpClient http, TourneyConfiguration config) {
        this.http = checkNotNull(http, "http");
        this.config = checkNotNull(config, "config");
    }

    @Override
    public ListenableFuture<FindMultiResponse<Tournament>> find(FindRequest<Tournament> request) {
        if (config.tourneyUrl() == null) return Futures.immediateFuture(Collections::emptyList);
        return FutureUtils.mapSync(
                http.get(config.tourneyUrl(), LocalTournaments.SimpleTournament.class),
                (LocalTournaments.SimpleTournament tm) -> {
                    List<Tournament> tmList = Collections.singletonList(tm.toLocalTournament());
                    return (FindMultiResponse<Tournament>) () -> tmList;
                }
        );
    }

    @Override
    public ListenableFuture<Entrant> entrant(String tournamentId, String teamId) {
        String url = config.entrantUrl(tournamentId, teamId);
        if (url == null) Futures.immediateFailedFuture(new NotFound());
        return FutureUtils.mapSync(http.get(url, LocalTeams.SimpleTeam.class), LocalTeams.SimpleTeam::toLocalTeam);
    }

    // This is never used so just don't do anything
    @Override
    public ListenableFuture<Entrant> entrantByTeamName(String tournamentId, String teamName) {
        return Futures.immediateFailedFuture(new NotFound());
    }

    // Only used to default to your own team if you use /tm roster without a team as argument, can stay like this
    @Override
    public ListenableFuture<Entrant> entrantByMember(String tournamentId, PlayerId playerId) {
        return Futures.immediateFailedFuture(new NotFound());
    }

    // This is not needed in a local environment
    @Override
    public ListenableFuture<RecordMatchResponse> recordMatch(Tournament tournament, String matchId) {
        return Futures.immediateFailedFuture(new NotFound());
    }

}
