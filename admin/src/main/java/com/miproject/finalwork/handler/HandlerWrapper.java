package com.miproject.finalwork.handler;

import com.miproject.finalwork.common.convention.errorcode.BaseErrorCode;
import com.miproject.finalwork.common.convention.errorcode.IErrorCode;
import com.miproject.finalwork.common.convention.exception.ClientException;
import com.miproject.finalwork.common.convention.exception.RemoteException;
import com.miproject.finalwork.common.convention.exception.ServiceException;
import lombok.Getter;

import java.lang.reflect.Method;

public class HandlerWrapper {
    @Getter
    private final Object handler;
    private final Method method;

    public HandlerWrapper(Object handler, Method method) {
        this.handler = handler;
        this.method = method;
    }

    public void invoke(String funcall, Object input, HandlerChainContext context) {
        try {
            method.invoke(handler, funcall, input, context);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if(cause instanceof ServiceException){
                throw (ServiceException) cause;
            } else if (cause instanceof ClientException) {
                throw (ClientException) cause;
            }else if(cause instanceof RemoteException) {
                throw (RemoteException) cause;
            } else {
                throw new ServiceException(BaseErrorCode.SERVICE_HANDLER_ERROR);
            }
        }
    }
}
