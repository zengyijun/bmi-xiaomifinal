package com.miproject.finalwork.mq.schedule;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.common.constant.RedisCacheConstant;
import com.miproject.finalwork.common.convention.exception.RemoteException;
import com.miproject.finalwork.dao.entity.*;
import com.miproject.finalwork.dao.mapper.BatteryStatusMapper;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VehicleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.WarnMsgMQReqDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class DataReport {

    @Autowired
    private BatteryStatusMapper batteryStatusMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;


    @Scheduled(cron = "0 */1 * * * ?")
    public void reportData(){
        var lock = redissonClient.getLock(RedisCacheConstant.LOCK_STATUS_UPLOAD_KEY);
        try{
            lock.lock();
            LambdaQueryWrapper<BatteryStatusDO> queryWrapper = Wrappers.lambdaQuery(BatteryStatusDO.class)
                    .eq(BatteryStatusDO::getDelFlag, 0);
            List<BatteryStatusDO> statusDOList = batteryStatusMapper.selectList(queryWrapper);

            for(BatteryStatusDO statusDO : statusDOList){
                Float val = statusDO.getRawMaxVal() - statusDO.getRawMinVal();
                VehicleDO vehicle = vehicleMapper.selectById(statusDO.getVid());
                LambdaQueryWrapper<VoltageRuleDO> voltage = Wrappers.lambdaQuery(VoltageRuleDO.class)
                        .eq(VoltageRuleDO::getBatteryType, vehicle.getBatteryType());
                List<RulesDO> v_rule = new ArrayList<>(voltageRuleMapper.selectList(voltage));
                WarnMsgMQReqDTO warnMsgMQReqDTO = isWarnable(v_rule, statusDO);
                if(warnMsgMQReqDTO != null){
                    String topicName = "warn-topic";
                    warnMsgMQReqDTO.setTimeStamp(new Date());
                    byte[] body = JSON.toJSONString(warnMsgMQReqDTO).getBytes(StandardCharsets.UTF_8);
                    rocketMQTemplate.convertAndSend(topicName, body);
                }
                warnMsgMQReqDTO = null;
                LambdaQueryWrapper<CurrentRuleDO> current = Wrappers.lambdaQuery(CurrentRuleDO.class)
                        .eq(CurrentRuleDO::getBatteryType, vehicle.getBatteryType());
                List<RulesDO> c_rule = new ArrayList<>(currentRuleMapper.selectList(current));
                warnMsgMQReqDTO = isWarnable(c_rule, statusDO);
                if(warnMsgMQReqDTO != null){
                    String topicName = "warn-topic";
                    warnMsgMQReqDTO.setTimeStamp(new Date());
                    byte[] body = JSON.toJSONString(warnMsgMQReqDTO).getBytes(StandardCharsets.UTF_8);
                    SendResult result = rocketMQTemplate.syncSend(topicName, body);
                    log.info("定时任务上传了消息到MQ，状态："+result.getSendStatus());
                }
                statusDO.setDelFlag(1);
                batteryStatusMapper.updateById(statusDO);
            }
        }finally {
            lock.unlock();
        }
    }

    public WarnMsgMQReqDTO isWarnable(List<RulesDO>rules, BatteryStatusDO status){
        for(RulesDO rule: rules){
            String esp = rule.getRule();
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            engine.put("val", (status.getRawMaxVal()-status.getRawMinVal()));
            Boolean res;
            try {
                res = (Boolean) engine.eval(esp);
            } catch (ScriptException e) {
                throw new RemoteException(e.getMessage());
            }
            if(res == true && rule.getWarnLevel() >= 0){
                WarnMsgMQReqDTO reqDTO = new WarnMsgMQReqDTO();
                BeanUtils.copyProperties(rule, reqDTO);
                reqDTO.setVid(status.getVid());
                return reqDTO;
            }
        }
        return null;
    }

}
