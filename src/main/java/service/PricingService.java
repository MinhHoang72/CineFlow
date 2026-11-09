package service;

import dao.CentrePricingDAO;
import dao.SurchargeDAO;
import dao.TimeSlotPricingDAO;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class PricingService {
    public static String policyType(String path) {
        if(path.endsWith("centre-surcharges"))return "CENTRE";
        if(path.endsWith("hall-surcharges"))return "HALL";
        if(path.endsWith("movie-surcharges"))return "MOVIE";
        if(path.endsWith("time-slots"))return "TIME";
        if(path.endsWith("seat-surcharges"))return "SEAT";
        throw new IllegalArgumentException("Loại phụ thu không hợp lệ.");
    }

    public void savePolicy(jakarta.servlet.http.HttpServletRequest request) throws java.sql.SQLException {
        String type=request.getParameter("type");
        if(!java.util.Set.of("CENTRE","HALL","SEAT","MOVIE","TIME").contains(type==null?"":type))throw new IllegalArgumentException("Loại phụ thu không hợp lệ.");
        int centre=ShowtimeService.requireCentreId(request),user=ShowtimeService.requireUserId(request);
        String target=request.getParameter("target"),name=request.getParameter("name");
        if(name==null||name.isBlank()||name.trim().length()>100)throw new IllegalArgumentException("Tên phụ thu phải có từ 1 đến 100 ký tự.");
        java.sql.Time start=null,end=null;
        if("TIME".equals(type)){
            start=java.sql.Time.valueOf(java.time.LocalTime.parse(request.getParameter("startTime")));
            end=java.sql.Time.valueOf(java.time.LocalTime.parse(request.getParameter("endTime")));
            if(!start.before(end))throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu. Khung qua đêm cần tách thành hai khung.");
            target=start.toString()+"-"+end.toString();
        }else if(!surchargeDAO.targets(centre,type).containsKey(target))throw new IllegalArgumentException("Đối tượng không thuộc cụm rạp của bạn.");
        String mode=request.getParameter("effectiveMode");
        if(!"now".equals(mode)&&!"scheduled".equals(mode))throw new IllegalArgumentException("Chọn thời điểm áp dụng.");
        java.sql.Timestamp from="scheduled".equals(mode)?java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(request.getParameter("effectiveFrom"))):null;
        String id=request.getParameter("id");
        surchargeDAO.savePolicy(centre,user,type,target,name.trim(),java.math.BigDecimal.valueOf(parseVnd(request.getParameter("amount"))),from,start,end,id==null||id.isBlank()?null:Integer.valueOf(id));
    }
    /** VND input accepts plain digits or Vietnamese thousands separators. */
    public static double parseVnd(String value) {
        String text = value == null ? "" : value.trim();
        if (!text.matches("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+)"))
            throw new IllegalArgumentException("Số tiền phải là số nguyên VNĐ không âm.");
        java.math.BigDecimal amount = new java.math.BigDecimal(text.replace(".", ""));
        if (amount.compareTo(new java.math.BigDecimal("9999999999")) > 0)
            throw new IllegalArgumentException("Số tiền vượt giới hạn cho phép.");
        return amount.doubleValue();
    }


    private CentrePricingDAO centrePricingDAO;
    private TimeSlotPricingDAO timeSlotPricingDAO;
    private SurchargeDAO surchargeDAO;


    public PricingService() {

        centrePricingDAO =
                new CentrePricingDAO();

        timeSlotPricingDAO =
                new TimeSlotPricingDAO();

        surchargeDAO =
                new SurchargeDAO();
    }


    /** Legacy callers use a 2D hall; pass hallType explicitly for actual bookings. */
    public double calculateTicketPrice(int centreId, LocalDateTime showtime, String seatType) {
        return calculateTicketPrice(centreId, showtime, seatType, "2D");
    }

    public double calculateTicketPrice(
            int centreId,
            LocalDateTime showtime,
            String seatType,
            String hallType) {

        return calculateTicketPrice(centreId,0,showtime,seatType,hallType);
    }

    /** Live preview only. Checkout uses ShowtimeSeatPrices, never mutable policies. */
    public double calculateTicketPrice(int centreId,int movieId,LocalDateTime showtime,String seatType,String hallType) {
        try(java.sql.Connection c=utils.DBContext.getConnection();java.sql.PreparedStatement ps=c.prepareStatement(
            "SELECT p.BasePrice+p.CentreAmount+p.HallAmount+p.MovieAmount+p.TimeAmount+ISNULL(sc.Amount,0) FROM dbo.CfPriceAt(?,?,?,?) p OUTER APPLY (SELECT TOP 1 Amount FROM CentreSurcharges WHERE CentreID=? AND SurchargeType='SEAT' AND TargetValue=? AND EffectiveFrom<=? AND (EffectiveTo IS NULL OR EffectiveTo>?) ORDER BY EffectiveFrom DESC,SurchargeID DESC) sc")){
            var at=Timestamp.valueOf(showtime);ps.setInt(1,centreId);ps.setInt(2,movieId);ps.setString(3,hallType);ps.setTimestamp(4,at);ps.setInt(5,centreId);ps.setString(6,seatType);ps.setTimestamp(7,at);ps.setTimestamp(8,at);
            try(var rs=ps.executeQuery()){if(!rs.next())throw new IllegalStateException("Chưa có giá cơ bản cho giờ chiếu.");return rs.getBigDecimal(1).doubleValue();}
        }catch(java.sql.SQLException e){throw new IllegalStateException("Không thể tính giá vé.",e);}
    }
}
