package com.xy.system.dispatch.content;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 正文图片迁移结果
 * */
@Data
public class ImageMigrationResult {

    /** 重写图片地址后的正文 */
    private String content;

    /** 正文中的图片总数 */
    private int totalImages;

    /** 成功迁移到平台图床的图片数 */
    private int migratedCount;

    /** 降级到自有图床的图片数 */
    private int fallbackCount;

    /** 迁移失败的图片数 */
    private int failedCount;

    /** 原地址 -> 新地址 映射 */
    private Map<String, String> urlMapping = new LinkedHashMap<>();

    /** 迁移失败明细 */
    private List<String> failures = new ArrayList<>();
}
