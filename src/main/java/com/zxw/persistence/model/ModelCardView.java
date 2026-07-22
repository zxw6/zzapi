package com.zxw.persistence.model;

import lombok.Data;

@Data
/**
 * 模型卡片视图对象。
 * 用于模型列表等只需要展示基础信息的场景。
 */
/**
 * 模型卡片视图对象。
 * 用于返回轻量级模型列表和展示信息。
 */
public class ModelCardView {

    private String modelCode;
    private String modelName;
    private String modelType;
}
