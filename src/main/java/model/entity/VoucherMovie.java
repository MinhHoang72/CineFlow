package model.entity;

import java.sql.Timestamp;

public class VoucherMovie {
    private int voucherMovieId;
    private int voucherId;
    private int movieId;
    private String movieTitle;
    private Timestamp createdAt;

    public VoucherMovie() {
    }

    public VoucherMovie(int voucherId, int movieId) {
        this.voucherId = voucherId;
        this.movieId = movieId;
    }

    public int getVoucherMovieId() {
        return voucherMovieId;
    }

    public void setVoucherMovieId(int voucherMovieId) {
        this.voucherMovieId = voucherMovieId;
    }

    public int getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(int voucherId) {
        this.voucherId = voucherId;
    }

    public int getMovieId() {
        return movieId;
    }

    public void setMovieId(int movieId) {
        this.movieId = movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
