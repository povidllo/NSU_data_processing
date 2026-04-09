SELECT *
FROM flights_history
ORDER BY flight_id,
    fare_conditions
LIMIT 50;

SELECT * FROM pricing_rules 
ORDER BY route_no, fare_conditions
LIMIT 30;