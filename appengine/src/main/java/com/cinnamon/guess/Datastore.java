package com.cinnamon.guess;


import com.cinnamon.guess.model.Player;

import static com.cinnamon.guess.OfyService.ofy;

public class Datastore {

    public static Player findPlayer(String email) {
        return ofy().load().type(Player.class).id(email).now();
    }
}
