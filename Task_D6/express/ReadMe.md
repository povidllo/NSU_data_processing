```sql
SELECT f.flight_id, f.scheduled_departure, bookings.now()
FROM bookings.flights f
WHERE f.scheduled_departure > bookings.now()
ORDER BY f.scheduled_departure;
```

Узнать какие можно забронировать

```sql

SELECT
  s.seat_no, s.fare_conditions
FROM bookings.flights f
JOIN bookings.routes r ON r.route_no = f.route_no
    AND f.scheduled_departure <@ r.validity
JOIN seats s ON s.airplane_code = r.airplane_code
WHERE f.flight_id = 11051
  AND s.fare_conditions = 'Economy'
  AND f.scheduled_departure > bookings.now()
  AND NOT EXISTS (
      SELECT 1
      FROM boarding_passes bp
      WHERE bp.flight_id = f.flight_id
        AND bp.seat_no = s.seat_no
  )
ORDER BY s.seat_no;
```

Посмотреть какие места свободны (11048, 11051)
