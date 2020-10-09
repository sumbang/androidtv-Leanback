package com.example.android.wouritv.config;

public class CurrentUser {

    private String id; private String username; private int pub;

    private static CurrentUser currentUser;

    public static CurrentUser getInstance(){
        if(currentUser == null) {
            currentUser = new CurrentUser();
        }
        return currentUser;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public static CurrentUser getCurrentUser() {
        return currentUser;
    }

    public int getPub() {
        return pub;
    }

    public void setPub(int pub) {
        this.pub = pub;
    }
}
