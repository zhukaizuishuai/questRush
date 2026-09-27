package com.learn.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码返回（文档 4.7）：captchaId + base64 图片
 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    private String captchaId;
    private String image;
}
