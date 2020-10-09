package com.example.android.wouritv.model;

public class Contenu {

    private int id;
    private String titre;
    private String video;
    private String provider;

    public Contenu(){}

    public int getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getTitre() {
        return titre;
    }

    public String getVideo() {
        return video;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setVideo(String video) {
        this.video = video;
    }

}
