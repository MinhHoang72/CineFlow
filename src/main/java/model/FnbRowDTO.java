/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.math.BigDecimal;

/**
 *
 * @author ADM
 */
//list -1 dòng 1 item hoặc 1 combo
public class FnbRowDTO {

    private String kind;
    private int id; //id items or id combo
    private String name;
    private String categoryName;
    private BigDecimal price;
    private String status;
    private String imageUrl;
    private int centreId;
    private String centreName;

    public FnbRowDTO() {
    }

    public FnbRowDTO(String kind, int id, String name, String categoryName, BigDecimal price, String status, String imageUrl, int centreId, String centreName) {
        this.kind = kind;
        this.id = id;
        this.name = name;
        this.categoryName = categoryName;
        this.price = price;
        this.status = status;
        this.imageUrl = imageUrl;
        this.centreId = centreId;
        this.centreName = centreName;
    }

    public String getKind() {
        return kind;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getCentreId() {
        return centreId;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }
    //check combo

    public boolean isCombo() {
        return "COMBO".equals(kind);
    }
    //check có đang bán k 
   public boolean  isActive(){
   return  "ACTIVE".equals(status);
   }
}
