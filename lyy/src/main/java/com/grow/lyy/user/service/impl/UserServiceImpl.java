package com.grow.lyy.user.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grow.lyy.common.jwt.JwtUtil;
import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.user.dto.LoginDTO;
import com.grow.lyy.user.dto.LoginVO;
import com.grow.lyy.user.entity.User;
import com.grow.lyy.user.mapper.UserMapper;
import com.grow.lyy.user.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;



@Service
@RequiredArgsConstructor
public class UserServiceImpl
        implements UserService {

    private final JwtUtil jwtUtil;


    private final UserMapper userMapper;


    private final BCryptPasswordEncoder passwordEncoder;


    @Override
    public User login(
            String username,
            String password
    ){


        User user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<User>()
                                .eq(
                                        User::getUsername,
                                        username
                                )
                );


        if(user==null){

            throw new RuntimeException(
                    "用户不存在"
            );
        }
        boolean match =
                passwordEncoder.matches(
                        password,
                        user.getPassword()
                );
        if(!match){
            throw new RuntimeException(
                    "密码错误"
            );
        }
        return user;
    }
    @Override
    public User getById(Long id){

        return userMapper.selectById(id);

    }

    @Override
    public LoginVO login(
            LoginDTO dto
    ){

        User user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<User>()

                                .eq(
                                        User::getUsername,
                                        dto.getUsername()
                                )
                );


        if(user==null){

            throw new RuntimeException(
                    "用户不存在"
            );
        }



        boolean result =
                passwordEncoder.matches(
                        dto.getPassword(),
                        user.getPassword()
                );


        if(!result){

            throw new RuntimeException(
                    "密码错误"
            );
        }



        String token =
                jwtUtil.generateToken(
                        user.getId()
                );



        LoginVO vo =
                new LoginVO();


        vo.setToken(token);

        vo.setUserId(
                user.getId()
        );

        vo.setNickname(
                user.getNickname()
        );


        return vo;

    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("账号不存在，请重新登录后再试。");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("当前密码不正确，请重新输入。");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次输入的新密码不一致。");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException("新密码需要与当前密码不同。");
        }

        User passwordUpdate = new User();
        passwordUpdate.setId(userId);
        passwordUpdate.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(passwordUpdate);
    }
}
