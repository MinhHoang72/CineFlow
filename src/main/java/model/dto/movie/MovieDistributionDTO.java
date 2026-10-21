package model.dto.movie;

import model.entity.Movie;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO phục vụ phân phối phim tới các cụm rạp (/admin/distribution).
 * Theo CineFlow Architecture Standard: Movie + distributedCentres + totalCentres.
 */
public class MovieDistributionDTO extends Movie {

    private int distributedCentres;
    private int totalCentres;
    private List<Integer> assignedCentreIds = new ArrayList<>();

    public MovieDistributionDTO() {
        super();
    }

    public MovieDistributionDTO(Movie movie, int distributedCentres, int totalCentres) {
        if (movie != null) {
            setMovieId(movie.getMovieId());
            setTitle(movie.getTitle());
            setGenre(movie.getGenre());
            setDuration(movie.getDuration());
            setReleaseDate(movie.getReleaseDate());
            setEndDate(movie.getEndDate());
            setPosterUrl(movie.getPosterUrl());
            setTrailerUrl(movie.getTrailerUrl());
            setDescription(movie.getDescription());
            setStatus(movie.getStatus());
            setAgeRating(movie.getAgeRating());
            setDirector(movie.getDirector());
            setCast(movie.getCast());
            setLanguage(movie.getLanguage());
        }
        this.distributedCentres = distributedCentres;
        this.totalCentres = totalCentres;
    }

    @Override
    public int getDistributedCentres() {
        return distributedCentres;
    }

    @Override
    public void setDistributedCentres(int distributedCentres) {
        this.distributedCentres = distributedCentres;
    }

    @Override
    public int getTotalCentres() {
        return totalCentres;
    }

    @Override
    public void setTotalCentres(int totalCentres) {
        this.totalCentres = totalCentres;
    }

    public List<Integer> getAssignedCentreIds() {
        return assignedCentreIds;
    }

    public void setAssignedCentreIds(List<Integer> assignedCentreIds) {
        this.assignedCentreIds = assignedCentreIds != null ? assignedCentreIds : new ArrayList<>();
    }
}
