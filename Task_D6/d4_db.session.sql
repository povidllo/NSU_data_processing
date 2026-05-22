DROP TABLE IF EXISTS pricing_rules CASCADE;
CREATE TABLE pricing_rules (
  route_no text NOT NULL,
  fare_conditions text NOT NULL,
  price_per_hour double precision NOT NULL,
  PRIMARY KEY (route_no, fare_conditions)
);
INSERT INTO pricing_rules (
    route_no,
    fare_conditions,
    price_per_hour
  )
SELECT r.route_no,
  s.fare_conditions,
  percentile_cont(0.5) WITHIN GROUP (
    ORDER BY s.price / (
        EXTRACT(
          EPOCH
          FROM r.duration
        ) / 3600.0
      )
  ) AS price_per_hour
FROM routes r
  JOIN flights f ON f.route_no = r.route_no
  AND r.validity @> f.scheduled_departure
  JOIN segments s ON s.flight_id = f.flight_id
WHERE f.status = ANY (ARRAY ['Arrived', 'Departed'])
GROUP BY r.route_no,
  s.fare_conditions;
SELECT *
FROM pricing_rules
LIMIT 10;