package com.miproject.finalwork.service;

import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;

import java.util.List;

public interface WarnInfoService {
    List<WarnInfoRespDTO> getWarnInfo(String vid);
}
