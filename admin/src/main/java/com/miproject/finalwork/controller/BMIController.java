package com.miproject.finalwork.controller;

import com.miproject.finalwork.common.convention.result.Result;
import com.miproject.finalwork.common.convention.result.Results;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.handler.HandlerChainFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zengyijun
 */
@RestController
public class BMIController {

    @Autowired
    private HandlerChainFactory handlerChainFactory;

    @PostMapping("/api/warn")
    Result<List<WarnRespDTO>> reportWarn(@RequestBody List<WarnReqDTO> warnReqDTO){
        return Results.success((List<WarnRespDTO>) handlerChainFactory.execute(new String[]{"validate", "warn"}, "warn" ,warnReqDTO));

    }
// 管理人员用于新增电流/电压规则
    @PostMapping("/api/admin/addRule")
    Result<Void> addRule(@RequestBody RuleAddReqDTO warnReqDTO){
        handlerChainFactory.execute(new String[]{"validate", "admin"}, "admin",warnReqDTO);
        return Results.success();

    }

    @PostMapping("/api/reportData")
    Result<Void> reportData(@RequestBody ReportReqDTO reqDTO){
        handlerChainFactory.execute(new String[]{"validate", "report"}, "upload" ,reqDTO);
        return Results.success();
    }

    @GetMapping("/api/getReportData")
    Result<DetailReportRespDTO> getReportData(@RequestBody ReportReqDTO reqDTO){
        return Results.success((DetailReportRespDTO) handlerChainFactory.execute(new String[]{"report"}, "get", reqDTO));
    }
    @GetMapping("/api/getDetailReportData")
    Result<DetailReportRespDTO> getDetailReportData(@RequestBody ReportReqDTO reqDTO){
        return Results.success((DetailReportRespDTO) handlerChainFactory.execute(new String[]{"detail_report"}, "detail_report", reqDTO));
    }
    @GetMapping("/api/getWarnInfo/{vid}")
    Result<List<WarnInfoRespDTO>> getWarnInfo(@PathVariable("vid") String vid){
        return Results.success((List<WarnInfoRespDTO>) handlerChainFactory.execute(new String[]{"warn_info"}, "warn_info", vid));
    }

}
