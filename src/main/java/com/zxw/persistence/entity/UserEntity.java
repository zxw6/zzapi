package com.zxw.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
@ApiModel("用户实体")
public class UserEntity {

    @TableId(type = IdType.AUTO)
    @ApiModelProperty("用户ID")
    private Long id;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("密码哈希")
    private String passwordHash;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("手机号")
    private String phone;

    @ApiModelProperty("角色编码")
    private String roleCode;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("删除标记")
    private Integer deleted;

    @ApiModelProperty("是否启用套餐限制")
    private Integer packageRestrictionEnabled;

    @ApiModelProperty("最大并发请求数，空或0表示使用全局默认")
    private Integer maxConcurrentRequests;

    @ApiModelProperty("最大并发流式请求数，空或0表示使用全局默认")
    private Integer maxConcurrentStreams;

    @ApiModelProperty("最后登录时间")
    private LocalDateTime lastLoginAt;

    @ApiModelProperty("最后活跃时间")
    private LocalDateTime lastActiveAt;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty("更新时间")
    private LocalDateTime updatedAt;
}
