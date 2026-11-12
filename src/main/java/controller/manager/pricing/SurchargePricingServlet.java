package controller.manager.pricing;

import dao.SurchargeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import service.PricingService;
import service.ShowtimeService;
import java.io.IOException;

@WebServlet(urlPatterns={"/pricing/centre-surcharges","/pricing/hall-surcharges","/pricing/seat-surcharges","/pricing/movie-surcharges","/pricing/surcharges/save","/pricing/surcharges/end"})
public class SurchargePricingServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        try {
            String type=PricingService.policyType(req.getServletPath());
            int centre=ShowtimeService.requireCentreId(req);
            SurchargeDAO dao=new SurchargeDAO();
            req.setAttribute("policyType",type);
            var policies=dao.policies(centre,type);
            req.setAttribute("policies",policies);
            String edit=req.getParameter("edit");
            if(edit!=null)req.setAttribute("editPolicy",policies.stream().filter(p->p.get("SurchargeID").toString().equals(edit)).findFirst().orElseThrow(()->new IllegalArgumentException("Không tìm thấy phụ thu.")));
            req.setAttribute("targets",dao.targets(centre,type));
            req.setAttribute("centreName",new dao.CentreDAO().getCentreById(centre).getCentreName());
            req.getRequestDispatcher("/WEB-INF/views/manager/pricing/seatSurcharge.jsp").forward(req,resp);
        }catch(IllegalArgumentException e){resp.sendError(404);}
        catch(Exception e){throw new ServletException("Không thể tải phụ thu.",e);}
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if(!"MANAGER".equals(req.getSession().getAttribute("role"))){resp.sendError(403);return;}
        try {
            if(req.getServletPath().endsWith("/end"))new SurchargeDAO().endPolicy(ShowtimeService.requireCentreId(req),Integer.parseInt(req.getParameter("id")));
            else new PricingService().savePolicy(req);
            req.getSession().setAttribute("message","Đã lưu chính sách. Giá của các suất đã mở bán được giữ nguyên.");
        }catch(Exception e){req.getSession().setAttribute("error",e instanceof IllegalArgumentException?e.getMessage():"Không thể lưu phụ thu. Vui lòng thử lại.");}
        String type=req.getParameter("type");
        String path=switch(type==null?"":type){case "CENTRE"->"centre-surcharges";case "HALL"->"hall-surcharges";case "MOVIE"->"movie-surcharges";case "TIME"->"time-slots";default->"seat-surcharges";};
        resp.sendRedirect(req.getContextPath()+"/pricing/"+path);
    }
}
