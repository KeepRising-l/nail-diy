package com.diy.nail.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.diy.nail.entity.Material;
import com.diy.nail.mapper.AnalyticsMapper;
import com.diy.nail.mapper.MaterialMapper;
import com.diy.nail.vo.AnalyticsOverviewVO;
import com.diy.nail.vo.DesignPrefsVO;
import com.diy.nail.vo.NameCountVO;
import com.diy.nail.vo.TrendPointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 统计看板业务（change-09）：SQL 聚合 + 设计参数快照内存统计（数据量小直查 DB，见能力规格备注）
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    /** 榜单长度 */
    private static final int TOP_N = 5;

    private final AnalyticsMapper analyticsMapper;
    private final MaterialMapper materialMapper;

    /** 总览卡片 + 履约占比 + 评价分布（ANA-REQ-1/4/5） */
    public AnalyticsOverviewVO overview() {
        AnalyticsOverviewVO vo = new AnalyticsOverviewVO();
        vo.setTotalOrders(analyticsMapper.countValidOrders());
        vo.setTotalSales(analyticsMapper.sumSales());
        vo.setPendingFulfillment(analyticsMapper.countPendingFulfillment());
        vo.setAvgRating(analyticsMapper.avgRating().setScale(1, RoundingMode.HALF_UP));

        long selfPickup = 0;
        long express = 0;
        for (Map<String, Object> row : analyticsMapper.deliveryDist()) {
            String type = String.valueOf(row.get("k"));
            long count = ((Number) row.get("c")).longValue();
            if ("SELF_PICKUP".equals(type)) {
                selfPickup = count;
            } else if ("EXPRESS".equals(type)) {
                express = count;
            }
        }
        vo.setSelfPickupCount(selfPickup);
        vo.setExpressCount(express);

        Map<Integer, Long> dist = new HashMap<>();
        for (Map<String, Object> row : analyticsMapper.ratingDist()) {
            dist.put(((Number) row.get("k")).intValue(), ((Number) row.get("c")).longValue());
        }
        List<NameCountVO> ratingDist = new ArrayList<>();
        for (int star = 1; star <= 5; star++) {
            ratingDist.add(new NameCountVO(String.valueOf(star), dist.getOrDefault(star, 0L)));
        }
        vo.setRatingDist(ratingDist);
        return vo;
    }

    /** 销售趋势（ANA-REQ-2）：days=7/30，0=全部 */
    public List<TrendPointVO> trend(int days) {
        List<TrendPointVO> points = new ArrayList<>();
        for (Map<String, Object> row : analyticsMapper.trendByDay(Math.max(days, 0))) {
            TrendPointVO point = new TrendPointVO();
            point.setDate(String.valueOf(row.get("d")));
            point.setAmount(new BigDecimal(String.valueOf(row.get("s"))));
            point.setOrderCount(((Number) row.get("c")).longValue());
            points.add(point);
        }
        return points;
    }

    /** 设计偏好 TOP5（ANA-REQ-3）：解析订单明细设计参数快照，按购买数量加权 */
    public DesignPrefsVO designPrefs() {
        Map<Long, Long> shapeCount = new HashMap<>();
        Map<Long, Long> colorCount = new HashMap<>();
        Map<Long, Long> patternCount = new HashMap<>();
        Set<Long> allIds = new TreeSet<>();

        for (Map<String, Object> row : analyticsMapper.validOrderItemParams()) {
            String params = row.get("p") == null ? "" : String.valueOf(row.get("p"));
            long qty = row.get("q") == null ? 1L : ((Number) row.get("q")).longValue();
            if (StrUtil.isBlank(params) || !JSONUtil.isTypeJSONObject(params)) {
                continue;
            }
            JSONObject json = JSONUtil.parseObj(params);
            // 五指结构（change-05 起设计器实际保存格式）：逐指累加
            JSONArray fingers = json.getJSONArray("fingers");
            if (fingers != null && !fingers.isEmpty()) {
                for (Object item : fingers) {
                    if (!(item instanceof JSONObject)) {
                        continue;
                    }
                    JSONObject finger = (JSONObject) item;
                    accumulate(finger.getLong("shapeId"), shapeCount, allIds, qty);
                    accumulate(finger.getLong("colorId"), colorCount, allIds, qty);
                    List<Long> fingerPatterns = finger.getBeanList("patternIds", Long.class);
                    if (fingerPatterns != null) {
                        for (Long pid : fingerPatterns) {
                            accumulate(pid, patternCount, allIds, qty);
                        }
                    }
                }
                continue;
            }
            // 旧单甲结构（模板/历史数据）
            Long shapeId = json.getLong("shapeId");
            Long colorId = json.getLong("colorId");
            List<Long> patternIds = json.getBeanList("patternIds", Long.class);
            accumulate(shapeId, shapeCount, allIds, qty);
            accumulate(colorId, colorCount, allIds, qty);
            if (patternIds != null) {
                for (Long pid : patternIds) {
                    accumulate(pid, patternCount, allIds, qty);
                }
            }
        }

        Map<Long, String> names = allIds.isEmpty() ? Map.of()
                : materialMapper.selectList(new LambdaQueryWrapper<Material>()
                        .in(Material::getId, allIds))
                .stream().collect(Collectors.toMap(Material::getId, Material::getName, (a, b) -> a));

        DesignPrefsVO vo = new DesignPrefsVO();
        vo.setTopShapes(topNames(shapeCount, names));
        vo.setTopColors(topNames(colorCount, names));
        vo.setTopPatterns(topNames(patternCount, names));
        return vo;
    }

    /** 素材 id 计数累加（id 为空跳过） */
    private void accumulate(Long id, Map<Long, Long> counter, Set<Long> allIds, long qty) {
        if (id == null) {
            return;
        }
        counter.merge(id, qty, Long::sum);
        allIds.add(id);
    }

    /** 计数 Map 转"名称+数量"榜单：按数量倒序（同数按 id 升序）取 TOP5，素材已删除的条目跳过 */
    private List<NameCountVO> topNames(Map<Long, Long> counts, Map<Long, String> names) {
        List<Map.Entry<Long, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(Comparator.<Map.Entry<Long, Long>, Long>comparing(Map.Entry::getValue, Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey));
        List<NameCountVO> list = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : entries) {
            String name = names.get(entry.getKey());
            if (name != null && list.size() < TOP_N) {
                list.add(new NameCountVO(name, entry.getValue()));
            }
        }
        return list;
    }
}
