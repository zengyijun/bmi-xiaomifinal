package com.miproject.finalwork.service;

import com.miproject.finalwork.common.context.RuleContext;
import com.miproject.finalwork.common.convention.exception.ServiceException;

/**
 * 规则解析服务接口
 */
public interface RuleEvaluationService {

    /**
     * 使用规则上下文评估数据是否符合规则表达式
     *
     * @param rule 规则表达式字符串
     * @param context 规则上下文
     * @return 评估结果
     * @throws ServiceException 当规则表达式解析失败时抛出
     */
    boolean evaluateRule(String rule, RuleContext context) throws ServiceException;

    /**
     * 检查规则表达式的合法性（主要是在规则上传时即检查，保证数据库中的规则一定合法）
     * @param
     */
    boolean checkRule(String rule);
}
