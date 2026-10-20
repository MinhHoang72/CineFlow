package model;

import java.sql.Date;

/**
 * Đại diện cho thông tin một bộ phim trong danh mục phim của CineFlow.
 */
public class Movie {

    private int movieId;
    private String title;
    private String description;
    private int duration;
    private String genre;
    private String director;
    private String cast;
    private String language;
    private Date releaseDate;
    private Date endDate;
    private String posterUrl;
    private String trailerUrl;
    private String status;
    private String ageRating;
    private int distributedCentres;
    private int totalCentres;

    public int getDistributedCentres() {
        return distributedCentres;
    }

    public void setDistributedCentres(int distributedCentres) {
        this.distributedCentres = distributedCentres;
    }

    public int getTotalCentres() {
        return totalCentres;
    }

    public void setTotalCentres(int totalCentres) {
        this.totalCentres = totalCentres;
    }

    public String getDistributionRatio() {
        return distributedCentres + "/" + totalCentres;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public boolean isComingSoon() {
        if (status != null && "COMING_SOON".equalsIgnoreCase(status.trim())) {
            return true;
        }
        if (releaseDate != null) {
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate release = releaseDate.toLocalDate();
            if (release.isAfter(today)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRateable() {
        return !isComingSoon();
    }
}
