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

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("trade_location")
@ApiModel(value = "TradeLocation对象", description = "校内线下交易地点表（含真实坐标与可配置有效核销半径）")
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

    @ApiModelProperty(value = "真实地图纬度（GCJ-02）")
    private BigDecimal latitude;

    @ApiModelProperty(value = "真实地图经度（GCJ-02）")
    private BigDecimal longitude;

    @ApiModelProperty(value = "允许当面核销的有效半径（米，默认80米，可自由设置10~2000米）")
    private Integer validRadiusMeters;

    @ApiModelProperty(value = "是否有校园监控覆盖（1=是, 0=否）")
    private Integer hasCctv;

    @ApiModelProperty(value = "是否可用（1=可用, 0=不可用）")
    private Integer isAvailable;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
