package com.cinnamon.guess.model;


import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Match {

    public static final int NO_SELECTION = -1;

    private static final int MAX_PLAYERS = 2;

    // TODO consider adding a state that allows storing the game after completion
    public enum State {
        INIT,
        AUTO_MATCHING,
        ACTIVE,
        COMPLETE,
        CANCELLED,
        EXPIRED
    };

    State state;

    @Id Long id; // unique id of the match

    @Index
    List<String> players;

    String turnOf; // unique id of the player

    int guess, newGuess;

    public Match() {
        players = new ArrayList<String>();
        state = State.INIT;
        guess = newGuess = NO_SELECTION;
    }

    public Long getId() {
        return id;
    }

    public List<String> getPlayers() {
        return players;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getTurnOf() {
        return turnOf;
    }

    public void setTurnOf(String email) {
        turnOf = email;
    }

    public int getGuess() {
        return guess;
    }

    public void setGuess(int g) {
        guess = g;
    }

    public int getNewGuess() {
        return newGuess;
    }

    public void setNewGuess(int guess) {
        newGuess = guess;
    }

    public boolean addPlayer(String email) {
        if (players.size() == MAX_PLAYERS) return false;
        return players.add(email);
    }

    public boolean isTurnToSelect() {
        return guess == NO_SELECTION;
    }

    public boolean addWrongGuessMove(String player) {
        if (! player.equals(turnOf)) {
            return false;
        }
        guess = newGuess = NO_SELECTION;
        return true;
    }

    public boolean addSelectNumMove(int newGuess, String player) {
        if (! player.equals(turnOf)) {
            return false;
        }
        guess = newGuess;
        int index = players.indexOf(player);
        if (++index == players.size()) {
            index = 0;
        }
        turnOf = players.get(index);
        return true;
    }

    public boolean complete(String player) {
        if (! player.equals(turnOf)) {
            return false;
        }
        state = State.COMPLETE;
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match)) return false;
        Match match = (Match) o;
        return (!id.equals(match.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Match{" +
                "state=" + state +
                ", id=" + id +
                ", players=" + players +
                ", turnOf='" + turnOf + '\'' +
                ", guess=" + guess +
                '}';
    }
}
