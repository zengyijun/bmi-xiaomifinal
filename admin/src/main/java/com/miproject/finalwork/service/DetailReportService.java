package com.miproject.finalwork.service;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;

/**
 * @author zengyijun
 */
public interface DetailReportService {
    DetailReportRespDTO getDetailStatus(ReportReqDTO reqDTO);

}
