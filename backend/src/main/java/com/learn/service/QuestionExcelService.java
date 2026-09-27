package com.learn.service;

import com.learn.vo.ImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 题库 Excel 导入导出服务（文档 4.4）
 */
public interface QuestionExcelService {

    /** 下载导入模板 */
    void downloadTemplate(HttpServletResponse response);

    /**
     * 批量导入：流式读取、500 条一批、批内 Set 查重、
     * 查重键 = 分类 + 题干 MD5（去空白归一化）、duplicateStrategy = SKIP/OVERWRITE/ERROR
     */
    ImportResultVO importExcel(MultipartFile file, String duplicateStrategy);

    /** 按条件批量导出 */
    void export(Long categoryId, Integer isVip, Integer difficulty, Integer status,
                HttpServletResponse response);
}
