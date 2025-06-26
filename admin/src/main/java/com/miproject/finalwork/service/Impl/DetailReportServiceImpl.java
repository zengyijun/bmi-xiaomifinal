package com.miproject.finalwork.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VehicleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;
import com.miproject.finalwork.dto.resp.ReportRespDTO;
import com.miproject.finalwork.service.DetailReportService;
import com.miproject.finalwork.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DetailReportServiceImpl implements DetailReportService {
    //    获取详细的信息


    @Autowired
    private VehicleMapper vehicleMapper;
    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private ReportService reportService;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;
    /**
     * 两种查法：先查最近保存的信息，再查详细信息
     *          直接根据reqDTO中的数据查询详细信息
     * @param reqDTO
     * @return
     */

    public boolean hasData(ReportReqDTO reqDTO){

        if(reqDTO.getReqType() == 1){
            return (reqDTO.getRawMaxVal() != null || reqDTO.getRawMinVal() != null);
        }
        return false;
    }


    @Override
    public DetailReportRespDTO getDetailStatus(ReportReqDTO reqDTO) {

        VehicleDO vehicleDO = vehicleMapper.selectById(reqDTO.getVid());
        if(vehicleDO == null)
            throw new ClientException(BaseErrorCode.DATA_ERROR);

        String batteryType = vehicleDO.getBatteryType();
        List<RulesDO> rules = new ArrayList<>();

        if(reqDTO.getType() == 1){
            LambdaQueryWrapper<VoltageRuleDO> queryWrapper = Wrappers.lambdaQuery(VoltageRuleDO.class)
                    .eq(VoltageRuleDO::getBatteryType, batteryType);
            rules.addAll(voltageRuleMapper.selectList(queryWrapper));
        } else if (reqDTO.getType() == 2) {
            LambdaQueryWrapper<CurrentRuleDO> queryWrapper = Wrappers.lambdaQuery(CurrentRuleDO.class)
                    .eq(CurrentRuleDO::getBatteryType, batteryType);
            rules.addAll(currentRuleMapper.selectList(queryWrapper));
        }
        if(!hasData(reqDTO)){
            ReportRespDTO respDTO = reportService.getStatus(reqDTO);
            BeanUtils.copyProperties(respDTO, reqDTO);
        }

        float  val = reqDTO.getRawMaxVal() - reqDTO.getRawMinVal();

        DetailReportRespDTO detailReportRespDTO = new DetailReportRespDTO();
        WarnDetailDO warnDetailDO = getWarnDetail(rules, val);
        BeanUtils.copyProperties(warnDetailDO, detailReportRespDTO);
        BeanUtils.copyProperties(reqDTO, detailReportRespDTO);
        return detailReportRespDTO;
    }

    public WarnDetailDO getWarnDetail(List<RulesDO> rules, Float val) {
        for (RulesDO rule : rules) {
            String esp = rule.getRule();
            esp = esp.replaceAll("(\\d+(?:\\.\\d+)?)\\s*<=\\s*val\\s*<\\s*(\\d+(?:\\.\\d+)?)", "($1 <= val && val < $2)");

            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            engine.put("val", val);
            Boolean res;
            try {
                res = (Boolean) engine.eval(esp);
            } catch (ScriptException e) {
                throw new ServiceException(e.getMessage());
            }
            if(res == true){
                WarnDetailDO warnDetailDO = new WarnDetailDO();
                BeanUtils.copyProperties(rule, warnDetailDO);
                return warnDetailDO;
            }
        }
        return null;
    }
}
