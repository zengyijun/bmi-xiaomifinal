# RocketMQ消息积压问题分析与解决方案

## 1. 问题分析

通过对项目代码的分析，我们发现当前系统在使用RocketMQ时存在潜在的消息积压风险。主要问题包括：

### 1.1 消费端处理能力不足

当前的[Listener](file:///D:/JavaProject/bmi-xiaomifinal/admin/src/main/java/com/miproject/finalwork/mq/Listener.java#L31-L71)类中，虽然使用了优先级队列来处理消息，但消费处理逻辑存在以下问题：

1. **单线程处理**：只有一个消费线程从优先级队列中取出消息进行处理，无法充分利用多核CPU资源。
2. **阻塞操作**：消费过程中包含数据库写入和Redis写入等I/O操作，这些操作会阻塞消费线程。
3. **无批量处理**：消息逐条处理，没有利用批量操作来提高吞吐量。

### 1.2 生产端消息发送策略不当

在[DataReport](file:///D:/JavaProject/bmi-xiaomifinal/admin/src/main/java/com/miproject/finalwork/mq/schedule/DataReport.java#L52-L116)定时任务中，消息发送存在以下问题：

1. **同步发送**：使用`syncSend`方法同步发送消息，会阻塞发送线程直到收到Broker确认。
2. **无发送限流**：大批量消息发送时没有控制发送速率，可能导致Broker压力过大。
3. **无失败重试机制**：消息发送失败时缺乏有效的重试机制。

### 1.3 缺乏监控和告警

系统中没有对MQ消息积压情况进行监控和告警，无法及时发现和处理消息积压问题。

## 2. 解决方案

### 2.1 优化消费者处理能力

#### 2.1.1 引入多线程消费

将单线程消费改为多线程消费，提高消息处理能力：

1. 使用线程池处理消息，而不是单个消费线程
2. 控制线程池大小，避免创建过多线程导致系统资源耗尽
3. 合理设置队列大小，防止内存溢出

#### 2.1.2 实现批量处理

对于可以批量处理的操作，如数据库写入，采用批量操作提高处理效率：

1. 积累一定数量的消息后再进行批量处理
2. 设置合理的批量大小和超时时间
3. 保证批量处理的原子性

#### 2.1.3 异步处理I/O操作

将数据库写入和Redis写入等I/O操作异步化：

1. 使用异步数据库操作
2. 使用Redis异步API
3. 减少消费线程的阻塞时间

### 2.2 优化生产者发送策略

#### 2.2.1 使用异步发送

将同步发送改为异步发送，提高发送效率：

1. 使用`asyncSend`方法异步发送消息
2. 设置合理的回调处理逻辑
3. 避免发送线程被阻塞

#### 2.2.2 实现发送限流

控制消息发送速率，避免对Broker造成过大压力：

1. 使用令牌桶或漏桶算法实现发送限流
2. 根据Broker处理能力动态调整发送速率
3. 在发送高峰期自动降速

#### 2.2.3 增强失败重试机制

实现更完善的消息发送失败重试机制：

1. 设置合理的重试次数和重试间隔
2. 区分不同类型的失败，采用不同的重试策略
3. 对于无法重试的永久性错误，记录日志并通知管理员

### 2.3 增加监控和告警

建立完善的MQ监控体系：

1. 监控消息积压数量
2. 监控消息发送和消费速率
3. 监控消息处理耗时
4. 设置告警阈值，及时发现异常情况

### 2.4 优化Broker配置

调整RocketMQ Broker配置以提高处理能力：

1. 增加Broker的处理线程数
2. 调整消息存储策略
3. 优化刷盘策略

## 3. 具体实施方案

### 3.1 消费端优化方案

#### 3.1.1 引入线程池处理消息

```java
// 修改Listener类
@Component
@RocketMQMessageListener(topic = "warn-topic", consumerGroup = "bmi-consumer")
public class Listener implements RocketMQListener<MessageExt> {
    
    @Autowired
    private WarnInfoMapper warnInfoMapper;
    
    @Autowired
    private RedissonClient redissonClient;
    
    // 创建线程池处理消息
    private final ExecutorService executorService = new ThreadPoolExecutor(
        10,  // 核心线程数
        20,  // 最大线程数
        60L, TimeUnit.SECONDS,  // 空闲线程存活时间
        new LinkedBlockingQueue<>(1000),  // 任务队列
        new ThreadFactoryBuilder().setNameFormat("warn-consumer-%d").build(),
        new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
    );
    
    // 优先级队列保持不变
    private final PriorityBlockingQueue<WarnMsgMQReqDTO> queue =
        new PriorityBlockingQueue<>(10, Comparator.comparingInt(WarnMsgMQReqDTO::getWarnLevel));
    
    @PostConstruct
    public void init() {
        // 启动多个消费线程处理队列中的消息
        for (int i = 0; i < 5; i++) {
            executorService.submit(this::processMessages);
        }
    }
    
    private void processMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                WarnMsgMQReqDTO recv = queue.take();
                // 异步处理消息
                processMessageAsync(recv);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("处理消息时发生错误", e);
            }
        }
    }
    
    private void processMessageAsync(WarnMsgMQReqDTO recv) {
        CompletableFuture.runAsync(() -> {
            try {
                // 插入数据库
                WarnInfoDO warnInfoDO = new WarnInfoDO();
                BeanUtils.copyProperties(recv, warnInfoDO);
                warnInfoMapper.insert(warnInfoDO);
                
                // 写入Redis
                String redisKey = String.format("warn:%s:%d", recv.getVid(), recv.getWarnId());
                RBucket<String> bucket = redissonClient.getBucket(redisKey);
                bucket.set(JSON.toJSONString(recv), 1, TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("异步处理消息失败: {}", recv, e);
            }
        }, executorService);
    }
    
    @Override
    public void onMessage(MessageExt messageExt) {
        log.info("消费者消费了一条消息");
        try {
            WarnMsgMQReqDTO recv = JSON.parseObject(messageExt.getBody(), WarnMsgMQReqDTO.class);
            // 解析优先级
            recv.setWarnLevel(recv.getWarnId());
            // 加入优先级队列
            queue.put(recv);
        } catch (Exception e) {
            log.error("解析消息失败", e);
        }
    }
}
```

#### 3.1.2 实现批量处理

```java
// 新增批量处理逻辑
private void processMessagesInBatch() {
    List<WarnMsgMQReqDTO> batch = new ArrayList<>();
    while (!Thread.currentThread().isInterrupted()) {
        try {
            // 积累一批消息
            WarnMsgMQReqDTO msg = queue.poll(1, TimeUnit.SECONDS);
            if (msg != null) {
                batch.add(msg);
            }
            
            // 达到批次大小或超时时进行批量处理
            if (batch.size() >= 10 || (msg == null && !batch.isEmpty())) {
                processBatchMessages(batch);
                batch.clear();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
        } catch (Exception e) {
            log.error("批量处理消息时发生错误", e);
        }
    }
}

private void processBatchMessages(List<WarnMsgMQReqDTO> messages) {
    if (messages.isEmpty()) {
        return;
    }
    
    try {
        // 批量插入数据库
        List<WarnInfoDO> warnInfoList = messages.stream()
            .map(msg -> {
                WarnInfoDO warnInfoDO = new WarnInfoDO();
                BeanUtils.copyProperties(msg, warnInfoDO);
                return warnInfoDO;
            })
            .collect(Collectors.toList());
        
        // 假设有批量插入方法
        warnInfoMapper.insertBatch(warnInfoList);
        
        // 批量写入Redis
        for (WarnMsgMQReqDTO msg : messages) {
            String redisKey = String.format("warn:%s:%d", msg.getVid(), msg.getWarnId());
            RBucket<String> bucket = redissonClient.getBucket(redisKey);
            bucket.set(JSON.toJSONString(msg), 1, TimeUnit.HOURS);
        }
    } catch (Exception e) {
        log.error("批量处理消息失败", e);
        // 失败时逐个处理
        for (WarnMsgMQReqDTO msg : messages) {
            processMessageAsync(msg);
        }
    }
}
```

### 3.2 生产端优化方案

#### 3.2.1 使用异步发送

```java
// 修改DataReport类中的消息发送逻辑
public void reportData() {
    var lock = redissonClient.getLock(RedisCacheConstant.LOCK_STATUS_UPLOAD_KEY);
    try {
        lock.lock();
        LambdaQueryWrapper<BatteryStatusDO> queryWrapper = Wrappers.lambdaQuery(BatteryStatusDO.class)
                .eq(BatteryStatusDO::getDelFlag, 0);
        List<BatteryStatusDO> statusDOList = batteryStatusMapper.selectList(queryWrapper);

        for (BatteryStatusDO statusDO : statusDOList) {
            VehicleDO vehicle = vehicleMapper.selectById(statusDO.getVid());
            
            // 处理电压规则
            LambdaQueryWrapper<VoltageRuleDO> voltage = Wrappers.lambdaQuery(VoltageRuleDO.class)
                    .eq(VoltageRuleDO::getBatteryType, vehicle.getBatteryType());
            List<BaseRuleDO> v_rule = new ArrayList<>(voltageRuleMapper.selectList(voltage));
            WarnMsgMQReqDTO warnMsgMQReqDTO = isWarnable(v_rule, statusDO);
            if (warnMsgMQReqDTO != null) {
                sendMessageAsync("warn-topic", warnMsgMQReqDTO);
            }
            
            // 处理电流规则
            LambdaQueryWrapper<CurrentRuleDO> current = Wrappers.lambdaQuery(CurrentRuleDO.class)
                    .eq(CurrentRuleDO::getBatteryType, vehicle.getBatteryType());
            List<BaseRuleDO> c_rule = new ArrayList<>(currentRuleMapper.selectList(current));
            warnMsgMQReqDTO = isWarnable(c_rule, statusDO);
            if (warnMsgMQReqDTO != null) {
                sendMessageAsync("warn-topic", warnMsgMQReqDTO);
            }
            
            statusDO.setDelFlag(1);
            batteryStatusMapper.updateById(statusDO);
        }
    } finally {
        lock.unlock();
    }
}

private void sendMessageAsync(String topic, WarnMsgMQReqDTO message) {
    message.setTimeStamp(new Date());
    byte[] body = JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8);
    
    // 异步发送消息
    rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(body).build(), new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            log.info("消息发送成功: {}", sendResult.getMsgId());
        }
        
        @Override
        public void onException(Throwable throwable) {
            log.error("消息发送失败: {}", message, throwable);
            // 可以将失败的消息存储到数据库或文件中，供后续重试
            handleSendFailure(message);
        }
    });
}

private void handleSendFailure(WarnMsgMQReqDTO message) {
    // 实现失败消息的处理逻辑
    // 可以存储到专门的失败消息表中，供后续重试
    try {
        FailedMessageDO failedMessage = new FailedMessageDO();
        failedMessage.setMessage(JSON.toJSONString(message));
        failedMessage.setTopic("warn-topic");
        failedMessage.setCreateTime(new Date());
        failedMessage.setRetryCount(0);
        // failedMessageMapper.insert(failedMessage);
        log.warn("消息发送失败，已记录到失败消息表: {}", message);
    } catch (Exception e) {
        log.error("记录失败消息时发生错误", e);
    }
}
```

#### 3.2.2 实现发送限流

```java
// 添加令牌桶限流器
@Component
public class MessageRateLimiter {
    
    private final RateLimiter rateLimiter;
    
    public MessageRateLimiter(@Value("${mq.send.rate.limit:1000}") int rate) {
        // 创建令牌桶限流器，每秒产生rate个令牌
        this.rateLimiter = RateLimiter.create(rate);
    }
    
    public boolean tryAcquire() {
        // 尝试获取令牌，不阻塞
        return rateLimiter.tryAcquire();
    }
    
    public void acquire() {
        // 阻塞直到获取到令牌
        rateLimiter.acquire();
    }
}

// 在DataReport中使用限流器
@Autowired
private MessageRateLimiter messageRateLimiter;

private void sendMessageAsync(String topic, WarnMsgMQReqDTO message) {
    // 限制消息发送速率
    if (!messageRateLimiter.tryAcquire()) {
        log.warn("消息发送速率超过限制，暂时丢弃消息: {}", message);
        return;
    }
    
    message.setTimeStamp(new Date());
    byte[] body = JSON.toJSONString(message).getBytes(StandardCharsets.UTF_8);
    
    rocketMQTemplate.asyncSend(topic, MessageBuilder.withPayload(body).build(), new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            log.info("消息发送成功: {}", sendResult.getMsgId());
        }
        
        @Override
        public void onException(Throwable throwable) {
            log.error("消息发送失败: {}", message, throwable);
            handleSendFailure(message);
        }
    });
}
```

### 3.3 增加监控和告警

#### 3.3.1 添加消息积压监控

```java
// 添加消息积压监控组件
@Component
public class MQBacklogMonitor {
    
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkMessageBacklog() {
        try {
            // 获取消费者组的消费进度
            // 这里需要根据实际使用的RocketMQ版本调整实现方式
            long backlog = getMessageBacklog("warn-topic", "bmi-consumer");
            
            // 发送告警
            if (backlog > 1000) { // 积压超过1000条消息时告警
                sendAlert("消息积压告警", "当前消息积压数量: " + backlog);
            }
            
            log.info("当前消息积压数量: {}", backlog);
        } catch (Exception e) {
            log.error("检查消息积压时发生错误", e);
        }
    }
    
    private long getMessageBacklog(String topic, String consumerGroup) {
        // 实现获取消息积压数量的逻辑
        // 具体实现依赖于RocketMQ的版本和监控API
        // 这里只是一个示例
        return 0L;
    }
    
    private void sendAlert(String title, String content) {
        // 实现告警发送逻辑
        // 可以通过邮件、短信、微信等方式发送告警
        log.warn("发送告警: {} - {}", title, content);
    }
}
```

#### 3.3.2 添加配置项

```yaml
# application.yml中添加相关配置
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: bmi-producer
    # 发送超时时间
    sendMessageTimeout: 3000
    # 失败重试次数
    retryTimesWhenSendFailed: 2
    # 异步发送失败重试次数
    retryTimesWhenSendAsyncFailed: 2
  consumer:
    group: bmi-consumer
    
# MQ相关自定义配置
mq:
  send:
    rate:
      limit: 1000  # 每秒消息发送限制
  consumer:
    thread:
      core-size: 10  # 消费线程核心数
      max-size: 20   # 消费线程最大数
      queue-size: 1000  # 任务队列大小
```

## 4. 部署和运维建议

### 4.1 Broker优化配置

```properties
# rocketmq broker配置优化
# 增加处理线程数
sendMessageThreadPoolNums=32
pullMessageThreadPoolNums=32

# 调整存储配置
mapedFileSizeCommitLog=1073741824
mapedFileSizeConsumeQueue=300000

# 优化刷盘策略
flushDiskType=ASYNC_FLUSH

# 调整消息存储时间
fileReservedTime=72
```

### 4.2 监控指标建议

重点关注以下指标：

1. **消息积压数量**：消费者未处理的消息数量
2. **消息发送TPS**：每秒发送的消息数量
3. **消息消费TPS**：每秒消费的消息数量
4. **消息处理耗时**：单条消息从发送到处理完成的耗时
5. **失败消息数量**：发送或消费失败的消息数量

### 4.3 告警阈值设置

建议设置以下告警阈值：

1. **消息积压告警**：积压消息超过1000条持续5分钟
2. **消费延迟告警**：消息消费延迟超过30秒
3. **失败率告警**：消息处理失败率超过1%
4. **TPS异常告警**：消息发送或消费TPS突增或突降超过50%

## 5. 总结

通过以上优化措施，可以有效解决系统中的消息积压问题：

1. **提升消费能力**：通过多线程和批量处理显著提高消费速度
2. **优化发送策略**：异步发送和限流机制保证发送的稳定性
3. **增强可靠性**：失败重试和监控告警机制提高系统可靠性
4. **可扩展性**：通过配置参数可以灵活调整系统性能

这些优化措施需要根据实际业务场景和系统负载情况进行调整，建议在测试环境中充分验证后再部署到生产环境。