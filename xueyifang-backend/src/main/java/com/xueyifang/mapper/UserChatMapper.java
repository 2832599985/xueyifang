package com.xueyifang.mapper;

import com.xueyifang.domain.po.UserChat;
import com.xueyifang.domain.vo.ChatConversationVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 用户聊天记录表 Mapper 接口
 * </p>
 *
 * @author xueyifang
 * @since 2025-11-24
 */
public interface UserChatMapper extends BaseMapper<UserChat> {

    List<ChatConversationVO> selectConversations(@Param("userId") Long userId);

}
