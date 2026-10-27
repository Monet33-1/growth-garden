package com.grow.lyy.diary.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grow.lyy.diary.entity.Diary;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface DiaryMapper
        extends BaseMapper<Diary> {

}