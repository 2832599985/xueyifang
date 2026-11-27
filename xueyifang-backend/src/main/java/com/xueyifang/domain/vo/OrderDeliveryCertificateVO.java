package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@ApiModel(value = "OrderDeliveryCertificateVO", description = "校园技能交付与社会实践数字存证证书（SHA-256防伪签章）")
public class OrderDeliveryCertificateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("数字存证证书编号（CERT-XYF-...）")
    private String certificateNo;

    @ApiModelProperty("关联担保订单ID")
    private Long orderId;

    @ApiModelProperty("担保订单号")
    private String orderNumber;

    @ApiModelProperty("技能服务ID")
    private Long serviceId;

    @ApiModelProperty("技能服务标题")
    private String serviceTitle;

    @ApiModelProperty("技能分类标签")
    private String serviceTagName;

    @ApiModelProperty("所属校区ID")
    private Long campusId;

    @ApiModelProperty("所属校区名称")
    private String campusName;

    @ApiModelProperty("创作者（卖方）用户ID")
    private Long sellerId;

    @ApiModelProperty("创作者姓名/昵称")
    private String sellerName;

    @ApiModelProperty("创作者学号（脱敏展示）")
    private String sellerStudentIdMasked;

    @ApiModelProperty("创作者是否通过教育邮箱核验")
    private Integer sellerEduVerified;

    @ApiModelProperty("委托方（买方）用户ID")
    private Long buyerId;

    @ApiModelProperty("委托方姓名/昵称")
    private String buyerName;

    @ApiModelProperty("委托方学号（脱敏展示）")
    private String buyerStudentIdMasked;

    @ApiModelProperty("担保结算总金额（元）")
    private BigDecimal totalAmount;

    @ApiModelProperty("交付方式（1=线上担保交付, 2=校园线下当面核销）")
    private Integer tradeType;

    @ApiModelProperty("核销存证通道（BLE_CRYPTO / QR_10S / TOTP_30S / ONLINE_ESCROW）")
    private String verifyChannel;

    @ApiModelProperty("线下会面校内地标名称")
    private String meetupPoiName;

    @ApiModelProperty("折算校园技能实践学时（小时）")
    private BigDecimal practiceCreditHours;

    @ApiModelProperty("履约完结存证时间")
    private LocalDateTime completedTime;

    @ApiModelProperty("SHA-256 链上/平台防篡改数字指纹签章")
    private String sha256Seal;

    @ApiModelProperty("验真状态（VERIFIED_AUTHENTIC）")
    private String verificationStatus;
}
