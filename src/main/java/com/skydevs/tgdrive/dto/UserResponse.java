package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String role;
    private String email;
    private String nickname;
    private String avatar;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime lastLoginTime;
    private String phone;
    private Integer status;
    private String remark;
    
    // 状态描述
    public String getStatusDesc() {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "正常";
            case 1: return "禁用";
            case 2: return "锁定";
            default: return "未知";
        }
    }
}