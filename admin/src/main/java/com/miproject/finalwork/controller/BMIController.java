package com.miproject.finalwork.controller;

import com.miproject.finalwork.common.annotation.RateLimiter;
import com.miproject.finalwork.common.convention.result.Result;
import com.miproject.finalwork.common.convention.result.Results;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.req.WarnInfoQueryReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoPageRespDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.handler.HandlerChainFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 电池管理系统控制器
 * 提供告警处理、规则管理、报告生成等功能
 * @author zengyijun
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "电池信息管理系统", description = "电池管理系统的相关接口")
public class BMIController {

    @Autowired
    private HandlerChainFactory handlerChainFactory;

    /**
     * 处理告警信息
     * @param warnReqDTO 告警请求数据列表
     * @return 告警响应数据列表
     */
    @PostMapping("/warnings")
    @Operation(summary = "处理告警信息")
    public Result<List<WarnRespDTO>> processWarnings(@RequestBody List<WarnReqDTO> warnReqDTO){
        return Results.success((List<WarnRespDTO>) handlerChainFactory.execute(new String[]{"validate", "warn"}, "warn" ,warnReqDTO));
    }

    /**
     * 管理人员新增电流/电压规则
     * @param ruleAddReqDTO 规则添加请求数据
     * @return 操作结果
     */
    @PostMapping("/rules")
    public Result<Void> addRule(@RequestBody RuleAddReqDTO ruleAddReqDTO){
        handlerChainFactory.execute(new String[]{"validate", "admin"}, "admin",ruleAddReqDTO);
        return Results.success();
    }

    /**
     * 上报数据
     * @param reqDTO 报告请求数据
     * @return 操作结果
     */
    @PostMapping("/reports")
    public Result<Void> reportData(@RequestBody ReportReqDTO reqDTO){
        handlerChainFactory.execute(new String[]{"validate", "report"}, "upload" ,reqDTO);
        return Results.success();
    }

    /**
     * 获取报告数据
     * @param reqDTO 报告请求数据
     * @return 报告响应数据
     */
    @GetMapping("/reports/summary")
    @RateLimiter(maxRequests = 100, timeWindow = 60)
    public Result<DetailReportRespDTO> getReportData(@RequestBody ReportReqDTO reqDTO){
        return Results.success((DetailReportRespDTO) handlerChainFactory.execute(new String[]{"report"}, "get", reqDTO));
    }
    
    /**
     * 获取详细报告数据
     * @param reqDTO 报告请求数据
     * @return 详细报告响应数据
     */
    @GetMapping("/reports/detail")
    @RateLimiter(maxRequests = 100, timeWindow = 60)
    public Result<DetailReportRespDTO> getDetailReportData(@RequestBody ReportReqDTO reqDTO){
        return Results.success((DetailReportRespDTO) handlerChainFactory.execute(new String[]{"detail_report"}, "detail_report", reqDTO));
    }
    
    /**
     * 根据车辆ID获取告警信息（全部）
     * @param vid 车辆ID
     * @return 告警信息列表
     */
    @GetMapping("/vehicles/warnings/{vid}")
    @RateLimiter(maxRequests = 100, timeWindow = 60)
    public Result<List<WarnInfoRespDTO>> getWarnInfo(@PathVariable("vid") String vid){
        return Results.success((List<WarnInfoRespDTO>) handlerChainFactory.execute(new String[]{"warn_info"}, "warn_info", vid));
    }
    
    /**
     * 根据车辆ID分页获取告警信息
     * @param queryDTO 查询参数
     * @return 分页告警信息
     */
    @PostMapping("/vehicles/warnings/page")
    @RateLimiter(maxRequests = 200, timeWindow = 60)
    public Result<WarnInfoPageRespDTO> getWarnInfoByPage(@RequestBody WarnInfoQueryReqDTO queryDTO){
        return Results.success((WarnInfoPageRespDTO) handlerChainFactory.execute(new String[]{"warn_info"}, "warn_info_page", queryDTO));
    }
}