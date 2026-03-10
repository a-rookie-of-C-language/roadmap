package com.roadmap.util;

/**
 * 坐标转换工具类
 * <p>
 * 支持 WGS-84、GCJ-02、BD-09(ll/mc) 之间的互转。
 * <ul>
 *   <li>gcj02ToWgs84: 使用迭代逼近算法，精度 < 0.5m（原一次近似误差可达几十米）</li>
 *   <li>bd09mcToBd09ll: 百度墨卡托米制坐标 → 百度经纬度</li>
 *   <li>toWgs84: 统一入口，根据 coordType 自动选择转换链路</li>
 * </ul>
 */
public final class CoordTransformUtil {

    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;
    private static final double X_PI = Math.PI * 3000.0 / 180.0;

    /** 迭代逼近精度阈值（度），约对应 0.01m */
    private static final double ITERATION_THRESHOLD = 1e-9;
    /** 最大迭代次数 */
    private static final int MAX_ITERATIONS = 15;

    // ===== 百度墨卡托常量 =====
    private static final double[] MC_BAND = {12890594.86, 8362377.87, 5591021.0, 3481989.83, 1678043.12, 0};
    private static final double[][] MC2LL = {
            {1.410526172116255e-8, 8.98305509648872e-6, -1.9939833816331, 200.9824383106796,
                    -187.2403703815547, 91.6087516669843, -23.38765649603339, 2.57121317296198,
                    -0.03801003308653, 17337981.2},
            {-7.435856389565537e-9, 8.983055097726239e-6, -0.78625201886289, 96.32687599759846,
                    -1.85204757529826, -59.36935905485877, 47.40033549296737, -16.50741931063887,
                    2.28786674699375, 10260144.86},
            {-3.030883460898826e-8, 8.98305509983578e-6, 0.30071316287616, 59.74293618442277,
                    7.357984074871, -25.38371002664745, 13.45380521110908, -3.29883767235584,
                    0.32710905363475, 6856817.37},
            {-1.981981304930552e-8, 8.983055099779535e-6, 0.03278182852591, 40.31678527705744,
                    0.65659298677277, -4.44255534477492, 0.85341911805263, 0.12923347998204,
                    -0.04625736007561, 4482777.06},
            {3.09191371068437e-9, 8.983055096812155e-6, 6.995724062e-5, 23.10934304144901,
                    -0.00023663490511, -0.6321817810242, -0.00663494467042, 0.03430082397953,
                    -0.00466043876332, 2555164.4},
            {2.890871144776878e-9, 8.983055095805407e-6, -3.068298e-8, 7.47137025468032,
                    -3.53937994e-6, -0.02145144861037, -1.234426596e-5, 1.0322952773e-4,
                    -3.23890364e-6, 826088.5}
    };

    private CoordTransformUtil() {
    }

    // ========================== 公开转换方法 ==========================

    /**
     * 统一坐标转 WGS-84 入口
     *
     * @param lng       经度（或墨卡托 X）
     * @param lat       纬度（或墨卡托 Y）
     * @param coordType 坐标类型：wgs84, gcj02, bd09(=bd09ll), bd09mc
     * @return WGS-84 [lng, lat]
     */
    public static double[] toWgs84(double lng, double lat, String coordType) {
        String normalized = coordType == null ? "gcj02" : coordType.trim().toLowerCase();
        return switch (normalized) {
            case "wgs84" -> new double[]{lng, lat};
            case "bd09", "bd09ll" -> bd09ToWgs84(lng, lat);
            case "bd09mc" -> {
                double[] ll = bd09mcToBd09ll(lng, lat);
                yield bd09ToWgs84(ll[0], ll[1]);
            }
            case "gcj02" -> gcj02ToWgs84(lng, lat);
            default -> gcj02ToWgs84(lng, lat);
        };
    }

    /**
     * GCJ-02 → WGS-84（迭代逼近算法）
     * <p>
     * 原实现使用一次近似 wgs ≈ 2*gcj - transform(gcj)，误差可达几十米。
     * 改为迭代法：从 gcj02 点出发，反复用 wgs84ToGcj02 校正，收敛精度 < 0.5m。
     */
    public static double[] gcj02ToWgs84(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        // 初始猜测：直接用 GCJ-02 坐标作为 WGS-84 的近似
        double wgsLng = lng;
        double wgsLat = lat;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double[] gcj = wgs84ToGcj02(wgsLng, wgsLat);
            double dLng = lng - gcj[0];
            double dLat = lat - gcj[1];
            wgsLng += dLng;
            wgsLat += dLat;
            if (Math.abs(dLng) < ITERATION_THRESHOLD && Math.abs(dLat) < ITERATION_THRESHOLD) {
                break;
            }
        }
        return new double[]{wgsLng, wgsLat};
    }

    /**
     * WGS-84 → GCJ-02
     */
    public static double[] wgs84ToGcj02(double lng, double lat) {
        if (outOfChina(lng, lat)) {
            return new double[]{lng, lat};
        }
        return transform(lng, lat);
    }

    /**
     * BD-09(ll) → WGS-84
     */
    public static double[] bd09ToWgs84(double lng, double lat) {
        double[] gcj = bd09ToGcj02(lng, lat);
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }

    /**
     * BD-09(ll) → GCJ-02
     */
    public static double[] bd09ToGcj02(double lng, double lat) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        double ggLng = z * Math.cos(theta);
        double ggLat = z * Math.sin(theta);
        return new double[]{ggLng, ggLat};
    }

    /**
     * GCJ-02 → BD-09(ll)
     */
    public static double[] gcj02ToBd09(double lng, double lat) {
        double z = Math.sqrt(lng * lng + lat * lat) + 0.00002 * Math.sin(lat * X_PI);
        double theta = Math.atan2(lat, lng) + 0.000003 * Math.cos(lng * X_PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLng, bdLat};
    }

    /**
     * BD-09 墨卡托（米制坐标）→ BD-09 经纬度
     * <p>
     * bd09mc 的 X/Y 值通常在百万到千万级，如 (12947725, 4846471)。
     * 如果传入的值在经纬度范围内，直接原样返回（已经是 bd09ll）。
     */
    public static double[] bd09mcToBd09ll(double x, double y) {
        // 快速判断：如果值在经纬度范围内，可能已经是 bd09ll
        if (Math.abs(x) <= 180 && Math.abs(y) <= 90) {
            return new double[]{x, y};
        }
        double absY = Math.abs(y);
        double[] factors = null;
        for (int i = 0; i < MC_BAND.length; i++) {
            if (absY >= MC_BAND[i]) {
                factors = MC2LL[i];
                break;
            }
        }
        if (factors == null) {
            // 兜底：使用最后一组系数
            factors = MC2LL[MC2LL.length - 1];
        }
        return convertor(x, y, factors);
    }

    // ========================== 输入校验 ==========================

    /**
     * 判断坐标值是否像墨卡托坐标（而非经纬度）
     * <p>
     * 如果 |lng| > 180 或 |lat| > 90，说明不是经纬度，很可能是墨卡托米制坐标。
     *
     * @return true 表示疑似墨卡托坐标
     */
    public static boolean looksLikeMercator(double lng, double lat) {
        return Math.abs(lng) > 180 || Math.abs(lat) > 90;
    }

    // ========================== 内部方法 ==========================

    private static double[] transform(double lng, double lat) {
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * Math.PI);
        double mgLat = lat + dLat;
        double mgLng = lng + dLng;
        return new double[]{mgLng, mgLat};
    }

    private static double transformLat(double lng, double lat) {
        double ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat
                + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin(lat / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(lat / 12.0 * Math.PI) + 320 * Math.sin(lat * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double lng, double lat) {
        double ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng
                + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
        ret += (20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin(lng / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(lng / 12.0 * Math.PI) + 300.0 * Math.sin(lng / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 中国境内判断（GCJ-02 加密仅适用于中国大陆区域）
     * <p>
     * 南纬界修正为 3.86（覆盖海南三沙群岛最南端），原值 0.8293 遗漏了部分南海区域。
     */
    private static boolean outOfChina(double lng, double lat) {
        return lng < 72.004 || lng > 137.8347 || lat < 3.86 || lat > 55.8271;
    }

    /**
     * 百度墨卡托转换内部函数
     */
    private static double[] convertor(double x, double y, double[] factors) {
        double t = factors[9];
        double lng = factors[0] + factors[1] * Math.abs(x);
        double absY = Math.abs(y) / t;
        double lat = factors[2]
                + factors[3] * absY
                + factors[4] * absY * absY
                + factors[5] * absY * absY * absY
                + factors[6] * Math.pow(absY, 4)
                + factors[7] * Math.pow(absY, 5)
                + factors[8] * Math.pow(absY, 6);
        lng *= (x < 0 ? -1 : 1);
        lat *= (y < 0 ? -1 : 1);
        return new double[]{lng, lat};
    }
}
