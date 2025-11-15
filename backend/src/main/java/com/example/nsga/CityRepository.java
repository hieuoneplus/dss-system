package com.example.nsga;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class CityRepository {
    private final JdbcTemplate jdbc;
    public CityRepository(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    public static class City {
        public int id; public String name;
        public double lat, lng; public double radiusM;
    }

    public List<City> findAll() {
        String sql = "SELECT id, name, ST_Y(geom::geometry) AS lat, ST_X(geom::geometry) AS lng, radius_m " +
                "FROM city ORDER BY name ASC";
        return jdbc.query(sql, (rs, i) -> {
            City c = new City();
            c.id = rs.getInt("id");
            c.name = rs.getString("name");
            c.lat = rs.getDouble("lat");
            c.lng = rs.getDouble("lng");
            c.radiusM = rs.getDouble("radius_m");
            return c;
        });
    }
}
