package com.xueyifang.domain.po;

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
 * 服务纠纷表
 * </p>
 *
 * @author luoyi
 * @since 2025-11-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("service_dispute")
@ApiModel(value="ServiceDispute对象", description="服务纠纷表")
public class ServiceDispute implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "主键")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "订单ID")
    private Long orderId;

    @ApiModelProperty(value = "纠纷发起者ID")
    private Long disputeInitiatorId;

    @ApiModelProperty(value = "纠纷类型（1=卖家未履行, 2=货不对板, 3=质量问题, 4=其他）")
    private Integer disputeType;

    @ApiModelProperty(value = "纠纷描述")
    private String description;

    @ApiModelProperty(value = "纠纷举证图片（逗号分隔相对路径）")
    private String evidenceImages;

    @ApiModelProperty(value = "纠纷状态（1=待审核, 2=处理中, 3=已解决, 4=已驳回）")
    private Integer disputeStatus;

    @ApiModelProperty(value = "管理员回复")
    private String adminReply;

    @ApiModelProperty(value = "解决方案")
    private String resolution;

    @ApiModelProperty(value = "处理人ID（管理员）")
    private Long handledBy;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "逻辑删除")
    private Integer isDelete;


}
