package com.zxw.modules.gateway.controller;

import com.zxw.modules.gateway.service.GatewayChatService;
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
public class GatewayChatController {

    private final GatewayChatService gatewayChatService;

    public GatewayChatController(GatewayChatService gatewayChatService) {
        this.gatewayChatService = gatewayChatService;
    }

    @GetMapping({"/v1/models", "/models"})
    public Map<String, Object> models() {
        return gatewayChatService.listModels();
    }

    @PostMapping({"/v1/chat/completions", "/chat/completions"})
    public ResponseEntity<?> chatCompletions(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestBody String body,
                                             HttpServletRequest request) {
        return gatewayChatService.chatCompletions(resolveAuthorization(authorization, null), body, request);
    }

    @PostMapping({"/v1/responses", "/responses"})
    public ResponseEntity<?> responses(@RequestHeader(value = "Authorization", required = false) String authorization,
                                       @RequestBody String body,
                                       HttpServletRequest request) {
        return gatewayChatService.responses(resolveAuthorization(authorization, null), body, request);
    }

    @PostMapping({"/v1/messages", "/messages", "/v1/v1/messages"})
    public ResponseEntity<?> messages(@RequestHeader(value = "Authorization", required = false) String authorization,
                                      @RequestHeader(value = "x-api-key", required = false) String xApiKey,
                                      @RequestBody String body,
                                      HttpServletRequest request) {
        return gatewayChatService.anthropicMessages(resolveAuthorization(authorization, xApiKey), body, request);
    }

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
