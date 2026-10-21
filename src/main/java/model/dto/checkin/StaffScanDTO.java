package model.dto.checkin;

public class StaffScanDTO {

    private final String staffName;
    private final int success;
    private final int rejected;

    public StaffScanDTO(String staffName, int success, int rejected) {
        this.staffName = staffName;
        this.success = success;
        this.rejected = rejected;
    }

    public String getStaffName() { return staffName; }
    public int getSuccess() { return success; }
    public int getRejected() { return rejected; }
}
