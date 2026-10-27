package com.grow.lyy.letter.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grow.lyy.letter.entity.Letter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


@Mapper
public interface LetterMapper
        extends BaseMapper<Letter> {

    @Select("SELECT * FROM letter WHERE id = #{id} FOR UPDATE")
    Letter selectByIdForUpdate(@Param("id") Long id);


}
