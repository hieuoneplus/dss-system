//bỏ qua vì db cloud đang tạo sẵn rồi, ko cần chạy

CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_raster;
SELECT postgis_full_version();

DROP TABLE IF EXISTS candidate_site CASCADE;
DROP TABLE IF EXISTS competitor CASCADE;
DROP TABLE IF EXISTS population_point CASCADE;

CREATE TABLE candidate_site (
                                id SERIAL PRIMARY KEY,
                                name TEXT NOT NULL,
                                address TEXT,
                                rent_month NUMERIC(14,2) NOT NULL,
                                available_from DATE,
                                geom GEOGRAPHY(POINT,4326) NOT NULL
);

CREATE INDEX idx_candidate_geom ON candidate_site USING GIST(geom);

CREATE TABLE competitor (
                            id SERIAL PRIMARY KEY,
                            brand TEXT,
                            geom GEOGRAPHY(POINT,4326) NOT NULL
);
CREATE INDEX idx_competitor_geom ON competitor USING GIST(geom);

CREATE TABLE population_point (
                                  id SERIAL PRIMARY KEY,
                                  pop INTEGER NOT NULL,
                                  geom GEOGRAPHY(POINT,4326) NOT NULL
);
CREATE INDEX idx_pop_geom ON population_point USING GIST(geom);

CREATE TABLE city (
                      id         SERIAL PRIMARY KEY,
                      name       TEXT NOT NULL,
                      lat        DOUBLE PRECISION NOT NULL,
                      lng        DOUBLE PRECISION NOT NULL,
                      geom       geometry(Point, 4326) GENERATED ALWAYS AS (
                          ST_SetSRID(ST_MakePoint(lng, lat), 4326)
                          ) STORED,
                      radius_m   DOUBLE PRECISION NOT NULL         -- bán kính (m), lấy geom làm tâm
);

CREATE INDEX city_geom_gist ON city USING GIST (geom);



-- =========================================================
-- 3) COMPETITOR (GEOGRAPHY) — 70% lõi, 30% rìa; brand có trọng số nhẹ
-- =========================================================

-- HÀ NỘI: 150 đối thủ
WITH c AS (
    SELECT id, name, geom::geography AS g, radius_m FROM city WHERE name='Hà Nội'
), gen AS (
    SELECT gs AS i FROM generate_series(1,150) gs
), polar AS (
    SELECT
        CASE WHEN random() < 0.70
                 THEN random() * (c.radius_m * 0.5)
             ELSE (c.radius_m * 0.5) + random()*(c.radius_m*0.5)
            END AS dist_m,
        (random()*2*pi())::double precision AS bearing,
    c.g AS g
FROM c CROSS JOIN gen
    ), pts AS (
SELECT
    (ARRAY['AEON','LOTTE','GO!','Winmart','Co.opmart'])[1 + (floor(power(random(),1.2)*5))::int] AS brand,
    ST_Project(g, dist_m, bearing) AS geog
FROM polar
    )
INSERT INTO competitor (brand, geom)
SELECT brand, geog FROM pts;

-- TP. HCM: 200 đối thủ
WITH c AS (
    SELECT id, name, geom::geography AS g, radius_m FROM city WHERE name='TP. Hồ Chí Minh'
), gen AS (
    SELECT gs AS i FROM generate_series(1,200) gs
), polar AS (
    SELECT
        CASE WHEN random() < 0.70
                 THEN random() * (c.radius_m * 0.5)
             ELSE (c.radius_m * 0.5) + random()*(c.radius_m*0.5)
            END AS dist_m,
        (random()*2*pi())::double precision AS bearing,
    c.g AS g
FROM c CROSS JOIN gen
    ), pts AS (
SELECT
    (ARRAY['AEON','LOTTE','GO!','Winmart','Co.opmart'])[1 + (floor(power(random(),1.2)*5))::int] AS brand,
    ST_Project(g, dist_m, bearing) AS geog
FROM polar
    )
INSERT INTO competitor (brand, geom)
SELECT brand, geog FROM pts;

-- ĐÀ NẴNG: 100 đối thủ
WITH c AS (
    SELECT id, name, geom::geography AS g, radius_m FROM city WHERE name='Đà Nẵng'
), gen AS (
    SELECT gs AS i FROM generate_series(1,100) gs
), polar AS (
    SELECT
        CASE WHEN random() < 0.70
                 THEN random() * (c.radius_m * 0.5)
             ELSE (c.radius_m * 0.5) + random()*(c.radius_m*0.5)
            END AS dist_m,
        (random()*2*pi())::double precision AS bearing,
    c.g AS g
FROM c CROSS JOIN gen
    ), pts AS (
SELECT
    (ARRAY['AEON','LOTTE','GO!','Winmart','Co.opmart'])[1 + (floor(power(random(),1.2)*5))::int] AS brand,
    ST_Project(g, dist_m, bearing) AS geog
FROM polar
    )
INSERT INTO competitor (brand, geom)
SELECT brand, geog FROM pts;


-----------Candidate_site------------



----------------TP Đà Nẵng----------------
DELETE FROM candidate_site WHERE address = 'Auto seed - TP Đà Nẵng';

WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(108.20623, 16.047079), 4326)::geography AS g
),

-- -------- Band 1: [0, 10km], 200 lô, 70–100 triệu --------
     b1 AS (
         SELECT
             'Auto seed - TP Đà Nẵng'::text AS address,
    -- Giá 70–100 triệu, làm tròn 1 chữ số thập phân trước khi nhân (VNĐ)
                 (ROUND((70 + random()*30)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 0..10,000 m; góc 0..2π
             ST_Project(c.g,
                        (random()*10000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,200) AS gs(i)
     ),

-- -------- Band 2: (10, 15] km, 180 lô, 40–60 triệu --------
     b2 AS (
         SELECT
             'Auto seed - TP Đà Nẵng'::text AS address,
                 (ROUND((40 + random()*20)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 10,000..15,000 m
             ST_Project(c.g,
                        (10000.0 + random()*5000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,180) AS gs(i)
     ),

-- -------- Band 3: (15, 20] km, 120 lô, 7–30 triệu --------
     b3 AS (
         SELECT
             'Auto seed - TP Đà Nẵng'::text AS address,
                 (ROUND((7 + random()*23)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 15,000..20,000 m
             ST_Project(c.g,
                        (15000.0 + random()*5000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,120) AS gs(i)
     ),

     allpts AS (
         SELECT * FROM b1
         UNION ALL
         SELECT * FROM b2
         UNION ALL
         SELECT * FROM b3
     ),
     num AS (
         SELECT
             ROW_NUMBER() OVER () AS rn,
                 address, rent_month, available_from, geog
         FROM allpts
     )
INSERT INTO candidate_site (name, address, rent_month, available_from, geom)
SELECT
        'ĐN Parcel ' || LPAD(rn::text, 3, '0') AS name,
        address,
        rent_month,
        available_from,
        geog
FROM num;



----------------TP HCM----------------
DELETE FROM candidate_site WHERE address = 'Auto seed - TP. Hồ Chí Minh';

WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(106.660172, 10.762622), 4326)::geography AS g
),

-- -------- Band 1: [0, 10km], 200 lô, 70–100 triệu --------
     b1 AS (
         SELECT
             'Auto seed - TP. Hồ Chí Minh'::text AS address,
    -- Giá 70–100 triệu, làm tròn 1 chữ số thập phân trước khi nhân (VNĐ)
                 (ROUND((70 + random()*30)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 0..10,000 m; góc 0..2π
             ST_Project(c.g,
                        (random()*10000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,200) AS gs(i)
     ),

-- -------- Band 2: (10, 15] km, 180 lô, 40–60 triệu --------
     b2 AS (
         SELECT
             'Auto seed - TP. Hồ Chí Minh'::text AS address,
                 (ROUND((40 + random()*20)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 10,000..15,000 m
             ST_Project(c.g,
                        (10000.0 + random()*5000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,180) AS gs(i)
     ),

-- -------- Band 3: (15, 20] km, 120 lô, 7–30 triệu --------
     b3 AS (
         SELECT
             'Auto seed - TP. Hồ Chí Minh'::text AS address,
                 (ROUND((7 + random()*23)::numeric, 1) * 1000000)::numeric(14,2) AS rent_month,
                 (CURRENT_DATE + (random()*60)::int) AS available_from,
             -- Khoảng cách 15,000..20,000 m
             ST_Project(c.g,
                        (15000.0 + random()*5000.0),
                        (random()*2*pi())::double precision) AS geog
         FROM center c
                  CROSS JOIN generate_series(1,120) AS gs(i)
     ),

     allpts AS (
         SELECT * FROM b1
         UNION ALL
         SELECT * FROM b2
         UNION ALL
         SELECT * FROM b3
     ),
     num AS (
         SELECT
             ROW_NUMBER() OVER () AS rn,
                 address, rent_month, available_from, geog
         FROM allpts
     )
INSERT INTO candidate_site (name, address, rent_month, available_from, geom)
SELECT
        'HCM Parcel ' || LPAD(rn::text, 3, '0') AS name,
        address,
        rent_month,
        available_from,
        geog
FROM num;




-----------------Population----------------
----------Hà Nội----------------
WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(105.804817, 21.028511), 4326)::geography AS g
)
DELETE FROM population_point pp
    USING center c
WHERE ST_DWithin(pp.geom, c.g, 20000);

WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(105.804817, 21.028511), 4326)::geography AS g
),

-- -------- Band 1: [0, 10km], pop 400–600, 2000 điểm --------
     b1 AS (
         SELECT
             -- random int trong [400,600]
             (floor(400 + random() * 201))::int AS pop,
                 ST_Project(c.g,
                            (random() * 10000.0),                     -- 0 .. 10,000 m
                            (random() * 2 * pi())::double precision   -- góc 0..2π
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 2: (10, 15] km, pop 300–400, 2000 điểm --------
     b2 AS (
         SELECT
             (floor(300 + random() * 101))::int AS pop,           -- 300 .. 400
                 ST_Project(c.g,
                            (10000.0 + random() * 5000.0),            -- 10,000 .. 15,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 3: (15, 20] km, pop 150–250, 2000 điểm --------
     b3 AS (
         SELECT
             (floor(150 + random() * 101))::int AS pop,           -- 150 .. 250
                 ST_Project(c.g,
                            (15000.0 + random() * 5000.0),            -- 15,000 .. 20,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     )

INSERT INTO population_point (pop, geom)
SELECT pop, geog FROM b1
UNION ALL
SELECT pop, geog FROM b2
UNION ALL
SELECT pop, geog FROM b3;



--------------TP. Hồ Chí Minh----------------
WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(106.660172, 10.762622), 4326)::geography AS g
)
DELETE FROM population_point pp
    USING center c
WHERE ST_DWithin(pp.geom, c.g, 20000);

WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(106.660172, 10.762622), 4326)::geography AS g
),

-- -------- Band 1: [0, 10km], pop 400–600, 2000 điểm --------
     b1 AS (
         SELECT
             -- random int trong [400,600]
             (floor(400 + random() * 201))::int AS pop,
                 ST_Project(c.g,
                            (random() * 10000.0),                     -- 0 .. 10,000 m
                            (random() * 2 * pi())::double precision   -- góc 0..2π
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 2: (10, 15] km, pop 300–400, 2000 điểm --------
     b2 AS (
         SELECT
             (floor(300 + random() * 101))::int AS pop,           -- 300 .. 400
                 ST_Project(c.g,
                            (10000.0 + random() * 5000.0),            -- 10,000 .. 15,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 3: (15, 20] km, pop 150–250, 2000 điểm --------
     b3 AS (
         SELECT
             (floor(150 + random() * 101))::int AS pop,           -- 150 .. 250
                 ST_Project(c.g,
                            (15000.0 + random() * 5000.0),            -- 15,000 .. 20,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     )

INSERT INTO population_point (pop, geom)
SELECT pop, geog FROM b1
UNION ALL
SELECT pop, geog FROM b2
UNION ALL
SELECT pop, geog FROM b3;


--------------TP Đà Nẵng----------------
WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(108.20623, 16.047079), 4326)::geography AS g
)
DELETE FROM population_point pp
    USING center c
WHERE ST_DWithin(pp.geom, c.g, 12000);

WITH center AS (
    SELECT ST_SetSRID(ST_MakePoint(108.20623, 16.047079), 4326)::geography AS g
),

-- -------- Band 1: [0, 10km], pop 400–600, 2000 điểm --------
     b1 AS (
         SELECT
             -- random int trong [400,600]
             (floor(400 + random() * 201))::int AS pop,
                 ST_Project(c.g,
                            (random() * 10000.0),                     -- 0 .. 10,000 m
                            (random() * 2 * pi())::double precision   -- góc 0..2π
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 2: (10, 15] km, pop 300–400, 2000 điểm --------
     b2 AS (
         SELECT
             (floor(300 + random() * 101))::int AS pop,           -- 300 .. 400
                 ST_Project(c.g,
                            (10000.0 + random() * 5000.0),            -- 10,000 .. 15,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     ),

-- -------- Band 3: (15, 20] km, pop 150–250, 2000 điểm --------
     b3 AS (
         SELECT
             (floor(150 + random() * 101))::int AS pop,           -- 150 .. 250
                 ST_Project(c.g,
                            (15000.0 + random() * 5000.0),            -- 15,000 .. 20,000 m
                            (random() * 2 * pi())::double precision
                     ) AS geog
         FROM center c
                  CROSS JOIN generate_series(1, 150)
     )

INSERT INTO population_point (pop, geom)
SELECT pop, geog FROM b1
UNION ALL
SELECT pop, geog FROM b2
UNION ALL
SELECT pop, geog FROM b3;






commit;