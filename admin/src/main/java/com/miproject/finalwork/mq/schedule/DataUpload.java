package com.miproject.finalwork.mq.schedule;

import cn.hutool.core.lang.hash.Hash;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.miproject.finalwork.dao.entity.BatteryStatusDO;
import com.miproject.finalwork.dao.entity.CurrentRuleDO;
import com.miproject.finalwork.dao.entity.VoltageRuleDO;
import com.miproject.finalwork.dao.mapper.BatteryStatusMapper;
import com.miproject.finalwork.dao.mapper.CurrentRuleMapper;
import com.miproject.finalwork.dao.mapper.VoltageRuleMapper;
import com.miproject.finalwork.dto.req.ReportReqDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 自动上报电池信息
// 定时根据规则生成数据上传
@Slf4j
@Component
public class DataUpload {
    private final String[] vids = new String[]{"bE7yHdL6rWiO3xK9", "tGz8wQe93kL1VaBs", "X2mF0uBQaPcN7ZrL"};
    private final String[] batteryTypes = new String[]{"三元电池", "铁锂电池", "三元电池"};
    private static final String url = "http://127.0.0.1:9000/api/v1/reports";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private VoltageRuleMapper voltageRuleMapper;

    @Autowired
    private CurrentRuleMapper currentRuleMapper;

    @Scheduled(cron = "1 1 1 * * ?")
    public void dataUpload(){
        int ts = (int) (System.currentTimeMillis() / 1000);
        int vid = ts % 3;
        int ruleId = ts % 2 + 1;
        String rule;
        if(ruleId == 1){
            rule = getVoltageRule(batteryTypes[vid]);
        }
        else{
            rule = getCurrentRule(batteryTypes[vid]);
        }
        Float min, max;
        Float[] res= genData(rule);
        min = res[0];
        max = res[1];

        ReportReqDTO reportReqDTO = new ReportReqDTO();
        reportReqDTO.setVid(vids[vid]);
        reportReqDTO.setType(ruleId);
        reportReqDTO.setRawMinVal(min);
        reportReqDTO.setRawMaxVal(max);
        reportReqDTO.setReqType(1);
        reportReqDTO.setUnit("xx");

        try{
            ResponseEntity<String> resp = restTemplate.postForEntity(url, reportReqDTO, String.class);
            log.info("数据上传成功: " + resp.getStatusCode());

        }catch(Exception e){
            log.error("[数据上传服务存在错误]："+e.getMessage(), e);
        }

        log.info("定时任务定时通过接口创建了电池信息，类型为："+ruleId+"，Minval="+min+"，Maxval="+max);

    }

    public static Float[] genData(String rule) {
        List<Float> nums = new java.util.ArrayList<>();
        // 修正正则：提取 float 数字（可含小数点）
        Pattern pattern = Pattern.compile("\\d+(?:\\.\\d+)?");
        Matcher matcher = pattern.matcher(rule);
        while (matcher.find()) {
            nums.add(Float.parseFloat(matcher.group()));
        }

        float min = 0f;
        float max = 0f;

        if (nums.size() == 0) {
            // 如果规则中没有数字，生成随机数据
            min = getRandomFloat(0, 100);
            max = min + getRandomFloat(1, 20);
        } else if (nums.size() == 1) {
            // 如果只有一个数字，可能是上限值
            float limit = nums.get(0);
            min = getRandomFloat(0, limit/2);
            max = getRandomFloat(limit/2, limit);
        } else if (nums.size() >= 2) {
            // 如果有两个或更多数字，取前两个作为范围
            float low = Math.min(nums.get(0), nums.get(1));
            float high = Math.max(nums.get(0), nums.get(1));

            // 生成在规则范围内的数据，也有一定概率生成范围外的数据用于测试告警
            if (Math.random() < 0.7) {
                // 70%概率生成范围内的数据
                min = getRandomFloat(low * 0.5f, low * 0.9f);
                max = getRandomFloat(high * 1.1f, high * 1.5f);
            } else {
                // 30%概率生成范围外的数据
                if (Math.random() < 0.5) {
                    min = getRandomFloat(0, low * 0.3f);
                    max = getRandomFloat(high * 1.8f, high * 2.5f);
                } else {
                    min = getRandomFloat(low * 0.2f, low * 0.4f);
                    max = getRandomFloat(high * 0.5f, high * 0.8f);
                }
            }
        }

        // 确保min <= max
        if (min > max) {
            float temp = min;
            min = max;
            max = temp;
        }

        return new Float[] { min, max };
    }
    
    private static float getRandomFloat(float min, float max) {
        Random random = new Random();
        return min + random.nextFloat() * (max - min);
    }

    public String getVoltageRule(String batteryType){
        LambdaQueryWrapper<VoltageRuleDO> wrapper = Wrappers.lambdaQuery(VoltageRuleDO.class)
                .eq(VoltageRuleDO::getBatteryType, batteryType);
        List<VoltageRuleDO> rules = voltageRuleMapper.selectList(wrapper);

        if (rules.isEmpty()) {
            // 如果没有找到规则，返回默认规则
            return "0 <= (Mx-Mn) < 20";
        }

        int size = rules.size();
        int index = ThreadLocalRandom.current().nextInt(0, size);
        return rules.get(index).getRule();
    }
    
    public String getCurrentRule(String batteryType){
        LambdaQueryWrapper<CurrentRuleDO> wrapper = Wrappers.lambdaQuery(CurrentRuleDO.class)
                .eq(CurrentRuleDO::getBatteryType, batteryType);
        List<CurrentRuleDO> rules = currentRuleMapper.selectList(wrapper);

        if (rules.isEmpty()) {
            // 如果没有找到规则，返回默认规则
            return "0 <= (Ix-In) < 10";
        }

        int size = rules.size();
        int index = ThreadLocalRandom.current().nextInt(0, size);
        return rules.get(index).getRule();
    }

}
