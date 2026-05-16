SELECT r.route_no,
  f.flight_id,
  r.duration,
  f.scheduled_departure,
  s.fare_conditions,
  (
    pr.price_per_hour * (
      EXTRACT(
        EPOCH
        FROM r.duration
      ) / 3600.0
    )
  ) AS restored_price,
  s.price AS actual_price,
  s.ticket_no
FROM routes r
  JOIN flights f ON f.route_no = r.route_no
  AND r.validity @> f.scheduled_departure
  JOIN segments s ON s.flight_id = f.flight_id
  JOIN pricing_rules pr ON pr.route_no = r.route_no
  AND pr.fare_conditions = s.fare_conditions
WHERE f.status IN ('Arrived', 'Departed')
ORDER BY r.route_no, s.fare_conditions, r.duration 
LIMIT 100;