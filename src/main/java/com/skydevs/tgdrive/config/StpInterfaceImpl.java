package com.skydevs.tgdrive.config;

import cn.dev33.satoken.stp.StpInterface;
import com.skydevs.tgdrive.entity.User;
import com.skydevs.tgdrive.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * 自定义权限验证接口
 * @author SkyDev
 * @date 2025-08-13 15:24:26
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private UserService userService;

    /**
     * Description:
     * 返回一个用户所拥有的权限码集合
     * @author SkyDev
     * @date 2025-08-13 15:25:10
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return 用户所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    /**
     * Description:
     * 返回一个用户所拥有的角色标识集合
     * @author SkyDev
     * @date 2025-08-13 15:25:37
     * @param loginId  账号id
     * @param loginType 账号类型
     * @return 用户所拥有的角色标识集合
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 1. 把 loginId 转换成用户ID类型
        long userId = Long.parseLong(String.valueOf(loginId));

        // 2. 从数据库查询用户
        User user = userService.getById(userId);

        // 3. 如果用户存在，并且有角色字段
        if (user != null && user.getRole() != null) {
            List<String> roleList = new ArrayList<>();
            roleList.add(user.getRole());
            return roleList;
        }

        // 4. 如果没查到，就返回空列表喵~
        return new ArrayList<>();
    }
}