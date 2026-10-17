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
public class FnbItem {
    private int fnbItemId;
    private int categoryId;
    private String itemName = "";
    private String description = "";
    private BigDecimal price;
    private String imageUrl;
    private String status = "ACTIVE";
    private int centreId;

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

    public int getCategoryId() {
        return categoryId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getStatus() {
        return status;
    }

    public int getCentreId() {
        return centreId;
    }

    public void setFnbItemId(int fnbItemId) {
        this.fnbItemId = fnbItemId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCentreId(int centreId) {
        this.centreId = centreId;
    }

    @Override
    public String toString() {
        return "FnbItem{" + "fnbItemId=" + fnbItemId + ", categoryId=" + categoryId + ", itemName=" + itemName + ", description=" + description + ", price=" + price + ", imageUrl=" + imageUrl + ", status=" + status + ", centreId=" + centreId + '}';
    }
    
}
