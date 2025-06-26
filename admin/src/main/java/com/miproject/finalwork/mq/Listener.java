package com.miproject.finalwork.mq;



import com.alibaba.fastjson.JSON;
import com.miproject.finalwork.dao.entity.WarnInfoDO;
import com.miproject.finalwork.dao.mapper.WarnInfoMapper;
import com.miproject.finalwork.dto.req.WarnMsgMQReqDTO;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RedissonClient;
import org.redisson.api.RBucket;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;

// 监听器，按优先级处理
@RocketMQMessageListener(
        topic = "warn-level-0-topic||warn-level-1-topic||warn-level-2-topic||warn-level-3-topic||warn-level-4-topic",
        consumerGroup = "bmi-consumer"
)
@Component
public class Listener implements RocketMQListener<MessageExt> {

    @Autowired
    private WarnInfoMapper warnInfoMapper;
    @Autowired
    private RedissonClient redissonClient;

    // 优先级队列，数字越小优先级越高
    private final PriorityBlockingQueue<WarnMsgMQReqDTO> queue =
            new PriorityBlockingQueue<>(100, Comparator.comparingInt(WarnMsgMQReqDTO::getWarnLevel));


    public Listener() {
        // 启动消费线程
        new Thread(() -> {
            while (true) {
                try {
                    WarnMsgMQReqDTO recv = queue.take();
                    // 插入数据库
                    WarnInfoDO warnInfoDO = new WarnInfoDO();
                    BeanUtils.copyProperties(recv, warnInfoDO);
                    warnInfoMapper.insert(warnInfoDO);
                    // 写入Redis
                    String redisKey = String.format("warn:%s:%d", recv.getVid(), recv.getWarnId());
                    RBucket<String> bucket = redissonClient.getBucket(redisKey);
                    bucket.set(JSON.toJSONString(recv), 1, java.util.concurrent.TimeUnit.HOURS);
                } catch (Exception ignored) {}
            }
        }, "warn-priority-consumer").start();
    }

    @Override
    public void onMessage(MessageExt messageExt){
        System.out.println("Listener is working");
        String topic = messageExt.getTopic();
        WarnMsgMQReqDTO recv = JSON.parseObject(messageExt.getBody(), WarnMsgMQReqDTO.class);
        // 解析优先级
        int priority = 9;
        try {
            String[] arr = topic.split("-");
            if (arr.length > 2) {
                priority = Integer.parseInt(arr[2]);
            }
        } catch (Exception ignored) {}
        // 优先级覆盖warnLevel字段
        recv.setWarnLevel(priority);
        // 加入优先级队列
        queue.put(recv);
    }
}
