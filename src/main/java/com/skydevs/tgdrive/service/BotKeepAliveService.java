package com.skydevs.tgdrive.service;

/**
 * 机器人保活服务接口
 * 用于定期发送保活消息，防止机器人被Telegram停用
 */
public interface BotKeepAliveService {
    
    /**
     * 发送保活消息
     * @return 是否发送成功
     */
    boolean sendKeepAliveMessage();
    
    /**
     * 启动定时保活任务
     */
    void startKeepAliveTask();
    
    /**
     * 停止定时保活任务
     */
    void stopKeepAliveTask();
    
    /**
     * 检查保活任务是否正在运行
     * @return 是否正在运行
     */
    boolean isKeepAliveTaskRunning();
}