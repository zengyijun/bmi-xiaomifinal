package com.miproject.finalwork.handler.Impl;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.DetailReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 处理电压详细信息
 */
@AlarmType("detail_report")
@Component
public class DetailReportHandler implements Handler<ReportReqDTO, DetailReportRespDTO> {


    @Autowired
    private DetailReportService reportService;

    @Override
    public DetailReportRespDTO handle(String funcall, ReportReqDTO data, HandlerChainContext ctx) {
        if (!"detail_report".equals(funcall)) {
            ctx.next(funcall, data);
            return null;
        }

        DetailReportRespDTO respDTO = reportService.getDetailStatus(data);
        ctx.put("finalResult", respDTO);
        return respDTO;
    }


}
