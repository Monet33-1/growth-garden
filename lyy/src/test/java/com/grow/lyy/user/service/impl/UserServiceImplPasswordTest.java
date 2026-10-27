package com.grow.lyy.user.service.impl;

import com.grow.lyy.common.exception.BusinessException;
import com.grow.lyy.common.jwt.JwtUtil;
import com.grow.lyy.garden.service.GardenService;
import com.grow.lyy.user.entity.User;
import com.grow.lyy.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplPasswordTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserServiceImpl userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(mock(JwtUtil.class), userMapper, mock(GardenService.class), encoder);
        user = new User();
        user.setId(7L);
        user.setPassword(encoder.encode("garden-old-123"));
        when(userMapper.selectById(7L)).thenReturn(user);
    }

    @Test
    void changesPasswordAfterValidatingCurrentAndConfirmation() {
        userService.changePassword(7L, "garden-old-123", "garden-new-456", "garden-new-456");

        var update = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(update.capture());
        String storedPassword = update.getValue().getPassword();
        assertFalse("garden-new-456".equals(storedPassword));
        assertTrue(encoder.matches("garden-new-456", storedPassword));
        assertFalse(encoder.matches("garden-old-123", storedPassword));
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutUpdating() {
        assertThrows(BusinessException.class, () -> userService.changePassword(
                7L, "wrong-password", "garden-new-456", "garden-new-456"));
        verify(userMapper, never()).updateById(any(User.class));
        assertTrue(encoder.matches("garden-old-123", user.getPassword()));
    }

    @Test
    void rejectsMismatchedConfirmationWithoutUpdating() {
        assertThrows(BusinessException.class, () -> userService.changePassword(
                7L, "garden-old-123", "garden-new-456", "different-new-456"));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void rejectsReusingCurrentPasswordWithoutUpdating() {
        assertThrows(BusinessException.class, () -> userService.changePassword(
                7L, "garden-old-123", "garden-old-123", "garden-old-123"));
        verify(userMapper, never()).updateById(any(User.class));
        assertTrue(encoder.matches("garden-old-123", user.getPassword()));
    }
}
