package com.grow.lyy.desktop;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.user.dto.LoginVO;
import com.grow.lyy.user.dto.SetupDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** First-run account setup, available only in the Windows desktop profile. */
@RestController
@RequestMapping("/user/setup")
@Profile("desktop")
@RequiredArgsConstructor
public class DesktopSetupController {

    private final DesktopSetupService setupService;

    @GetMapping("/status")
    public Result<Boolean> status() {
        return Result.success(setupService.isSetupRequired());
    }

    @PostMapping
    public Result<LoginVO> setup(@Valid @RequestBody SetupDTO dto) {
        return Result.success(setupService.setup(dto));
    }
}
