package com.cinnamon.guess;


import com.cinnamon.guess.model.Match;
import com.cinnamon.guess.model.Player;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.logging.Logger;

public class GCMessaging {
    private static final Logger log = Logger.getLogger(RegistrationEndpoint.class.getName());

    public static void sendRegistrationComplete(Player p) throws IOException {
        Message msg = new Message.Builder().addData("message", "Your device is registered").build();
        sendUnicast(msg, p.getGcmIds().get(0), p.getEmail());
    }

    public static void sendNewMatch(Match match, Player p) throws IOException {
        Message msg = new Message.Builder()
                .addData(Constants.KEY_MSG, Constants.MSG_NEW_MATCH)
                .addData(Constants.KEY_MATCH, match.getId().toString())
                .build();
        sendUnicast(msg, p.getGcmIds().get(0), p.getEmail());
    }

    public static void sendMatchUpdate(Match match, Player p) throws IOException {
        Message msg = new Message.Builder()
                .addData(Constants.KEY_MSG, Constants.MSG_MATCH_UPDATE)
                .addData(Constants.KEY_MATCH, match.getId().toString())
                .build();
        sendUnicast(msg, p.getGcmIds().get(0), p.getEmail());
    }

    private static void sendUnicast(Message msg, String regId, String email) throws IOException {
        Sender sender = new Sender(Constants.KEY_GCM_API);
        log.info("sending msg to " + regId);
        Result result = sender.send(msg, regId, 5);
        if (result.getMessageId() != null) {
            log.info("Message sent to " + email);
            String canonicalRegId = result.getCanonicalRegistrationId();
            if (canonicalRegId != null) {
                // if the regId changed, we have to update the datastore
                // FIXME
                /*log.info("Registration Id changed for " + record.getRegId() + " updating to " + canonicalRegId);
                record.setRegId(canonicalRegId);
                ofy().save().entity(record).now();*/
            }
        } else {
            String error = result.getErrorCodeName();
            if (error.equals(com.google.android.gcm.server.Constants.ERROR_NOT_REGISTERED)) {
                //log.warning("Registration Id " + record.getRegId() + " no longer registered with GCM, removing from datastore");
                log.warning("Registration Id " + email + " no longer registered with GCM, removing from datastore");
                // if the device is no longer registered with Gcm, remove it from the datastore
                // FIXME
                /*ofy().delete().entity(record).now();*/
            } else {
                log.warning("Error when sending message : " + error);
            }
        }
    }
}
