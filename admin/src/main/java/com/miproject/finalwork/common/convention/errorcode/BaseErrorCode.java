/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miproject.finalwork.common.convention.errorcode;

/**
 * 基础错误码定义
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
public enum BaseErrorCode implements IErrorCode {

    // ========== 一级宏观错误码 客户端错误 ==========
    CLIENT_ERROR("A001", "用户端错误"),

    // ========== 二级宏观错误码 用户注册错误 ==========
    DATA_ERROR("A002", "数据不存在"),
    DATA_VALIDATION_ERROR("A003", "数据非法"),

    // ========== 二级宏观错误码 系统请求操作频繁 ==========
    FLOW_LIMIT_ERROR("A011", "当前系统繁忙，请稍后再试"),

    // ========== 一级宏观错误码 系统执行出错 ==========
    SERVICE_ERROR("B001", "系统执行出错"),
    // ========== 二级宏观错误码 系统执行超时 ==========
    SERVICE_TIMEOUT_ERROR("B100", "系统执行超时"),
    // ========== 二级宏观错误码 系统执行超时 ==========
    SERVICE_HANDLER_ERROR("B111", "系统执行错误"),
    // ========== 一级宏观错误码 调用第三方服务出错 ==========
    REMOTE_ERROR("C0001", "调用第三方服务出错");

    private final String code;

    private final String message;

    BaseErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
