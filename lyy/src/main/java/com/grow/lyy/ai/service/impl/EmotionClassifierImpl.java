package com.grow.lyy.ai.service.impl;


import com.grow.lyy.ai.AiUnavailableException;
import com.grow.lyy.ai.provider.AIProvider;
import com.grow.lyy.ai.service.EmotionClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.List;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionClassifierImpl implements EmotionClassifier {

    private static final Set<String> ALLOWED_EMOTIONS = Set.of(
            "开心", "平静", "难过", "孤独", "感动"
    );


    private final AIProvider aiProvider;



    @Override
    public List<String> classify(String content) {


        String prompt = """
                你是一个情绪分析助手。

                请分析下面这篇随笔包含的主要情绪。

                可以返回多个情绪。


                只能从以下情绪中选择：

                开心
                平静
                难过
                孤独
                感动


                要求：
                1. 每行返回一个情绪
                2. 不要解释
                3. 不要添加序号
                4. 不要添加其他文字


                随笔内容：

                %s
                """.formatted(content);



        try {
            String result = aiProvider.chat(prompt);
            if (result == null || result.isBlank()) {
                return List.of();
            }

            return Arrays.stream(result.split("\\R"))
                    .map(emotion -> emotion
                            .replace("。", "")
                            .replace("-", "")
                            .trim())
                    .filter(ALLOWED_EMOTIONS::contains)
                    .distinct()
                    .toList();
        }
        catch (RuntimeException e) {
            log.warn("AI 情绪分类失败，等待用户手动选择: {}", e.getMessage());
            throw new AiUnavailableException("AI 情绪分类暂不可用", e);
        }

    }

}