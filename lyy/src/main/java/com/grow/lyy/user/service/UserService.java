package com.grow.lyy.user.service;


import com.grow.lyy.user.dto.LoginDTO;
import com.grow.lyy.user.dto.LoginVO;
import com.grow.lyy.user.entity.User;


public interface UserService {


    User getById(Long id);

    User login(
            String username,
            String password
    );
    LoginVO login(LoginDTO dto);

    void changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword);

}
