package com.miproject.finalwork.service.Impl;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.common.context.RuleContext;
import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.*;
import com.miproject.finalwork.dto.req.SignalDTO;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.service.WarnService;
import com.miproject.finalwork.service.RuleEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;




/**
 * @author zengyijun
 */
@Slf4j
@Service
public class WarnServiceImpl implements WarnService {


    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

    @Autowired
    private CarFrameMapper carFrameMapper;
    
    @Autowired
    private RuleEvaluationService ruleEvaluationService;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<WarnRespDTO> getWarn(List<WarnReqDTO> reqDTO) {
        List<WarnRespDTO> warnRespDTOs = new ArrayList<>();
        RuleContext ruleContext = new RuleContext();
        
        // 预先获取所有需要的规则
        Map<String, List<BaseRuleDO>> rulesMap = new HashMap<>();
        
        for(WarnReqDTO warnReqDTO : reqDTO){
            CarFrameDO carFrameDO = carFrameMapper.selectById(warnReqDTO.getCarId());
            if(carFrameDO == null){
                throw new ClientException(BaseErrorCode.DATA_ERROR);
            }
            
            SignalDTO data = JSON.parseObject(warnReqDTO.getSignal(), SignalDTO.class);
            ruleContext.setMx(data.getMx());
            ruleContext.setMn(data.getMi());
            ruleContext.setIx(data.getIx());
            ruleContext.setIn(data.getIi());
            
            String batteryType = carFrameDO.getBatteryType();
            
            // 获取电压规则
            if(data.getMx() != null && !rulesMap.containsKey("voltage_" + batteryType)) {
                List<BaseRuleDO> voltageRules = new ArrayList<>(getVoltageRulesFromCache(batteryType));
                rulesMap.put("voltage_" + batteryType, voltageRules);
            }
            
            // 获取电流规则
            if(data.getIx() != null && !rulesMap.containsKey("current_" + batteryType)) {
                List<BaseRuleDO> currentRules = new ArrayList<>(getCurrentRulesFromCache(batteryType));
                rulesMap.put("current_" + batteryType, currentRules);
            }
            
            // 处理电压警告
            if(data.getMx() != null) {
                List<BaseRuleDO> voltageRules = rulesMap.get("voltage_" + batteryType);
                warnRespDTOs.add(getWarnDetail(voltageRules, carFrameDO, ruleContext));
            }
            
            // 处理电流警告
            if(data.getIx() != null) {
                List<BaseRuleDO> currentRules = rulesMap.get("current_" + batteryType);
                warnRespDTOs.add(getWarnDetail(currentRules, carFrameDO, ruleContext));
            }
        }
        
        return warnRespDTOs;
    }

    /**
     * 从缓存获取电流规则，如果没有则从数据库获取并存入缓存
     * @param batteryType 电池类型
     * @return 电流规则列表
     */
    private List<CurrentRuleDO> getCurrentRulesFromCache(String batteryType) {
        String cacheKey = "rules:current:" + batteryType;
        String cachedRulesJson = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cachedRulesJson != null) {
            // 缓存命中，直接返回
            return JSON.parseArray(cachedRulesJson, CurrentRuleDO.class);
        }
        
        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<CurrentRuleDO> queryWrapper = Wrappers.lambdaQuery(CurrentRuleDO.class)
                .eq(CurrentRuleDO::getBatteryType, batteryType);
        List<CurrentRuleDO> rules = currentRuleMapper.selectList(queryWrapper);
        
        // 存入缓存，过期时间1小时
        if (!rules.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(rules), 1, TimeUnit.HOURS);
        }
        
        return rules;
    }
    
    /**
     * 从缓存获取电压规则，如果没有则从数据库获取并存入缓存
     * @param batteryType 电池类型
     * @return 电压规则列表
     */
    private List<VoltageRuleDO> getVoltageRulesFromCache(String batteryType) {
        String cacheKey = "rules:voltage:" + batteryType;
        String cachedRulesJson = stringRedisTemplate.opsForValue().get(cacheKey);
        
        if (cachedRulesJson != null) {
            // 缓存命中，直接返回
            return JSON.parseArray(cachedRulesJson, VoltageRuleDO.class);
        }
        
        // 缓存未命中，从数据库查询
        LambdaQueryWrapper<VoltageRuleDO> queryWrapper = Wrappers.lambdaQuery(VoltageRuleDO.class)
                .eq(VoltageRuleDO::getBatteryType, batteryType);
        List<VoltageRuleDO> rules = voltageRuleMapper.selectList(queryWrapper);
        
        // 存入缓存，过期时间1小时
        if (!rules.isEmpty()) {
            stringRedisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(rules), 1, TimeUnit.HOURS);
        }
        
        return rules;
    }

    public WarnRespDTO getWarnDetail(List<BaseRuleDO> rules, CarFrameDO carFrameDO , RuleContext data) {
        for (BaseRuleDO rule : rules) {
            try {
                boolean result = ruleEvaluationService.evaluateRule(rule.getRule(), data);

                if(result) {
                    WarnRespDTO dto = new WarnRespDTO();
                    BeanUtils.copyProperties(carFrameDO, dto);
                    dto.setWarnName(rule.getWarnName());
                    dto.setWarnLevel(rule.getWarnLevel());
                    return dto;
                }
            } catch (ServiceException e) {
                throw e; // 重新抛出服务异常
            } catch (Exception e) {
                throw new ServiceException("规则评估过程中发生错误: " + e.getMessage());
            }
        }
        // 没有匹配规则时返回默认对象而不是null
        return new WarnRespDTO();
    }
}