package model.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class FnbItem {
    private int fnbItemId;
    private int categoryId;
    private String categoryName;
    private Integer centreId;
    private String centreName;
    private String itemName = "";
    private String description = "";
    private BigDecimal price;
    private String imageUrl;
    private String status = "ACTIVE"; // ACTIVE / INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public FnbItem() {
    }

    public FnbItem(int fnbItemId, int categoryId, BigDecimal price, String imageUrl, int centreId) {
        this.fnbItemId = fnbItemId;
        this.categoryId = categoryId;
        this.price = price;
        this.imageUrl = imageUrl;
        this.centreId = centreId;
    }

    public int getFnbItemId() {
        return fnbItemId;
    }

    public void setFnbItemId(int fnbItemId) {
        this.fnbItemId = fnbItemId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getCentreId() {
        return centreId;
    }

    public void setCentreId(Integer centreId) {
        this.centreId = centreId;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "FnbItem{" + "fnbItemId=" + fnbItemId + ", categoryId=" + categoryId + ", itemName=" + itemName + ", description=" + description + ", price=" + price + ", imageUrl=" + imageUrl + ", status=" + status + ", centreId=" + centreId + '}';
    }
}
