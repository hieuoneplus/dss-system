package com.example.nsga;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ParcelRepository {
    private final JdbcTemplate jdbc;
    public ParcelRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public static class ParcelFeature {
        public int id;
        public String name;
        public String address;
        public double lat, lng;
        public double rent;
        public LocalDate availableFrom;
        public long popSum;
        public int compCnt;
    }

    public List<ParcelFeature> fetchFeatures(int limit, Double radiusMeters, Double Rcomp, Double maxRent, LocalDate availableCutoff, List<Integer> whitelist) {
        double rPop = radiusMeters != null ? radiusMeters : 1000.0;
        double rComp = Rcomp;

        String base = ""
            + "SELECT cs.id, cs.name, cs.address, "
            + "       ST_Y(cs.geom::geometry) AS lat, "
            + "       ST_X(cs.geom::geometry) AS lng, "
            + "       cs.rent_month, "
            + "       cs.available_from, "
            + "       COALESCE((SELECT SUM(p.pop)::bigint FROM population_point p WHERE ST_DWithin(p.geom, cs.geom, ?)),0) AS pop_sum, "
            + "       COALESCE((SELECT COUNT(*)::int FROM competitor c WHERE ST_DWithin(c.geom, cs.geom, ?)),0) AS comp_cnt "
            + "FROM candidate_site cs ";

        StringBuilder sb = new StringBuilder(base);
        List<Object> args = new ArrayList<>();
        args.add(rPop);
        args.add(rComp);

        List<String> where = new ArrayList<>();
        if (maxRent != null) { where.add("cs.rent_month <= ?"); args.add(maxRent); }
        if (availableCutoff != null) { where.add("cs.available_from >= ?"); args.add(availableCutoff); }
        if (whitelist != null && !whitelist.isEmpty()) {
            String in = "(" + String.join(",", whitelist.stream().map(String::valueOf).toList()) + ")";
            where.add("cs.id IN " + in);
        }
        if (!where.isEmpty()) sb.append(" WHERE ").append(String.join(" AND ", where));
        sb.append(" ORDER BY cs.id ASC LIMIT ?"); args.add(limit);

        List<Map<String,Object>> rows = jdbc.queryForList(sb.toString(), args.toArray());
        List<ParcelFeature> out = new ArrayList<>();
        for (Map<String,Object> r : rows) {
            ParcelFeature pf = new ParcelFeature();
            pf.id = ((Number)r.get("id")).intValue();
            pf.name = String.valueOf(r.get("name"));
            pf.address = (String) r.get("address");
            pf.lat = ((Number)r.get("lat")).doubleValue();
            pf.lng = ((Number)r.get("lng")).doubleValue();
            pf.rent = ((Number)r.get("rent_month")).doubleValue();
            Object av = r.get("available_from");
            pf.availableFrom = (av == null) ? null : LocalDate.parse(String.valueOf(av));
            pf.popSum = ((Number)r.get("pop_sum")).longValue();
            pf.compCnt = ((Number)r.get("comp_cnt")).intValue();
            out.add(pf);
        }
        return out;
    }
}
