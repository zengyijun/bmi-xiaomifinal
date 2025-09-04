package com.miproject.finalwork.service;

import com.miproject.finalwork.dto.req.WarnInfoQueryReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoPageRespDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;

import java.util.List;

public interface WarnInfoService {
    List<WarnInfoRespDTO> getWarnInfo(String vid);
    
    /**
     * 分页查询告警信息
     * @param queryReqDTO 查询参数
     * @return 分页结果
     */
    WarnInfoPageRespDTO getWarnInfoByPage(WarnInfoQueryReqDTO queryReqDTO);
}
