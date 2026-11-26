package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 交易地点表
 * </p>
 *
 * @author luoyi
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("trade_location")
@ApiModel(value="TradeLocation对象", description="交易地点表")
public class TradeLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "地点名称")
    private String locationName;

    @ApiModelProperty(value = "地点描述")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址")
    private String locationAddress;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）")
    private Integer isAvailable;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    @TableLogic
    private Integer isDelete;


}
