package com.cinnamon.guess.model;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Player {
    @Id String email;
    String nickName;
    ArrayList<String> gcmIds;

    // TODO store achievements, highest score

    public Player() {
        gcmIds = new ArrayList<String>();
    }

    public Player(String email, String nickName) {
        this.email = email;
        this.nickName = nickName;
        gcmIds = new ArrayList<String>();
    }

    public String getEmail() {
        return email;
    }

    public String getNickName() {
        return nickName;
    }

    public List<String> getGcmIds() {
        if (gcmIds.size() == 0) {
            return null;
        }
        return gcmIds;
    }

    public boolean isGcmIdRegistered(String gcmId) {
        if (gcmIds.size() == 0 || gcmId == null || gcmId.length() == 0) {
            return false;
        }
        for (String id : gcmIds) {
            if (gcmId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    public void addGcmId(String gcmId) {
        gcmIds.add(gcmId);
    }

    public boolean removeGcmId(String gcmId) {
        return gcmIds.remove(gcmId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return (email.equals(player.email));
    }

    @Override
    public int hashCode() {
        return email.hashCode();
    }

    @Override
    public String toString() {
        return "Player{" +
                "email='" + email + '\'' +
                ", nickName='" + nickName + '\'' +
                ", gcmIds=" + gcmIds +
                '}';
    }

}
