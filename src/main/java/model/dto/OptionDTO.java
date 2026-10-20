package model.dto;
import model.entity.*;

public class OptionDTO {

    private final int id;
    private final String label;

    public OptionDTO(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

}
