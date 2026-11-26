package com.xueyifang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xueyifang.domain.po.CreditScoreLog;
import com.xueyifang.domain.po.User;
import com.xueyifang.mapper.CreditScoreLogMapper;
import com.xueyifang.mapper.UserMapper;
import com.xueyifang.service.ICreditScoreService;
import com.xueyifang.utils.RedisUtils;
import com.xueyifang.utils.TransactionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CreditScoreServiceImpl extends ServiceImpl<CreditScoreLogMapper, CreditScoreLog> implements ICreditScoreService {

    public static final int DEFAULT_SCORE = 100;
    public static final int MAX_SCORE = 120;
    public static final int MIN_SCORE = 0;

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisUtils redisUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustCreditScore(Long userId, int delta, String reasonCode, String reasonDesc, Long relatedOrderId) {
        if (userId == null || delta == 0) {
            return DEFAULT_SCORE;
        }
        User user = userMapper.selectForUpdate(userId);
        if (user == null) {
            return DEFAULT_SCORE;
        }
        int before = user.getCreditScore() != null ? user.getCreditScore() : DEFAULT_SCORE;
        int after = Math.max(MIN_SCORE, Math.min(MAX_SCORE, before + delta));
        if (after != before) {
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getCreditScore, after));
        }

        CreditScoreLog logEntry = new CreditScoreLog();
        logEntry.setUserId(userId);
        logEntry.setDelta(after - before);
        logEntry.setScoreBefore(before);
        logEntry.setScoreAfter(after);
        logEntry.setReasonCode(reasonCode);
        logEntry.setReasonDesc(reasonDesc);
        logEntry.setRelatedOrderId(relatedOrderId);
        logEntry.setCreateTime(LocalDateTime.now());
        this.save(logEntry);

        if (redisUtils != null) {
            TransactionUtils.afterCommit(() -> redisUtils.deleteUserCache(userId));
        }
        return after;
    }

    @Override
    public List<CreditScoreLog> listMyCreditLogs(Long userId) {
        return this.list(new LambdaQueryWrapper<CreditScoreLog>()
                .eq(CreditScoreLog::getUserId, userId)
                .orderByDesc(CreditScoreLog::getCreateTime)
                .last("LIMIT 50"));
    }
}
