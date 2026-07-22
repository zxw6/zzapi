package com.zxw.modules.system.controller;

import com.zxw.common.api.ApiResponse;
import com.zxw.modules.system.dto.SiteSettingsResponse;
import com.zxw.modules.system.dto.SiteSettingsUpdateRequest;
import com.zxw.modules.system.service.AdminSiteSettingsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/system")
@Api(tags = "系统管理")
/**
 * 系统管理控制器。
 * 提供健康检查、SSE 协议说明和站点设置维护接口。
 */
public class AdminSystemController {

    private final AdminSiteSettingsService adminSiteSettingsService;

    public AdminSystemController(AdminSiteSettingsService adminSiteSettingsService) {
        this.adminSiteSettingsService = adminSiteSettingsService;
    }

    /**
     * 返回系统健康状态。
     */
    @GetMapping("/health")
    @ApiOperation("健康检查")
    public ApiResponse<Map<String, Object>> health() {
        // 返回服务运行状态
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "ai-gateway",
                "timestamp", OffsetDateTime.now()
        ));
    }

    /**
     * 说明前端如何消费 SSE 协议事件。
     */
    @GetMapping("/sse-protocol")
    @ApiOperation("查看SSE协议说明")
    public ApiResponse<Map<String, Object>> sseProtocol() {
        // 返回SSE事件流格式说明
        return ApiResponse.ok(Map.of(
                "version", "1.0",
                "transport", "text/event-stream (SSE)",
                "format", "data: <JSON>\\n\\n",
                "events", List.of(
                        Map.of("type", "response.created",
                                "desc", "会话已创建，返回 response.id 和 model",
                                "fields", List.of("response.id", "response.model", "response.status")),
                        Map.of("type", "response.agent.status",
                                "desc", "Agent 状态变更",
                                "kind_values", List.of(
                                        "session.ready — 工作区就绪",
                                        "thinking — 正在推理（含 step 编号）",
                                        "tool.running — 开始执行工具",
                                        "tool.completed — 工具执行成功",
                                        "tool.failed — 工具执行失败",
                                        "tool.retry — 重新尝试",
                                        "tool.heuristic — 启发式工具调用")),
                        Map.of("type", "response.output_item.added",
                                "desc", "工具调用或输出开始",
                                "item_types", List.of("function_call", "function_call_output", "message")),
                        Map.of("type", "response.output_item.done",
                                "desc", "工具调用或输出完成"),
                        Map.of("type", "response.agent.file_changed",
                                "desc", "文件被修改，file 对象含 path / before_preview / after_preview"),
                        Map.of("type", "response.output_text.delta",
                                "desc", "最终回答的增量文本片段",
                                "fields", List.of("delta")),
                        Map.of("type", "response.output_text.done",
                                "desc", "最终回答完成",
                                "fields", List.of("text")),
                        Map.of("type", "response.completed",
                                "desc", "整个响应结束")
                ),
                "example_flow", List.of(
                        "1. response.created",
                        "2. response.agent.status (session.ready)",
                        "3. response.agent.status (thinking, step=1)",
                        "4. response.agent.status (tool.running)",
                        "5. response.output_item.added (function_call)",
                        "6. response.output_item.done (function_call)",
                        "7. response.output_item.added (function_call_output)",
                        "8. response.agent.file_changed (if applicable)",
                        "9. response.output_item.done (function_call_output)",
                        "10. response.agent.status (tool.completed)",
                        "11. response.output_item.added (message)",
                        "12. response.output_text.delta (streaming)",
                        "13. response.output_text.done",
                        "14. response.completed"
                )
        ));
    }

    /**
     * 查询站点基础设置。
     */
    @GetMapping("/site-settings")
    @ApiOperation("获取站点设置")
    public ApiResponse<SiteSettingsResponse> getSiteSettings() {
        // 查询站点基础配置
        return ApiResponse.ok(adminSiteSettingsService.getSiteSettings());
    }

    /**
     * 保存站点设置。
     */
    @PutMapping("/site-settings")
    @ApiOperation("更新站点设置")
    public ApiResponse<Void> updateSiteSettings(@Valid @RequestBody SiteSettingsUpdateRequest request) {
        // 更新站点名称、邮箱和主题等配置
        adminSiteSettingsService.updateSiteSettings(request);
        return ApiResponse.ok("站点信息保存成功", null);
    }
}
