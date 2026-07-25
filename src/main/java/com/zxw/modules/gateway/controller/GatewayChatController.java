package com.zxw.modules.gateway.controller;

import com.zxw.modules.gateway.service.GatewayChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Api(tags = "网关对话接口")
/**
 * 网关对话控制器。
 * 对外兼容 OpenAI 和 Anthropic 常用对话协议入口。
 */
public class GatewayChatController {

    private final GatewayChatService gatewayChatService;

    public GatewayChatController(GatewayChatService gatewayChatService) {
        this.gatewayChatService = gatewayChatService;
    }

    @GetMapping({"/v1/models", "/models"})
    @ApiOperation("查询可用模型")
    /**
     * 查询当前 Key 可访问的模型列表。
     */
    public Map<String, Object> models(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestHeader(value = "x-api-key", required = false) String xApiKey) {
        // 返回当前 Key 可访问的模型列表
        return gatewayChatService.listModels(resolveAuthorization(authorization, xApiKey));
    }

    @PostMapping({"/v1/chat/completions", "/chat/completions"})
    @ApiOperation("OpenAI Chat Completions")
    /**
     * 处理 OpenAI Chat Completions 请求。
     */
    public ResponseEntity<?> chatCompletions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody String body,
                                             HttpServletRequest request) {
        // 兼容 OpenAI chat completions 协议
        return gatewayChatService.chatCompletions(resolveAuthorization(authorization, null), body, request);
    }

    @PostMapping({"/v1/responses", "/responses"})
    @ApiOperation("OpenAI Responses")
    /**
     * 处理 OpenAI Responses 请求。
     */
    public ResponseEntity<?> responses(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody String body,
                                       HttpServletRequest request) {
        // 兼容 OpenAI responses 协议
        return gatewayChatService.responses(resolveAuthorization(authorization, null), body, request);
    }

    @PostMapping({"/v1/images/generations", "/images/generations"})
    @ApiOperation("OpenAI Images Generations")
    /**
     * 处理 OpenAI Images Generations 请求。
     */
    public ResponseEntity<?> imageGenerations(@RequestHeader(value = "Authorization", required = false) String authorization,
                                              @RequestHeader(value = "x-api-key", required = false) String xApiKey,
                                              @RequestBody String body,
                                              HttpServletRequest request) {
        // 兼容 OpenAI images generations 协议
        return gatewayChatService.imageGenerations(resolveAuthorization(authorization, xApiKey), body, request);
    }

    @PostMapping({"/v1/messages", "/messages", "/v1/v1/messages"})
    @ApiOperation("Anthropic Messages")
    /**
     * 处理 Anthropic Messages 请求。
     */
    public ResponseEntity<?> messages(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestHeader(value = "x-api-key", required = false) String xApiKey,
                                      @RequestBody String body,
                                      HttpServletRequest request) {
        // 兼容 Anthropic messages 协议
        return gatewayChatService.anthropicMessages(resolveAuthorization(authorization, xApiKey), body, request);
    }

    /**
     * 统一兼容 `Authorization` 和 `x-api-key` 两种传参方式。
     */
    private String resolveAuthorization(String authorization, String xApiKey) {
        if (authorization != null && !authorization.isBlank()) {
            return authorization;
        }
        if (xApiKey != null && !xApiKey.isBlank()) {
            return "Bearer " + xApiKey;
        }
        return authorization;
    }
}
