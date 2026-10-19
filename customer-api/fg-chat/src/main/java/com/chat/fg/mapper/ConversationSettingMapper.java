package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.ConversationSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话个人设置 数据层
 * */
@Mapper
public interface ConversationSettingMapper extends BaseMapper<ConversationSetting> {
}
