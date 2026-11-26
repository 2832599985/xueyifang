package com.xueyifang.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 校园电子围栏与空间几何计算引擎（纯 Java 8 无外部 GIS 依赖）
 * <p>
 * 1. Haversine 球面距离计算（精度米级，用于会面打卡距离与围栏兜底半径判定）
 * 2. Ray-Casting 奇偶射线法多边形点包含判定（支持不规则校园边界电子围栏）
 */
public final class GeofenceUtils {

    private static final double EARTH_RADIUS_METERS = 6371008.8;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GeofenceUtils() {
    }

    /**
     * 校验经纬度坐标合法性（中国大陆及常见全球合法范围）
     */
    public static boolean isValidCoordinate(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return false;
        }
        if (Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lng) || Double.isInfinite(lng)) {
            return false;
        }
        return lat >= -90.0 && lat <= 90.0 && lng >= -180.0 && lng <= 180.0;
    }

    /**
     * 计算两点之间的球面距离（单位：米）
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = radLat2 - radLat1;
        double deltaLng = Math.toRadians(lng2 - lng1);

        double sinLat = Math.sin(deltaLat / 2.0);
        double sinLng = Math.sin(deltaLng / 2.0);
        double a = sinLat * sinLat
                + Math.cos(radLat1) * Math.cos(radLat2) * sinLng * sinLng;
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * 判断目标坐标是否位于多边形围栏内（Ray-Casting 射线法）
     *
     * @param lat     目标纬度
     * @param lng     目标经度
     * @param polygon 多边形顶点列表，每个元素为 [lng, lat]
     */
    public static boolean isPointInPolygon(double lat, double lng, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0];
            double yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0];
            double yj = polygon.get(j)[1];

            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / ((yj - yi) + 1e-12) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 解析校区 JSON 多边形围栏并判定坐标是否在合法校园范围内；
     * 若未配置多边形，则回退使用校区中心点 + 安全半径（米）判定。
     */
    public static boolean isInsideCampusFence(double lat, double lng,
                                              String polygonJson,
                                              Double centerLat, Double centerLng,
                                              Integer fallbackRadiusMeters) {
        List<double[]> polygon = parsePolygonJson(polygonJson);
        if (polygon.size() >= 3) {
            return isPointInPolygon(lat, lng, polygon);
        }
        if (centerLat != null && centerLng != null) {
            int radius = (fallbackRadiusMeters != null && fallbackRadiusMeters > 0) ? fallbackRadiusMeters : 2000;
            return distanceMeters(lat, lng, centerLat, centerLng) <= radius;
        }
        return true;
    }

    /**
     * 解析形如 [[114.35,30.52],[114.37,30.52],...] 的多边形 JSON
     */
    public static List<double[]> parsePolygonJson(String polygonJson) {
        if (StringUtils.isBlank(polygonJson)) {
            return Collections.emptyList();
        }
        try {
            List<double[]> points = OBJECT_MAPPER.readValue(polygonJson, new TypeReference<List<double[]>>() {
            });
            if (points == null) {
                return Collections.emptyList();
            }
            for (double[] pt : points) {
                if (pt == null || pt.length < 2) {
                    return Collections.emptyList();
                }
            }
            return points;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
