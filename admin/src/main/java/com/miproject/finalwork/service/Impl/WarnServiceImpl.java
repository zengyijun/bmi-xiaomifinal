package com.miproject.finalwork.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.*;
import com.miproject.finalwork.dto.req.WarnReqDTO;
import com.miproject.finalwork.dto.resp.WarnRespDTO;
import com.miproject.finalwork.service.WarnService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WarnServiceImpl implements WarnService {


    @Autowired
    private BatteryStatusMapper batteryStatusMapper;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private VehicleMapper vehicleMapper;
    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

    @Autowired
    private CarFrameMapper carFrameMapper;


    @Override
    public List<WarnRespDTO> getWarn(List<WarnReqDTO> reqDTO) {
        List<WarnRespDTO> warnRespDTOs = new ArrayList<>();
        for(WarnReqDTO warnReqDTO:reqDTO){
            CarFrameDO carFrameDO = carFrameMapper.selectById(warnReqDTO.getCarId());
            Map<String, Float> data;
            try {
                data = objectMapper.readValue(warnReqDTO.getSignal(), new TypeReference<Map<String, Float>>() {});
            } catch (JsonProcessingException e) {
                throw new ServiceException(e.getMessage());
            }
            List<RulesDO> rules = new ArrayList<>();
            float max = 0;
            float min = 0;
            if(data.containsKey("Ix")){
                max = data.get("Ix");
                min = data.get("Ii");
                LambdaQueryWrapper<CurrentRuleDO> queryWrapper = Wrappers.lambdaQuery(CurrentRuleDO.class)
                        .eq(CurrentRuleDO::getBatteryType, carFrameDO.getBatteryType());
                rules.addAll(currentRuleMapper.selectList(queryWrapper));
            }

            warnRespDTOs.add(getWarnDetail(rules, carFrameDO, max-min));
            rules.clear();
            if(data.containsKey("Mx")){
                max = data.get("Mx");
                min = data.get("Mi");
                LambdaQueryWrapper<VoltageRuleDO> queryWrapper = Wrappers.lambdaQuery(VoltageRuleDO.class)
                        .eq(VoltageRuleDO::getBatteryType, carFrameDO.getBatteryType());
                rules.addAll(voltageRuleMapper.selectList(queryWrapper));
            }
            warnRespDTOs.add(getWarnDetail(rules, carFrameDO, max-min));

        }
        return warnRespDTOs;

    }



    public WarnRespDTO getWarnDetail(List<RulesDO> rules, CarFrameDO carFrameDO , Float val) {
        for (RulesDO rule : rules) {
            String esp = rule.getRule();
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            engine.put("val", val);
            Boolean res;
            try {
                res = (Boolean) engine.eval(esp);
            } catch (ScriptException e) {
                throw new ServiceException(e.getMessage());
            }
            if(res == true){
                WarnRespDTO dto = new WarnRespDTO();
                BeanUtils.copyProperties(carFrameDO, dto);
                dto.setWarnName(rule.getWarnName());
                dto.setWarnLevel(rule.getWarnLevel());
                return dto;
            }
        }
        return null;
    }
}
