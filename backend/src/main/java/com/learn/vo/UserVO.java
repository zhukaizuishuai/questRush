package com.learn.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息 VO：永不下发 password / loginFailCount（entity 不直接出参，文档 4.2/5.1）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private LocalDateTime vipExpireTime;
    private Integer status;
    private LocalDateTime createTime;

    /** VIP 是否有效（实时比较，文档 2.1/10.1） */
    public Boolean getVipValid() {
        return vipExpireTime != null && vipExpireTime.isAfter(LocalDateTime.now());
    }
}
