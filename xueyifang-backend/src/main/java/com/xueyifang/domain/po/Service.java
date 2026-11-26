package com.xueyifang.domain.po;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 服务表
 * </p>
 *
 * @author luoyi
 * @since 2025-11-17
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service")
@ApiModel(value="Service对象", description="服务表")
public class Service implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "服务标题")
    private String serviceTitle;

    @ApiModelProperty(value = "服务详情描述")
    private String serviceDescription;

    @ApiModelProperty(value = "发布者ID")
    private Long sellerId;

    @ApiModelProperty(value = "标签ID")
    private Long tagId;

    @ApiModelProperty(value = "交易类型（1=线下交易, 2=线上担保交易）")
    private Integer tradeType;

    @ApiModelProperty(value = "交易地点ID（仅线下交易）")
    private Long tradeLocationId;

    @ApiModelProperty(value = "服务价格")
    private BigDecimal price;

    @ApiModelProperty(value = "最多购买次数（-1=无限制）")
    private Integer maxPurchases;

    @ApiModelProperty(value = "当前购买次数")
    private Integer currentPurchaseCount;

    @ApiModelProperty(value = "是否实体物品（1=是, 0=否）")
    private Integer isPhysical;

    @ApiModelProperty(value = "服务状态（1=上架, 2=下架, 3=待审核, 4=审核驳回）")
    private Integer status;

    @ApiModelProperty(value = "审核状态（1=通过, 2=待审核, 3=驳回）")
    private Integer reviewStatus;

    @ApiModelProperty(value = "审核驳回原因")
    private String reviewReason;

    @ApiModelProperty(value = "审核人ID（管理员）")
    private Long reviewedBy;

    @ApiModelProperty(value = "审核时间")
    private LocalDateTime reviewTime;

    @ApiModelProperty(value = "浏览次数")
    private Integer viewCount;

    @ApiModelProperty(value = "收藏数")
    private Integer collectionCount;

    @ApiModelProperty(value = "平均评分")
    private BigDecimal averageRating;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


}
