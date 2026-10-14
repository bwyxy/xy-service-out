package com.chat.fg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.chat.RtcParticipant;
import org.apache.ibatis.annotations.Mapper;

/**
 * RTC房间参与者 Mapper
 * */
@Mapper
public interface RtcParticipantMapper extends BaseMapper<RtcParticipant> {
}
