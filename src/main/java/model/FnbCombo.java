package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FnbCombo {
    private int comboId;
    private String comboName = "";
    private String description = "";
    private BigDecimal price;
    private String imageUrl;
    private String status = "ACTIVE";
    private int centreId;
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

    public String getComboName() {
        return comboName;
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

    public List<ComboLine> getLines() {
        return lines;
    }

    public void setComboId(int comboId) {
        this.comboId = comboId;
    }

    public void setComboName(String comboName) {
        this.comboName = comboName;
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

    public void setLines(List<ComboLine> lines) {
        this.lines = lines;
    }

    @Override
    public String toString() {
        return "FnbCombo{" + "comboId=" + comboId + ", comboName=" + comboName + ", description=" + description + ", price=" + price + ", imageUrl=" + imageUrl + ", status=" + status + ", centreId=" + centreId + ", lines=" + lines + '}';
    }
    
}
