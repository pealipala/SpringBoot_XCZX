package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

/**
 * 自定义异常抛出类
 * @author : yechaoze
 * @date : 2019/6/6 9:59
 */
public class CustomException extends RuntimeException {

    ResultCode resultCode;//错误码

    public CustomException(ResultCode resultCode){
        this.resultCode=resultCode;
    }

    public ResultCode getResultCode() {
        return resultCode;
    }
}
