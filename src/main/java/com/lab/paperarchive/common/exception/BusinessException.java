package com.lab.paperarchive.common.exception;

/** 사용자에게 그대로 노출해도 되는 업무 예외 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
