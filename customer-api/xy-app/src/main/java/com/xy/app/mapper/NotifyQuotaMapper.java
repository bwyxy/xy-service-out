package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoSubscribeQuota;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 订阅消息授权额度
 *
 * <p>一次性订阅的关键是「授权一次只能发一条」，所以加减额度都必须是一条原子 SQL：
 * 扣减靠 {@code where remain > 0} 的影响行数判断成败，不能先查后改，否则并发下会超发。</p>
 * */
@Mapper
public interface NotifyQuotaMapper extends BaseMapper<TodoSubscribeQuota> {

    /**
     * 上报授权结果，累加额度；首次授权则插入
     * */
    @Insert("insert into t_todo_subscribe_quota (user_id, template_id, remain, total) "
            + "values (#{userId}, #{templateId}, #{count}, #{count}) "
            + "on duplicate key update remain = remain + #{count}, total = total + #{count}")
    int addQuota(@Param("userId") Long userId, @Param("templateId") String templateId, @Param("count") int count);

    /**
     * 扣减一次额度
     *
     * @return 影响行数，1 表示扣减成功，0 表示额度已耗尽
     * */
    @Update("update t_todo_subscribe_quota set remain = remain - 1, update_time = now() "
            + "where user_id = #{userId} and template_id = #{templateId} and remain > 0")
    int consumeQuota(@Param("userId") Long userId, @Param("templateId") String templateId);

    /**
     * 查额度，没有记录返回 null
     * */
    @Select("select user_id, template_id, remain, total from t_todo_subscribe_quota "
            + "where user_id = #{userId} and template_id = #{templateId}")
    TodoSubscribeQuota selectQuota(@Param("userId") Long userId, @Param("templateId") String templateId);
}
