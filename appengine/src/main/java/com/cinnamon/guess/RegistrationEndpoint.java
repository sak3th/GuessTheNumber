/*
   For step-by-step instructions on connecting your Android application to this backend module,
   see "App Engine Backend with Google Cloud Messaging" template documentation at
   https://github.com/GoogleCloudPlatform/gradle-appengine-templates/tree/master/GcmEndpoints
*/

package com.cinnamon.guess;

import com.cinnamon.guess.model.Player;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Named;

import static com.cinnamon.guess.OfyService.ofy;


/**
 * A registration endpoint class we are exposing for a device's GCM registration id on the backend
 * <p/>
 * For more information, see
 * https://developers.google.com/appengine/docs/java/endpoints/
 * <p/>
 * NOTE: This endpoint does not use any form of authorization or
 * authentication! If this app is deployed, anyone can access this endpoint! If
 * you'd like to add authentication, take a look at the documentation.
 */
@Api(   name = "registration",
        version = "v1",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID},
        audiences = {Constants.ANDROID_AUDIENCE},
        namespace = @ApiNamespace(
                ownerDomain = "guess.cinnamon.com",
                ownerName = "guess.cinnamon.com",
                packagePath = ""
        )
)
public class RegistrationEndpoint {
    private static final Logger log = Logger.getLogger(RegistrationEndpoint.class.getName());

    @ApiMethod(name = "register")
    public void registerDevice(@Named("regId")String regId, User user) throws UnauthorizedException {
        log.info("registerDevice called");
        if (user == null) {
            throw new UnauthorizedException("Unauthorized user!!!");
        }
        Player p = Datastore.findPlayer(user.getEmail());
        if (p == null) {
            p = new Player(user.getEmail(), user.getNickname());
            p.addGcmId(regId);
            ofy().save().entity(p).now();
            log.info("Registering " + user + " with " + regId);
        } else {
            if (! p.isGcmIdRegistered(regId)) { // read if NOT registered
                p.addGcmId(regId);
                ofy().save().entity(p).now();
                log.info("Registering " + user + " with " + regId);
            } else {
                log.info("Device " + regId + " already registered, skipping register");
            }
        }
        try {
            GCMessaging.sendRegistrationComplete(p);
        } catch (IOException e) {
            e.printStackTrace();
            log.severe("sendRegistrationComplete: " + e.getMessage());
        }
    }

    @ApiMethod(name = "unregister")
    public void unregisterDevice(@Named("regId") String regId, User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Unauthorized user!!!");
        }
        Player p = Datastore.findPlayer(user.getEmail());
        if (p.isGcmIdRegistered(regId)) {
            boolean res = p.removeGcmId(regId);
            log.info("Device " + regId + (res? "unregistered!" : "registered!"));
        } else {
            log.info("Device " + regId + " not registered, skipping unregister");
        }
    }

}
