package com.miproject.finalwork.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static com.miproject.finalwork.common.constant.RedisCacheConstant.BATTERY_RULES;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author zengyijun
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;



    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

//    添加规则
    @Override
    @Transactional
    public void addRule(RuleAddReqDTO rule) {

//        解析规则
        if(rule.getRule() == null || rule.getRule().isEmpty() || rule.getBatteryType().isEmpty())
        {
            throw new ClientException(BaseErrorCode.DATA_ERROR);
        }
        int i = -1;
        String ruleName = "";
        if(rule.getType() == RuleTypes.VOLTAGE_RULE.getCode()){
            ruleName = RuleTypes.getByCode(rule.getType());
            VoltageRuleDO voltageRuleDO = new VoltageRuleDO();
            BeanUtils.copyProperties(rule, voltageRuleDO);
            i = voltageRuleMapper.insert(voltageRuleDO);
            String ruleExp = "";
            try {
                ruleExp = objectMapper.writeValueAsString(voltageRuleDO);
            } catch (JsonProcessingException e){
                throw new ServiceException(e.getMessage());
            }
            stringRedisTemplate.opsForSet().add(BATTERY_RULES+ruleName, ruleExp);
            
            // 清除电压规则缓存，确保下次获取规则时使用最新的规则
            clearVoltageRuleCache(rule.getBatteryType());
        }
        else if(rule.getType() == RuleTypes.CURRENT_RULE.getCode()){
            ruleName = RuleTypes.getByCode(rule.getType());
            CurrentRuleDO currentRuleDO = new CurrentRuleDO();
            BeanUtils.copyProperties(rule, currentRuleDO);
            i = currentRuleMapper.insert(currentRuleDO);
            String ruleExp = "";
            try {
                ruleExp = objectMapper.writeValueAsString(currentRuleDO);
            } catch (JsonProcessingException e){
                throw new ServiceException(e.getMessage());
            }
            stringRedisTemplate.opsForSet().add(BATTERY_RULES+ruleName, ruleExp);
            
            // 清除电流规则缓存，确保下次获取规则时使用最新的规则
            clearCurrentRuleCache(rule.getBatteryType());

        }
        if(i < 0){
            throw new ServiceException(BaseErrorCode.SERVICE_ERROR);
        }
    }
    
    /**
     * 清除指定电池类型的电压规则缓存
     * @param batteryType 电池类型
     */
    private void clearVoltageRuleCache(String batteryType) {
        String cacheKey = "rules:voltage:" + batteryType;
        stringRedisTemplate.delete(cacheKey);
    }
    
    /**
     * 清除指定电池类型的电流规则缓存
     * @param batteryType 电池类型
     */
    private void clearCurrentRuleCache(String batteryType) {
        String cacheKey = "rules:current:" + batteryType;
        stringRedisTemplate.delete(cacheKey);
    }

}