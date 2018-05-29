package com.techmantra.collisiondetection.Models;

import android.location.Location;

/**
 * Created by test on 4/18/2018.
 */

public class User {
    public String id;
    public String userName ;
    public String userEmail;
    public Location userLocation ;
    public float speed ;
    public double latitude;
    public double longitude;

    public User(String id, String userName, String userEmail) {
        this.id = id;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    public User(String id, float speed, double latitude, double longitude) {
        this.id = id;
        this.speed = speed;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public User (){
    }

    public User(String id, String userName, Location userLocation, float speed) {
        this.id = id;
        this.userName = userName;
        this.userLocation = userLocation;
        this.speed = speed;
    }

    public User(float speed , Location userLocation) {
        this.speed = speed;
        this.userLocation = userLocation;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Location getUserLocation() {
        return userLocation;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
