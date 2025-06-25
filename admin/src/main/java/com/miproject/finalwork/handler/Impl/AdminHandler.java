package com.miproject.finalwork.handler.Impl;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.resp.ReportRespDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.AdminService;
import com.miproject.finalwork.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AlarmType("admin")
@Component
public class AdminHandler implements Handler<RuleAddReqDTO, Boolean> {

    @Autowired
    private AdminService adminService;
    @Override
    public Boolean handle(String funcall, RuleAddReqDTO data, HandlerChainContext ctx) {
        if(!funcall.equals("admin")){
            ctx.next(funcall, data);
            return null;
        }

        adminService.addRule(data);
        return true;
    }
}
