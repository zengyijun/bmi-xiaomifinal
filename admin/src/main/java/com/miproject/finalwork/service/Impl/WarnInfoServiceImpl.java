package com.miproject.finalwork.service.Impl;

import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.mapper.WarnInfoMapper;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.service.WarnInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarnInfoServiceImpl implements WarnInfoService {

    @Autowired
    private WarnInfoMapper warnInfoMapper;

    @Override
    public List<WarnInfoRespDTO> getWarnInfo(String vid) {
         List<WarnInfoRespDTO> resp = warnInfoMapper.getWarnInfo(vid);
         if(resp == null){
             throw new ServiceException(BaseErrorCode.DATA_ERROR);
         }
         return resp;
    }
}
