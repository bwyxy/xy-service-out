package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * 单日完成数（统计页柱状图的一根柱子）
 * */
@Data
public class TodoDayCountVO {

    /**
     * 日期 yyyy-MM-dd
     * */
    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 当天完成数，没有完成记录的日子为 0
     * */
    @JsonProperty("total")
    private Long total;
}
