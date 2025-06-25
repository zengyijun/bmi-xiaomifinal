package com.miproject.finalwork.handler;

import com.miproject.finalwork.dto.req.ReportReqDTO;
import com.miproject.finalwork.dto.resp.DetailReportRespDTO;

/**
 * @author zengyijun
 * 创建责任链分别处理：电流异常、电压异常、事故上报
 */
// 采用类型擦除和泛型的思路解决入参和返回值的问题
public interface Handler<T, R> {
    /**
     * 处理方法
     * @param funcall 功能调用标识，例如upload|get|detail
     * @param data 输入数据
     * @param ctx 责任链上下文
     * @return 处理结果
     */
    R handle(String funcall, T data, HandlerChainContext ctx);
}
