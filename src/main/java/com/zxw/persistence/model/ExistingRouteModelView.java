package com.zxw.persistence.model;

import lombok.Data;

@Data
/**
 * 已存在路由模型视图对象。
 * 用于根据渠道和上游模型查找当前平台内已存在的模型记录。
 */
/**
 * 已存在路由模型视图对象。
 * 用于导入模型时判断能否复用已有模型记录。
 */
public class ExistingRouteModelView {

    private Long modelId;
    private String modelCode;
}
