package com.miproject.finalwork.service.Impl;

import com.miproject.finalwork.common.constant.RuleTypes;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.entity.CurrentRuleDO;
import com.miproject.finalwork.dao.entity.VoltageRuleDO;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.RuleAddReqDTO;
import com.miproject.finalwork.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

//    添加规则
    @Override
    public void addRule(RuleAddReqDTO rule) {

//        解析规则
        if(rule.getRule() == null || rule.getRule().equals("") || rule.getBatteryType().equals("") || rule.getBatteryType() == null)
        {
            throw new ClientException(BaseErrorCode.DATA_ERROR);
        }
        int i = -1;
        if(rule.getType() == RuleTypes.VOLTAGE_RULE.getCode()){
            VoltageRuleDO voltageRuleDO = new VoltageRuleDO();
            BeanUtils.copyProperties(rule, voltageRuleDO);
            i = voltageRuleMapper.insert(voltageRuleDO);
        }
        else if(rule.getType() == RuleTypes.CURRENT_RULE.getCode()){
            CurrentRuleDO currentRuleDO = new CurrentRuleDO();
            BeanUtils.copyProperties(rule, currentRuleDO);
            i = currentRuleMapper.insert(currentRuleDO);
        }
        if(i < 0){
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR);
        }
    }

}
