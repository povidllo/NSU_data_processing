SELECT bookings.now();

DROP TABLE IF EXISTS flights_history CASCADE;

CREATE TABLE flights_history (
    flight_id INTEGER NOT NULL,
    fare_conditions TEXT NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    ticket_count INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (flight_id, fare_conditions)
);

INSERT INTO flights_history (flight_id, fare_conditions, price, ticket_count)
SELECT s.flight_id,
    s.fare_conditions,
    ROUND(AVG(s.price), 2) AS price,
    COUNT(*) AS ticket_count
FROM segments s
    JOIN flights f ON f.flight_id = s.flight_id
WHERE f.scheduled_departure < bookings.now()
    OR f.status IN ('Departed', 'Arrived', 'Cancelled')
GROUP BY s.flight_id,
    s.fare_conditions;

SELECT *
FROM flights_history
ORDER BY flight_id,
    fare_conditions
LIMIT 50;
