/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

/**
 *
 * @author ADM
 */
public class StockRowDTO {
    private int fnbItemId;
    private String itemName;
    private String categoryName;
    private boolean available;

    public StockRowDTO() {
    }

    public StockRowDTO(int fnbItemId, String itemName, String categoryName, boolean available) {
        this.fnbItemId = fnbItemId;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.available = available;
    }

    public int getFnbItemId() {
        return fnbItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setFnbItemId(int fnbItemId) {
        this.fnbItemId = fnbItemId;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {
        return "StockRowDTO{" + "fnbItemId=" + fnbItemId + ", itemName=" + itemName + ", categoryName=" + categoryName + ", available=" + available + '}';
    }
    
}
