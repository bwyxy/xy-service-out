package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoUserSeq;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户同步序号发号器 Mapper（表 t_todo_user_seq）
 * */
@Mapper
public interface UserSeqMapper extends BaseMapper<TodoUserSeq> {

    /**
     * 自增并返回当前序号（首次发号自动建行）
     * */
    @Insert("insert into t_todo_user_seq (user_id, seq) values (#{userId}, 1) "
            + "on duplicate key update seq = seq + 1")
    int increaseSeq(@Param("userId") Long userId);

    /**
     * 读取当前序号
     * */
    @Select("select seq from t_todo_user_seq where user_id = #{userId}")
    Long getSeq(@Param("userId") Long userId);
}
