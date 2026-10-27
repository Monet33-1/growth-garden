package com.grow.lyy.ai.service;


import java.util.List;


public interface EmotionClassifier {


    List<String> classify(String content);


}