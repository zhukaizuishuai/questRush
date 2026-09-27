package com.learn.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果（文档 4.4）：{successCount, skippedCount, failCount, failures:[{row, reason}]}
 */
@Data
public class ImportResultVO {

    private int successCount;
    private int skippedCount;
    private int failCount;
    private List<FailureItem> failures = new ArrayList<>();

    @Data
    public static class FailureItem {
        private int row;
        private String reason;

        public FailureItem() {
        }

        public FailureItem(int row, String reason) {
            this.row = row;
            this.reason = reason;
        }
    }
}
