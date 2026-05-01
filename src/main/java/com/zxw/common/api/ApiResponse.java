package com.zxw.common.api;

/**
 * 统一接口返回结构。
 * 所有后台接口都尽量使用这个对象包装返回值，便于前端统一处理。
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {

    /**
     * 构造默认成功响应。
     */
    public static <T> ApiResponse<T> ok(T data) {
        // 返回默认成功响应
        return new ApiResponse<>(true, "OK", data);
    }

    /**
     * 构造带自定义提示语的成功响应。
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        // 返回带自定义提示语的成功响应
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 构造失败响应。
     */
    public static <T> ApiResponse<T> fail(String message) {
        // 返回失败响应，data 固定为空
        return new ApiResponse<>(false, message, null);
    }
}
