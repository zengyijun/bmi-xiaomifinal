package com.miproject.finalwork.service;

import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;

import java.util.List;

public interface WarnService {
    List<WarnRespDTO> getWarn(List<WarnReqDTO> reqDTO);
}
