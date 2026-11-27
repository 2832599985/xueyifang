package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("group_session")
public class GroupSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String sessionNo;
    private Long creatorId;
    private Long campusId;
    private Long tagId;
    private String title;
    private String description;
    private BigDecimal basePrice;
    private Integer tierTargetCount;
    private BigDecimal tierDiscountPrice;
    private Integer minParticipants;
    private Integer maxParticipants;
    private Integer joinedCount;
    private Integer tierRebateApplied;
    private Integer tradeType;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;
    private BigDecimal meetupLat;
    private BigDecimal meetupLng;
    private String meetupPoiName;
    private String meetupAddress;
    private Integer meetupRadiusMeters;
    private Integer sessionStatus; // 0=招募中, 1=已成团待开课, 2=已满员锁定, 3=已完结, 4=已取消
    private Long templateServiceId;
    private String closeReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
