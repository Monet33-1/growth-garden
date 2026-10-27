package com.grow.lyy.user.controller;

import com.grow.lyy.user.dto.LoginDTO;
import com.grow.lyy.user.dto.LoginVO;
import com.grow.lyy.user.dto.ChangePasswordDTO;
import com.grow.lyy.user.service.UserService;
import com.grow.lyy.user.entity.User;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.response.Result;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public LoginVO login(
            @RequestBody LoginDTO dto
    ){

        return userService.login(
                dto
        );

    }
    @GetMapping("/info")
    public User info(
            Authentication authentication
    ){

        Long userId =
                (Long)
                        authentication.getPrincipal();


        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;

    }

    @PutMapping("/password")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordDTO dto,
            Authentication authentication
    ) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof Long userId)
                || userId <= 0
                || authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            throw new BusinessException("请先登录花园主人账号。");
        }
        userService.changePassword(userId, dto.getCurrentPassword(), dto.getNewPassword(), dto.getConfirmPassword());
        return Result.success();
    }



}
