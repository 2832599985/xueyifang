package com.xueyifang.constant;

/**
 * 聊天常量
 *
 * @author xueyifang
 */
public interface ChatConstant {

    /**
     * 消息类型
     */
    int MESSAGE_TYPE_TEXT = 1;   // 文本消息
    int MESSAGE_TYPE_IMAGE = 2;  // 图片消息
    int MESSAGE_TYPE_FILE = 3;   // 文件消息

    /**
     * 已读状态
     */
    int READ_STATUS_UNREAD = 0;  // 未读
    int READ_STATUS_READ = 1;    // 已读

    /**
     * 获取消息类型名称
     */
    static String getMessageTypeName(int messageType) {
        switch (messageType) {
            case MESSAGE_TYPE_TEXT:
                return "文本";
            case MESSAGE_TYPE_IMAGE:
                return "图片";
            case MESSAGE_TYPE_FILE:
                return "文件";
            default:
                return "未知";
        }
    }
}
