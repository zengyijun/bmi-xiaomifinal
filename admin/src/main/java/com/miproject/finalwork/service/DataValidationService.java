package com.miproject.finalwork.service;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;

import java.util.List;

public interface DataValidationService {
    boolean checkWarnData(List<WarnReqDTO> data);
    boolean checkRuleData(RuleAddReqDTO ruleAddReqDTO);
    boolean checkReportUploadData(ReportReqDTO reqDTO);
}
