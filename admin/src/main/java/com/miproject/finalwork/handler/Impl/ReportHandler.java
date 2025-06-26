package com.miproject.finalwork.handler.Impl;


import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.ReportRespDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 电压上报器
 */
@AlarmType("report")
@Component
public class ReportHandler implements Handler<ReportReqDTO, ReportRespDTO> {

    @Autowired
    private ReportService reportService;

    @Override
    public ReportRespDTO handle(String funcall, ReportReqDTO data, HandlerChainContext ctx) {

        switch (funcall) {
            case "upload":
                reportService.uploadStatus(data);
                return null;
            case "get":
                ReportRespDTO respDTO = reportService.getStatus(data);
                ctx.put("finalResult", respDTO);
                return respDTO;
            default:
                // detail或其他交给下一个处理器
                ctx.next(funcall, data);
                return null;
        }
    }
}
