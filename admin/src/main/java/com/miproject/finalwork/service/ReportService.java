package com.miproject.finalwork.service;


import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;
import com.miproject.finalwork.dto.resp.ReportRespDTO;

public interface ReportService {
    void uploadStatus(ReportReqDTO reqDTO);
    ReportRespDTO getStatus(ReportReqDTO reqDTO);

}
