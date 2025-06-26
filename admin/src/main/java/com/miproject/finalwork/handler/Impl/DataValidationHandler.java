package com.miproject.finalwork.handler.Impl;

// 责任链第一条：数据校验
// 拦截非法数据

import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.handler.Handler;
import com.miproject.finalwork.handler.HandlerChainContext;
import com.miproject.finalwork.handler.annotation.AlarmType;
import com.miproject.finalwork.service.DataValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

@AlarmType("validate")
@Component
public class DataValidationHandler implements Handler<Object, Boolean> {

    @Autowired
    private DataValidationService validationService;

    @Override
    public Boolean handle(String funcall, Object data, HandlerChainContext ctx) {
//        这三种情况需要进行数据校验
        if(data == null){
            throw new ClientException(BaseErrorCode.DATA_VALIDATION_ERROR);
        }

        if(data instanceof String){
            if(((String)data).isEmpty()){
                throw new ClientException(BaseErrorCode.DATA_VALIDATION_ERROR);
            }
        }


        if(funcall.equals("get") || funcall.equals("detail_report"))
        {
             ReportReqDTO dto = (ReportReqDTO) data;
             if(dto.getVid() != null && !dto.getVid().isEmpty()){
                 ctx.next(funcall, data);
                 return true;
             }
        }
        else if(checkData(data)) {
            ctx.next(funcall, data);
            return true;
        }
        throw new ClientException(BaseErrorCode.DATA_VALIDATION_ERROR);
    }
    public boolean checkData(Object data){
        if(data instanceof ArrayList){
            return validationService.checkWarnData((ArrayList<WarnReqDTO>) data);
        }
        else if(data instanceof RuleAddReqDTO){
            return validationService.checkRuleData((RuleAddReqDTO) data);
        }
        else if(data instanceof ReportReqDTO){
            return validationService.checkReportUploadData((ReportReqDTO) data);
        }
        throw new ServiceException(BaseErrorCode.DATA_ERROR);
    }
}
