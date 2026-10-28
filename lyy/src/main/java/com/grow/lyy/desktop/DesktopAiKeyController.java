package com.grow.lyy.desktop;

import com.grow.lyy.common.config.AIProperties;
import com.grow.lyy.common.response.Result;
import com.grow.lyy.user.dto.AiKeyDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Lets the single desktop account update its own DeepSeek key. */
@RestController
@RequestMapping("/user/ai-key")
@Profile("desktop")
@RequiredArgsConstructor
public class DesktopAiKeyController {

    private final AIProperties aiProperties;
    private final DesktopConfigService desktopConfigService;

    @GetMapping
    public Result<Map<String, Object>> status() {
        return Result.success(currentStatus());
    }

    @PutMapping
    public Result<Map<String, Object>> update(@Valid @RequestBody AiKeyDTO dto) {
        desktopConfigService.saveDeepSeekKey(dto.getApiKey());
        return Result.success(currentStatus());
    }

    private Map<String, Object> currentStatus() {
        String key = aiProperties.getApiKey();
        boolean configured = key != null && !key.isBlank();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", configured);
        result.put("masked", configured ? mask(key) : "");
        return result;
    }

    private String mask(String key) {
        if (key.length() <= 8) {
            return "已配置";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
