package com.miproject.finalwork.handler.Impl;

import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.dto.resp.WarnInfoRespDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.WarnInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@AlarmType("warn_info")
@Component
public class WarnInfoHandler implements Handler<String, List<WarnInfoRespDTO>> {
    @Autowired
    private WarnInfoService warnInfoService;

    @Override
    public List<WarnInfoRespDTO> handle(String funcall, String data, HandlerChainContext ctx) {
        List<WarnInfoRespDTO> warnInfoRespDTOS =  warnInfoService.getWarnInfo(data);
        if(warnInfoRespDTOS == null)
            throw new ClientException(BaseErrorCode.DATA_ERROR);
        ctx.put("finalResult", warnInfoRespDTOS);
        return warnInfoRespDTOS;
    }
}
