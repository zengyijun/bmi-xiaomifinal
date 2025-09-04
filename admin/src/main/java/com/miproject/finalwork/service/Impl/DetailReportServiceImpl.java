package com.miproject.finalwork.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.common.context.RuleContext;
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
import com.miproject.finalwork.service.RuleEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;

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
    
    @Autowired
    private RuleEvaluationService ruleEvaluationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


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
        List<BaseRuleDO> rules = new ArrayList<>();

        if(reqDTO.getType() == 1){
            // 从缓存获取电压规则，如果没有则从数据库获取并存入缓存
            List<VoltageRuleDO> voltageRules = getVoltageRulesFromCache(batteryType);
            rules.addAll(voltageRules);
        } else if (reqDTO.getType() == 2) {
            // 从缓存获取电流规则，如果没有则从数据库获取并存入缓存
            List<CurrentRuleDO> currentRules = getCurrentRulesFromCache(batteryType);
            rules.addAll(currentRules);
        }
        if(!hasData(reqDTO)){
            ReportRespDTO respDTO = reportService.getStatus(reqDTO);
            BeanUtils.copyProperties(respDTO, reqDTO);
        }

        DetailReportRespDTO detailReportRespDTO = new DetailReportRespDTO();
        WarnDetailDO warnDetailDO = getWarnDetail(rules, reqDTO);
        BeanUtils.copyProperties(warnDetailDO, detailReportRespDTO);
        BeanUtils.copyProperties(reqDTO, detailReportRespDTO);
        return detailReportRespDTO;
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

    public WarnDetailDO getWarnDetail(List<BaseRuleDO> rules, ReportReqDTO reqDTO) {
        for (BaseRuleDO rule : rules) {
            try {
                // 创建规则上下文
                RuleContext context = new RuleContext();
                context.setMx(reqDTO.getRawMaxVal());
                context.setMn(reqDTO.getRawMinVal());
                context.setIx(reqDTO.getRawMaxVal());
                context.setIn(reqDTO.getRawMinVal());
                // 使用规则解析服务评估规则
                boolean res = ruleEvaluationService.evaluateRule(rule.getRule(), context);
                
                if(res){
                    WarnDetailDO warnDetailDO = new WarnDetailDO();
                    BeanUtils.copyProperties(rule, warnDetailDO);
                    return warnDetailDO;
                }
            } catch (ServiceException e) {
                throw e; // 重新抛出服务异常
            } catch (Exception e) {
                throw new ServiceException("规则评估过程中发生错误: " + e.getMessage());
            }
        }
        // 没有匹配规则时返回默认对象而不是null
        return new WarnDetailDO();
    }
}