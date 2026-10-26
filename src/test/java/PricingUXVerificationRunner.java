import dao.CentrePricingDAO;
import dao.SurchargeDAO;
import model.entity.Surcharge;
import service.PricingService;
import utils.DBContext;
import java.sql.*;
import java.time.LocalDateTime;

/** Integration checks use a single transaction, always rolled back. */
public class PricingUXVerificationRunner {
    public static void main(String[] args) throws Exception {
        if (PricingService.parseVnd("80.000") != 80000 || PricingService.parseVnd("10000") != 10000)
            throw new AssertionError("Incorrect VND parsing");
        for (String invalid : new String[]{"-100", "NaN", "Infinity", "80.00", "10000000000", "1e5", ""}) {
            try { PricingService.parseVnd(invalid); throw new AssertionError("Accepted invalid money: " + invalid); }
            catch (IllegalArgumentException expected) { }
        }
        try (Connection c = DBContext.getConnection()) {
            c.setAutoCommit(false);
            try {
                int centre, user;
                LocalDateTime now;
                try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT TOP 1 CentreID,SYSDATETIME() Now FROM Centres ORDER BY CentreID")) {
                    r.next(); centre=r.getInt(1); now=r.getTimestamp(2).toLocalDateTime();
                }
                try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT TOP 1 UserID FROM Users ORDER BY UserID")) { r.next();user=r.getInt(1); }
                try (PreparedStatement s = c.prepareStatement("DELETE FROM CentrePricing WHERE CentreID=?")) {s.setInt(1,centre);s.executeUpdate();}
                CentrePricingDAO dao = new CentrePricingDAO();
                dao.scheduleBasePrice(c,centre,70000,user,null);
                LocalDateTime first=now.plusDays(1).withNano(0), second=now.plusDays(2).withNano(0);
                dao.scheduleBasePrice(c,centre,90000,user,second);
                dao.scheduleBasePrice(c,centre,80000,user,first);
                checkPrice(c,centre,first.minusSeconds(1),70000);
                checkPrice(c,centre,first,80000);
                checkPrice(c,centre,second.minusSeconds(1),80000);
                checkPrice(c,centre,second,90000);
                dao.scheduleBasePrice(c,centre,75000,user,null);
                checkPrice(c,centre,now.plusHours(1),75000);
                checkPrice(c,centre,first,80000);
                try { dao.scheduleBasePrice(c,centre,88000,user,first); throw new AssertionError("Duplicate effective time accepted"); }
                catch (IllegalArgumentException expected) { }
                try { dao.scheduleBasePrice(c,centre,88000,user,now.minusDays(1)); throw new AssertionError("Past effective time accepted"); }
                catch (IllegalArgumentException expected) { }
                try (PreparedStatement s=c.prepareStatement("SELECT COUNT(*) FROM CentrePricing a JOIN CentrePricing b ON a.CentreID=b.CentreID AND a.CentrePricingID<b.CentrePricingID AND (b.EffectiveTo IS NULL OR a.EffectiveFrom<b.EffectiveTo) AND (a.EffectiveTo IS NULL OR b.EffectiveFrom<a.EffectiveTo) WHERE a.CentreID=?")) {
                    s.setInt(1,centre);try(ResultSet r=s.executeQuery()){r.next();if(r.getInt(1)!=0)throw new AssertionError("Price periods overlap");}
                }
                Surcharge rule=new Surcharge();rule.setSurchargeName("Verification only");rule.setSurchargeType("HALL");rule.setTargetValue("VERIFY_"+System.nanoTime());rule.setAmount(12000);rule.setUpdatedBy(user);
                SurchargeDAO surcharge=new SurchargeDAO();
                if(!surcharge.createSurcharge(c,rule) || surcharge.createSurcharge(c,rule)) throw new AssertionError("Surcharge duplicate protection failed");
                System.out.println("PASS: VND validation, future scheduling, insertion between future prices, exact effective boundaries, immediate update preserving schedules, duplicate/past rejection, no overlaps, surcharge creation/duplicate prevention");
            } finally {c.rollback();}
        } finally {DBContext.closeRequestConnections();}
        System.out.println("All fixture changes rolled back");
    }
    private static void checkPrice(Connection c,int centre,LocalDateTime at,double expected)throws Exception {
        try(PreparedStatement s=c.prepareStatement("SELECT TOP 1 BasePrice FROM CentrePricing WHERE CentreID=? AND EffectiveFrom<=? AND (EffectiveTo IS NULL OR EffectiveTo>?) ORDER BY EffectiveFrom DESC")) {
            s.setInt(1,centre);s.setTimestamp(2,Timestamp.valueOf(at));s.setTimestamp(3,Timestamp.valueOf(at));
            try(ResultSet r=s.executeQuery()){if(!r.next()||r.getDouble(1)!=expected)throw new AssertionError("Wrong price at "+at);}
        }
    }
}
