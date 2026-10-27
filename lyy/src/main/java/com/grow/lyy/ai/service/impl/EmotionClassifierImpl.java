package com.grow.lyy.ai.service.impl;


import com.grow.lyy.ai.provider.AIProvider;
import com.grow.lyy.ai.service.EmotionClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.Arrays;
import java.util.List;


@Service
@RequiredArgsConstructor
public class EmotionClassifierImpl implements EmotionClassifier {


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



        String result =
                aiProvider.chat(prompt)
                        .trim();



        return Arrays.stream(
                        result.split("\n")
                )
                .map(
                        emotion -> emotion
                                .replace("。", "")
                                .replace("-", "")
                                .trim()
                )
                .filter(
                        emotion -> !emotion.isBlank()
                )
                .toList();

    }

}