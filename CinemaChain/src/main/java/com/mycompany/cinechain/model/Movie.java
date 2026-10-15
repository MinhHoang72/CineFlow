package com.mycompany.cinechain.model;

public class Movie {

    private final int id, duration;
    private final String title, genre, ageRating;

    public Movie(int id, String title, String genre, int duration, String ageRating) {
        this.id = id;
        this.title = title;
        this.genre = genre;
        this.duration = duration;
        this.ageRating = ageRating;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getGenre() { return genre; }
    public int getDuration() { return duration; }
    public String getAgeRating() { return ageRating; }
}
