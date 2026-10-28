package com.grow.lyy.desktop;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.jwt.JwtUtil;
import com.grow.lyy.garden.service.GardenService;
import com.grow.lyy.user.dto.LoginVO;
import com.grow.lyy.user.dto.SetupDTO;
import com.grow.lyy.user.entity.User;
import com.grow.lyy.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/** Creates the single account used by a fresh Windows desktop installation. */
@Service
@Profile("desktop")
@RequiredArgsConstructor
public class DesktopSetupService {

    private final UserMapper userMapper;
    private final GardenService gardenService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final DesktopConfigService desktopConfigService;

    public boolean isSetupRequired() {
        Long count = userMapper.selectCount(Wrappers.<User>lambdaQuery());
        return count == null || count == 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public LoginVO setup(SetupDTO dto) {
        if (!isSetupRequired()) {
            throw new BusinessException("初始化已完成，请直接登录。");
        }

        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();
        if (username.isBlank()) {
            throw new BusinessException("请填写用户名。");
        }

        String password = dto.getPassword();
        if (password == null || password.length() < 8) {
            throw new BusinessException("密码至少需要 8 个字符。");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("密码过长，请控制在 72 字节以内。");
        }

        Long existing = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );
        if (existing != null && existing > 0) {
            throw new BusinessException("该用户名已存在。");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(
                dto.getNickname() == null || dto.getNickname().isBlank()
                        ? username
                        : dto.getNickname().trim()
        );
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);

        gardenService.createGarden(user.getId());

        if (dto.getDeepseekApiKey() != null && !dto.getDeepseekApiKey().isBlank()) {
            desktopConfigService.saveDeepSeekKey(dto.getDeepseekApiKey());
        }

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId()));
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        return vo;
    }
}
