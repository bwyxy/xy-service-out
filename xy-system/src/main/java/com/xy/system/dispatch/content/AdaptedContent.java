package com.xy.system.dispatch.content;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 内容适配结果
 * */
@Data
public class AdaptedContent {

    /** 适配后的标题 */
    private String title;

    /** 适配后的摘要 */
    private String summary;

    /** 适配后的正文 */
    private String content;

    /** 适配后的标签 */
    private List<String> tags = new ArrayList<>();

    /** 适配过程中的提示信息（如超长被裁剪） */
    private List<String> warnings = new ArrayList<>();
}
