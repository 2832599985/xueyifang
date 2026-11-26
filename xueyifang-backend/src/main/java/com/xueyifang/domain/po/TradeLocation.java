package com.xueyifang.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 校内推荐安全交接地点表（v3.0 含经纬度与监控区标识）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("trade_location")
@ApiModel(value = "TradeLocation对象", description = "交易地点表")
public class TradeLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属校区ID")
    private Long campusId;

    @ApiModelProperty(value = "地点名称")
    private String locationName;

    @ApiModelProperty(value = "地点描述")
    private String locationDescription;

    @ApiModelProperty(value = "详细地址")
    private String locationAddress;

    @ApiModelProperty(value = "纬度（GCJ-02）")
    private BigDecimal latitude;

    @ApiModelProperty(value = "经度（GCJ-02）")
    private BigDecimal longitude;

    @ApiModelProperty(value = "是否有校园监控覆盖（1=是, 0=否）")
    private Integer hasCctv;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）")
    private Integer isAvailable;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
