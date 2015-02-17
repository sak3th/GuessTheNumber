package com.cinnamon.guess;

import com.cinnamon.guess.model.Player;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import java.util.logging.Logger;

import javax.inject.Named;

import static com.cinnamon.guess.OfyService.ofy;


@Api(   name = "playerApi",
        version = "v1",
        resource = "player",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
        audiences = {Constants.ANDROID_AUDIENCE},
        namespace = @ApiNamespace(
                ownerDomain = "guess.cinnamon.com",
                ownerName = "guess.cinnamon.com",
                packagePath = ""
        )
)
public class PlayerEndpoint {
    private static final Logger logger = Logger.getLogger(PlayerEndpoint.class.getName());

    @ApiMethod(name = "getPlayer")
    public Player getPlayer(@Named("email") String email) {
        logger.info("Calling getPlayer method");
        return Datastore.findPlayer(email);
    }

    @ApiMethod(name = "registerPlayer")
    public Player registerPlayer(User user) throws UnauthorizedException {
        // TODO: Implement this function
        logger.info("Calling registerPlayer method by user " + user);
        if (user == null) {
            throw new UnauthorizedException("Unauthorized user!!!");
        }

        Player p = Datastore.findPlayer(user.getEmail());
        if (p == null) {
            p = new Player(user.getEmail(), user.getNickname());
            ofy().save().entity(p).now();
        } else {
            logger.info(user + " is already registered");
        }
        return p;
    }
}
