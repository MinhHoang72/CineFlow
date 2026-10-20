package model.entity;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FnbCombo {
    private int comboId;
    private Integer centreId;
    private String centreName;
    private String comboName = "";
    private String description = "";
    private BigDecimal price;
    private String imageUrl;
    private String status = "ACTIVE"; // ACTIVE / INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<ComboLine> lines = new ArrayList<>();

    public FnbCombo() {
    }

    public FnbCombo(int comboId, BigDecimal price, String imageUrl, int centreId) {
        this.comboId = comboId;
        this.price = price;
        this.imageUrl = imageUrl;
        this.centreId = centreId;
    }

    public int getComboId() {
        return comboId;
    }

    public void setComboId(int comboId) {
        this.comboId = comboId;
    }

    public Integer getCentreId() {
        return centreId;
    }

    public void setCentreId(Integer centreId) {
        this.centreId = centreId;
    }

    public String getCentreName() {
        return centreName;
    }

    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    public String getComboName() {
        return comboName;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
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

    public List<ComboLine> getLines() {
        return lines;
    }

    public void setLines(List<ComboLine> lines) {
        this.lines = lines;
    }

    @Override
    public String toString() {
        return "FnbCombo{" + "comboId=" + comboId + ", comboName=" + comboName + ", description=" + description + ", price=" + price + ", imageUrl=" + imageUrl + ", status=" + status + ", centreId=" + centreId + ", lines=" + lines + '}';
    }
}
