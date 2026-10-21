package model.dto.centre;

import model.entity.Centre;

/**
 * DTO phục vụ màn hình Quản lý Cụm rạp (Admin: /admin/centres).
 * Theo CineFlow Architecture Standard: Centre + managerName + hallCount + totalSeats.
 */
public class CentreManagementDTO extends Centre {

    private String managerName;
    private int hallCount;
    private int totalSeats;

    public CentreManagementDTO() {
        super();
    }

    public CentreManagementDTO(Centre centre, String managerName, int hallCount, int totalSeats) {
        if (centre != null) {
            setCentreId(centre.getCentreId());
            setCentreName(centre.getCentreName());
            setAddress(centre.getAddress());
            setCity(centre.getCity());
            setPhone(centre.getPhone());
            setStatus(centre.getStatus());
        }
        this.managerName = managerName;
        this.hallCount = hallCount;
        this.totalSeats = totalSeats;
    }

    @Override
    public String getManagerName() {
        return managerName;
    }

    @Override
    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    @Override
    public int getHallCount() {
        return hallCount;
    }

    @Override
    public void setHallCount(int hallCount) {
        this.hallCount = hallCount;
        super.setHallCount(hallCount);
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
        super.setSeatCount(totalSeats);
    }

    @Override
    public int getSeatCount() {
        return totalSeats;
    }

    @Override
    public void setSeatCount(int seatCount) {
        this.totalSeats = seatCount;
        super.setSeatCount(seatCount);
    }
}
