package com.miproject.finalwork.handler.Impl;

import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.result.Result;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.WarnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author zengyijun
 */
@Component
@AlarmType("warn")
public class WarnHandler implements Handler<List<WarnReqDTO>, List<WarnRespDTO>> {

    @Autowired
    private WarnService warnService;

    @Override
    public List<WarnRespDTO> handle(String funcall, List<WarnReqDTO> data, HandlerChainContext ctx){
        List<WarnRespDTO> warnRespDTOS = warnService.getWarn(data);
        if(warnRespDTOS == null)
            throw new ClientException(BaseErrorCode.DATA_ERROR);
        ctx.put("finalResult", warnRespDTOS);
        return warnRespDTOS;

    }

}
