package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 四象限：按紧急程度分四格
 * */
@Data
public class TodoQuadrantVO {

    /**
     * 四个象限，顺序固定：重要且紧急 → 重要不紧急 → 紧急不重要 → 不重要不紧急
     *
     * <p>没有任务的象限也会返回（total 为 0、tasks 为空），前端不用自己补齐四格。</p>
     * */
    @JsonProperty("groups")
    private List<TodoQuadrantGroupVO> groups;
}
