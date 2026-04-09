DROP TABLE IF EXISTS pricing_rules CASCADE;

CREATE TABLE pricing_rules (
    rule_id            SERIAL PRIMARY KEY,
    route_no           TEXT           NOT NULL,
    fare_conditions    TEXT           NOT NULL,
    determined_price         NUMERIC(10,2)  NOT NULL,
    
    UNIQUE (route_no, fare_conditions)
);

INSERT INTO pricing_rules 
    (route_no, fare_conditions, determined_price)
SELECT 
    r.route_no,
    s.fare_conditions,
    ROUND(AVG(s.price), 2)                  AS determined_price
FROM segments s
JOIN flights f 
    ON f.flight_id = s.flight_id
JOIN routes r 
    ON r.route_no = f.route_no 
   AND r.validity @> f.scheduled_departure
WHERE f.scheduled_departure < bookings.now()
GROUP BY r.route_no, s.fare_conditions;

SELECT * FROM pricing_rules 
ORDER BY route_no, fare_conditions
LIMIT 30;