package com.grow.lyy.emotion.controller;

import com.grow.lyy.common.response.Result;
import com.grow.lyy.common.security.AdminAuthorizationService;
import com.grow.lyy.emotion.entity.Emotion;
import com.grow.lyy.emotion.service.EmotionService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class EmotionControllerTest {

    private final EmotionService emotionService = mock(EmotionService.class);
    private final AdminAuthorizationService adminAuthorizationService =
            mock(AdminAuthorizationService.class);
    private final EmotionController controller =
            new EmotionController(emotionService, adminAuthorizationService);

    @Test
    void createRejectsNonAdminBeforeWriting() {
        doThrow(new AccessDeniedException("forbidden"))
                .when(adminAuthorizationService).requireAdmin();

        assertThrows(AccessDeniedException.class,
                () -> controller.create(new Emotion()));

        verifyNoInteractions(emotionService);
    }

    @Test
    void deleteRejectsNonAdminBeforeDeleting() {
        doThrow(new AccessDeniedException("forbidden"))
                .when(adminAuthorizationService).requireAdmin();

        assertThrows(AccessDeniedException.class,
                () -> controller.delete(5L));

        verifyNoInteractions(emotionService);
    }

    @Test
    void createAllowsAdmin() {
        Emotion emotion = new Emotion();
        emotion.setName("期待");

        Result<?> result = controller.create(emotion);

        verify(adminAuthorizationService).requireAdmin();
        verify(emotionService).createEmotion(emotion);
        assertEquals(200, result.getCode());
    }

    @Test
    void deleteAllowsAdmin() {
        Result<?> result = controller.delete(5L);

        verify(adminAuthorizationService).requireAdmin();
        verify(emotionService).deleteEmotion(5L);
        assertEquals(200, result.getCode());
    }
}
