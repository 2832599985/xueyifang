package com.xueyifang.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Accessors(chain = true)
@ApiModel(value = "CreatorLeaderboardItemVO", description = "校友创作者技能风云榜与多维能力等级勋章视图")
public class CreatorLeaderboardItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("榜单排名名次（1, 2, 3...）")
    private Integer rank;

    @ApiModelProperty("创作者用户ID")
    private Long userId;

    @ApiModelProperty("展示昵称/姓名")
    private String displayName;

    @ApiModelProperty("头像URL")
    private String avatar;

    @ApiModelProperty("年级")
    private String grade;

    @ApiModelProperty("专业名称")
    private String professionalName;

    @ApiModelProperty("所属校区ID")
    private Long campusId;

    @ApiModelProperty("所属校区名称")
    private String campusName;

    @ApiModelProperty("个人技能简介")
    private String bio;

    @ApiModelProperty("是否完成教育邮箱核验（1=已核验）")
    private Integer eduVerified;

    @ApiModelProperty("校园履约信用分")
    private Integer creditScore;

    @ApiModelProperty("创作者成长等级称号（Lv.1 新锐学徒 ~ Lv.5 殿堂首席导师）")
    private String creatorLevelTitle;

    @ApiModelProperty("在池履约保证金余额（元）")
    private BigDecimal sellerDeposit;

    @ApiModelProperty("在架活跃技能服务数")
    private Integer activeServiceCount;

    @ApiModelProperty("累计完结担保订单数")
    private Integer completedOrderCount;

    @ApiModelProperty("累计折算技能实践学时（小时）")
    private BigDecimal totalPracticeHours;

    @ApiModelProperty("综合评价均分")
    private BigDecimal averageRating;

    @ApiModelProperty("累计评价条数")
    private Integer reviewCount;

    @ApiModelProperty("综合影响力指数得分")
    private BigDecimal compositeInfluenceScore;

    @ApiModelProperty("代表技能标签列表")
    private List<String> topSkillTags;

    @ApiModelProperty("已点亮的多维能力荣誉勋章列表")
    private List<CreatorBadgeVO> badges;

    @Data
    @Accessors(chain = true)
    public static class CreatorBadgeVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String badgeCode;
        private String badgeName;
        private String badgeTone; // gold, emerald, blue, purple
        private String badgeDesc;
    }
}
