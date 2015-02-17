package com.cinnamon.guess;

import com.cinnamon.guess.model.Match;
import com.cinnamon.guess.model.Player;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.cinnamon.guess.OfyService.ofy;


@Api(
        name = "matchApi",
        version = "v1",
        resource = "match",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
        audiences = {Constants.ANDROID_AUDIENCE},
        namespace = @ApiNamespace(
                ownerDomain = "guess.cinnamon.com",
                ownerName = "guess.cinnamon.com",
                packagePath = ""
        )
)
public class MatchEndpoint {

    private static final Logger logger = Logger.getLogger(MatchEndpoint.class.getName());

    private ArrayList<Match> matches; //  TODO objectify this

    public MatchEndpoint() {
        matches = new ArrayList<Match>();
        matches.add(new Match());

    }

    @ApiMethod(name = "createMatch", httpMethod = HttpMethod.POST)
    public Match createMatch(User user) throws NotFoundException, IOException {
        List<Player> players = ofy().load().type(Player.class).list();
        if (players == null || players.size() < 2) {
            throw new NotFoundException("Embarrassed: no other players!");
        } else {
            Match match = new Match();
            match.addPlayer(user.getEmail());
            Player random = null;
            do {
                random = players.get(new Random().nextInt(players.size()));
            } while (random.getEmail().equals(user.getEmail()));
            logger.info("selected random " + random);
            match.addPlayer(random.getEmail());
            match.setTurnOf(new Random().nextInt(2) == 1 ? user.getEmail() : random.getEmail());
            ofy().save().entity(match).now();
            GCMessaging.sendNewMatch(match, random);
            return match;
        }
    }

    @ApiMethod(name = "createMatchWith", httpMethod = HttpMethod.POST)
    public Match createMatchWith(@Named("email") String email, User user) throws NotFoundException, IOException {
        Player player = Datastore.findPlayer(email);
        if (player == null) {
            throw new NotFoundException("Embarrassed: player does not exist!");
        } else {
            Match match = new Match();
            match.addPlayer(user.getEmail());
            match.addPlayer(player.getEmail()); //assuming that the other player has not uninstalled
            match.setTurnOf(new Random().nextInt(1) == 1 ? user.getEmail() : player.getEmail());
            ofy().save().entity(match).now();
            GCMessaging.sendNewMatch(match, player);
            return match;
        }
    }


    @ApiMethod(name = "getMatch")
    public Match getMatch(@Named("id") Long id, User user) throws NotFoundException, UnauthorizedException {
        logger.info("Calling getMatch method");
        Match match = ofy().load().type(Match.class).id(id).now();
        if (match == null) {
            throw new NotFoundException("Oops: Match not found!");
        }
        if (! match.getPlayers().contains(user.getEmail())) {
            throw new UnauthorizedException("Sorry: you are not authorized!");
        }
        return match;
    }

    @ApiMethod(name = "getMatches")
    public List<Match> getMatches(User user) {
        logger.info("getMatches method called by " + user);
        List<Match> matches =
                ofy().load().type(Match.class).filter("players", user.getEmail()).list();
        if (matches != null) {
            logger.info("matches size " + matches.size());
        } else {
            logger.info("matches is null");
        }
        return matches;
    }

    @ApiMethod(name = "addMove", httpMethod = HttpMethod.PUT)
    public Match addMove(@Named("id") Long id, @Named("guess") int guess, User user)
            throws NotFoundException, UnauthorizedException, IOException {
        Match match = ofy().load().type(Match.class).id(id).now();
        if (match == null) {
            throw new NotFoundException("Oops: Match not found!");
        }
        if (! match.getPlayers().contains(user.getEmail())) {
            throw new UnauthorizedException("Sorry: you are not authorized!");
        }
        if (guess == -1) {
            if (match.addWrongGuessMove(user.getEmail())) {
                ofy().save().entity(match).now();
            } else throw new UnauthorizedException("Sorry: you are not authorized!");
        } else {
            if (match.addSelectNumMove(guess, user.getEmail())) {
                ofy().save().entity(match).now();
                Player p = Datastore.findPlayer(match.getTurnOf());
                GCMessaging.sendMatchUpdate(match, p);
            } else throw new UnauthorizedException("Sorry: you are not authorized!");
        }
        return match;
    }

    @ApiMethod(name = "completeMatch", httpMethod = HttpMethod.PUT)
    public Match completeMatch(@Named("id") Long id, User user)
            throws NotFoundException, UnauthorizedException, IOException {
        Match match = ofy().load().type(Match.class).id(id).now();
        if (match == null) {
            throw new NotFoundException("Oops: Match not found!");
        }
        if (! match.getPlayers().contains(user.getEmail())) {
            throw new UnauthorizedException("Sorry: you are not authorized!");
        }
        if (match.complete(user.getEmail())) {
            ofy().save().entity(match).now();
            Player p = Datastore.findPlayer(match.getTurnOf());
            GCMessaging.sendMatchUpdate(match, p);
        }
        return match;
    }

}