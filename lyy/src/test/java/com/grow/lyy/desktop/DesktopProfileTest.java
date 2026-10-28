package com.grow.lyy.desktop;

import com.grow.lyy.letter.entity.Letter;
import com.grow.lyy.letter.mapper.LetterMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=desktop",
        "spring.datasource.url=jdbc:h2:mem:desktop-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class DesktopProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LetterMapper letterMapper;

    @Autowired
    private DesktopLetterSeeder letterSeeder;

    @Test
    void createsTheSingleDesktopAccountAndGarden() throws Exception {
        mockMvc.perform(get("/user/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        MvcResult setupResult = mockMvc.perform(post("/user/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "desktop-owner",
                                  "password": "garden-password-123",
                                  "nickname": "花园主人"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.nickname").value("花园主人"))
                .andReturn();

        mockMvc.perform(get("/user/setup/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        Assumptions.assumeTrue(
                new ClassPathResource("db/desktop/letters.sql").exists(),
                "private letter seed is not present"
        );

        String token = JsonPath.read(
                setupResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.token"
        );
        mockMvc.perform(get("/letter/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists());

        mockMvc.perform(get("/letter/admin/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/user/ai-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configured").isBoolean());

        mockMvc.perform(post("/letter/admin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "本地新增信件",
                                  "content": "这封信直接写入桌面 H2。",
                                  "unlockType": "DIARY_COUNT",
                                  "unlockValue": 2,
                                  "sort": 99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists());

        Letter letter = letterMapper.selectById(1L);
        assertTrue(letter.getContent().contains("\n"));
        assertFalse(letter.getContent().contains("\\n"));

        mockMvc.perform(put("/letter/admin/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "本地改过的标题",
                                  "content": "本地修改后的正文。",
                                  "unlockType": "DIARY_COUNT",
                                  "unlockValue": 1,
                                  "sort": 0
                                }
                                """))
                .andExpect(status().isOk());

        letterSeeder.run(null);
        assertEquals("本地改过的标题", letterMapper.selectById(1L).getTitle());
    }
}
