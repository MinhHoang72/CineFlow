import dao.ShowtimeDAO;
import dao.SeatDAO;
import dao.SurchargeDAO;
import service.BookingService;
import utils.DBContext;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/** Opt-in local integration test. Creates its own showtime; removes only that fixture in finally. */
public class PricingLifecycleIntegrationTest {
    static int scalar(String sql,int id) throws Exception {
        try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement(sql)){
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){rs.next();return rs.getInt(1);}
        }
    }
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void blocked(Checked action,String message)throws Exception{
        try{action.run();}catch(IllegalArgumentException e){return;}throw new AssertionError(message);
    }
    interface Checked{void run()throws Exception;}
    static void adminAuthorizationChecks()throws Exception{
        var session=(jakarta.servlet.http.HttpSession)java.lang.reflect.Proxy.newProxyInstance(PricingLifecycleIntegrationTest.class.getClassLoader(),new Class[]{jakarta.servlet.http.HttpSession.class},(p,m,a)->m.getName().equals("getAttribute")?("role".equals(a[0])?"ADMIN":"userId".equals(a[0])?1:null):null);
        for(String path:List.of("/pricing","/pricing/update","/pricing/hall-surcharges","/pricing/surcharges/save","/showtimes/sales")){
            boolean[] passed={false};int[] status={0};
            var req=(jakarta.servlet.http.HttpServletRequest)java.lang.reflect.Proxy.newProxyInstance(PricingLifecycleIntegrationTest.class.getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletRequest.class},(p,m,a)->m.getName().equals("getSession")?session:m.getName().equals("getServletPath")?path:m.getName().equals("getContextPath")?"":null);
            var resp=(jakarta.servlet.http.HttpServletResponse)java.lang.reflect.Proxy.newProxyInstance(PricingLifecycleIntegrationTest.class.getClassLoader(),new Class[]{jakarta.servlet.http.HttpServletResponse.class},(p,m,a)->{if(m.getName().equals("sendError"))status[0]=(Integer)a[0];return null;});
            new filter.ManagerFilter().doFilter(req,resp,(r,s)->passed[0]=true);
            boolean base=path.equals("/pricing")||path.equals("/pricing/update");
            require(base?passed[0]:status[0]==403,"Incorrect Admin permission for "+path);
        }
        System.out.println("PASS: Admin base access allowed; Admin surcharge/sale mutations forbidden");
    }
    static void policyChecks(int user)throws Exception{
        int centre;
        try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("INSERT Centres(CentreName,Address,Status) OUTPUT INSERTED.CentreID VALUES(N'PRICING TEST FIXTURE',N'Test fixture only','INACTIVE')")){
            try(ResultSet rs=ps.executeQuery()){rs.next();centre=rs.getInt(1);}
        }
        SurchargeDAO dao=new SurchargeDAO();
        try{
            String target=String.valueOf(centre);
            dao.savePolicy(centre,user,"CENTRE",target,"Test",new java.math.BigDecimal("1000"),null,null,null,null);
            dao.savePolicy(centre,user,"CENTRE",target,"Scheduled",new java.math.BigDecimal("2000"),Timestamp.valueOf(LocalDateTime.now().plusDays(2)),null,null,null);
            require(dao.policies(centre,"CENTRE").size()==2,"Scheduled version lost history");
            int id=((Number)dao.policies(centre,"CENTRE").get(0).get("SurchargeID")).intValue();
            blocked(()->dao.endPolicy(-1,id),"Cross-centre end-policy allowed");
            dao.savePolicy(centre,user,"TIME","09:00:00-12:00:00","Morning",new java.math.BigDecimal("3000"),null,Time.valueOf("09:00:00"),Time.valueOf("12:00:00"),null);
            blocked(()->dao.savePolicy(centre,user,"TIME","11:00:00-13:00:00","Overlap",new java.math.BigDecimal("4000"),null,Time.valueOf("11:00:00"),Time.valueOf("13:00:00"),null),"Overlapping time policies accepted");
            dao.savePolicy(centre,user,"TIME","12:00:00-13:00:00","Adjacent",new java.math.BigDecimal("4000"),null,Time.valueOf("12:00:00"),Time.valueOf("13:00:00"),null);
            int timeId=((Number)dao.policies(centre,"TIME").stream().filter(r->r.get("TargetValue").equals("09:00:00-12:00:00")).findFirst().orElseThrow().get("SurchargeID")).intValue();
            dao.savePolicy(centre,user,"TIME","09:00:00-12:00:00","Morning revision",new java.math.BigDecimal("5000"),null,Time.valueOf("09:00:00"),Time.valueOf("12:00:00"),timeId);
            dao.endPolicy(centre,id);
            System.out.println("PASS: scheduled policy history, scoped writes, time overlap/adjacency, policy revision/end");
        }finally{
            try(Connection c=DBContext.getConnection()){
                for(String sql:new String[]{"DELETE CentreSurcharges WHERE CentreID=?","DELETE Centres WHERE CentreID=?"})try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,centre);ps.executeUpdate();}
            }
        }
    }
    public static void main(String[] args)throws Exception{
        if(args.length!=1||!"--local-db".equals(args[0]))throw new IllegalArgumentException("Use --local-db explicitly.");
        adminAuthorizationChecks();
        ShowtimeDAO dao=new ShowtimeDAO();int centre,movie,hall,user,id=0,seat;
        LocalDateTime start;
        try(Connection c=DBContext.getConnection();Statement st=c.createStatement();ResultSet rs=st.executeQuery(
            "SELECT TOP 1 h.CentreID,h.HallID,m.MovieID,u.UserID,DATEADD(DAY,1,(SELECT MAX(StartTime) FROM Showtimes WHERE HallID=h.HallID)),(SELECT TOP 1 SeatID FROM Seats WHERE HallID=h.HallID AND Status='AVAILABLE') FROM Halls h JOIN CentreMovies cm ON cm.CentreID=h.CentreID JOIN Movies m ON m.MovieID=cm.MovieID JOIN Users u ON u.CentreID=h.CentreID AND u.Role='MANAGER' WHERE h.Status='AVAILABLE' AND m.Status='NOW_SHOWING' AND cm.IsActive=1 AND EXISTS(SELECT 1 FROM Seats WHERE HallID=h.HallID AND Status='AVAILABLE') ORDER BY h.CentreID,h.HallID")){
            require(rs.next(),"No test fixture source");centre=rs.getInt(1);hall=rs.getInt(2);movie=rs.getInt(3);user=rs.getInt(4);start=rs.getTimestamp(5).toLocalDateTime();seat=rs.getInt(6);
        }
        if(start.isBefore(LocalDateTime.now().plusDays(1)))start=LocalDateTime.now().plusDays(1);
        final LocalDateTime at=start;
        policyChecks(user);
        try{
            dao.saveShowtime(centre,user,null,movie,hall,at);
            try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("SELECT MAX(ShowtimeID) FROM Showtimes WHERE HallID=? AND StartTime=? AND CreatedBy=?")){
                ps.setInt(1,hall);ps.setTimestamp(2,Timestamp.valueOf(at));ps.setInt(3,user);try(ResultSet rs=ps.executeQuery()){rs.next();id=rs.getInt(1);}
            }
            final int test=id;
            require(id>0,"Showtime not created");
            require(dao.getById(id).isEditable()&&"SCHEDULED".equals(dao.getById(id).getDisplayStatus()),"Editable scheduled showtime expected");
            require(dao.getBookableById(id)!=null,"Created showtime is not immediately bookable");
            blocked(()->dao.saveShowtime(-1,user,test,movie,hall,at),"Cross-centre edit allowed");
            dao.saveShowtime(centre,user,id,movie,hall,at.plusMinutes(1));
            require(dao.getBookableById(id)!=null,"Edited showtime not bookable");
            blocked(()->dao.saveShowtime(centre,user,test,movie,hall,LocalDateTime.now().minusDays(1)),"Past start accepted");
            require(dao.getBookableById(id)!=null,"Failed edit did not roll back");
            int booking=new BookingService().holdSeatsForCounter(id,List.of(seat));
            require(booking>0,"Booking failed");
            require(scalar("SELECT COUNT(*) FROM BookingSeats bs JOIN ShowtimeSeatPrices p ON p.ShowtimeID=bs.ShowtimeID JOIN Seats se ON se.SeatID=bs.SeatID AND se.SeatType=p.SeatType WHERE bs.BookingID=? AND bs.Price=p.Total",booking)==1,"Checkout price differs from snapshot");

            blocked(()->dao.cancelShowtime(centre,test),"Booked showtime cancelled");
            blocked(()->dao.saveShowtime(centre,user,test,movie,hall,at),"Booked showtime edited");
            try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE Bookings SET BookingStatus='CONFIRMED' WHERE BookingID=?")){ps.setInt(1,booking);ps.executeUpdate();}
            blocked(()->dao.cancelShowtime(centre,test),"Paid booking allowed cancellation");
            blocked(()->dao.saveShowtime(centre,user,test,movie,hall,at),"Paid booking allowed edit");
            try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE Bookings SET BookingStatus='PENDING' WHERE BookingID=?")){ps.setInt(1,booking);ps.executeUpdate();}
            try(Connection c=DBContext.getConnection();PreparedStatement ps=c.prepareStatement("UPDATE BookingSeats SET HoldUntil=DATEADD(MINUTE,-1,SYSDATETIME()) WHERE BookingID=?")){ps.setInt(1,booking);ps.executeUpdate();}
            require(dao.getById(id).isEditable(),"Expired hold still locks showtime");
            dao.saveShowtime(centre,user,id,movie,hall,at.plusMinutes(2));
            try(Connection c=DBContext.getConnection()){
                require(!new dao.PaymentDAO().bookingStillMatchesShowtime(c,booking),"Old expired booking can confirm after edit");
            }
            dao.cancelShowtime(centre,id);
            require("CANCELLED".equals(dao.getById(id).getStatus()),"Cancel failed");
            require(dao.getBookableById(id)==null,"Cancelled showtime bookable");
            blocked(()->dao.saveShowtime(centre,user,test,movie,hall,at),"Cancelled showtime edited");
            System.out.println("PASS: immediate sale, atomic edit/reprice, scope, checkout snapshot, held booking locks, expired holds permit edit/cancel");
        }finally{
            if(id>0)try(Connection c=DBContext.getConnection()){
                c.setAutoCommit(false);
                for(String sql:new String[]{"DELETE BookingSeats WHERE ShowtimeID=?","DELETE Bookings WHERE ShowtimeID=?","DELETE ShowtimeSeatPrices WHERE ShowtimeID=?","DELETE Showtimes WHERE ShowtimeID=?"})try(PreparedStatement ps=c.prepareStatement(sql)){ps.setInt(1,id);ps.executeUpdate();}
                c.commit();
            }
        }
    }
}
