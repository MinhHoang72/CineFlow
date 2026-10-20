package model.dto;
import model.entity.*;

import java.math.BigDecimal;

public class FnbRowDTO {

    private String kind;
    private int id;
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

    public boolean isCombo() {
        return "COMBO".equals(kind);
    }
   public boolean  isActive(){
   return  "ACTIVE".equals(status);
   }
}
