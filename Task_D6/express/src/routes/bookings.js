const pool = require("../db/pool");
const router = require("express").Router();

router.post("/bookings", async (req, res) => {
  const { passenger_id, passenger_name, flight_ids, fare_conditions } =
    req.body;

  if (
    !passenger_id ||
    !passenger_name ||
    !Array.isArray(flight_ids) ||
    flight_ids.length === 0 ||
    !fare_conditions
  ) {
    return res.status(400).json({ error: "Invalid request body" });
  }

  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const flightsOk = await isFlightsExist(client, flight_ids);

    if (!flightsOk) {
      await client.query("ROLLBACK");
      return res.status(400).json({ error: "Flights not found" });
    }

    const bookRef = await generateUniqueBookRef(client);
    const ticketNo = await generateUniqueTicketNo(client);

    const totalAmount = await calculateTotalAmount(
      client,
      flight_ids,
      fare_conditions,
    );

    await insertBooking(client, bookRef, new Date().toISOString(), totalAmount);

    await insertTicket(
      client,
      ticketNo,
      bookRef,
      passenger_id,
      passenger_name,
      true,
    );

    for (const flightId of flight_ids) {
      const price = await calculateFlightPrice(
        client,
        flightId,
        fare_conditions,
      );

      await insertSegment(client, ticketNo, flightId, fare_conditions, price);
    }

    await client.query("COMMIT");

    return res.status(201).json({
      book_ref: bookRef,
      ticket_no: ticketNo,
      total_amount: totalAmount,
    });
  } catch (err) {
    await client.query("ROLLBACK");
    console.error(err);
    return res.status(500).json({ error: "Database error" });
  } finally {
    client.release();
  }
});

const isFlightsExist = async (client, flight_ids) => {
  const result = await client.query(
    `
    SELECT COUNT(*)
    FROM bookings.flights
    WHERE flight_id = ANY($1)
    AND scheduled_departure > bookings.now()
    `,
    [flight_ids],
  );

  return Number(result.rows[0].count) === flight_ids.length;
};

const calculateTotalAmount = async (client, flightIds, fareConditions) => {
  const result = await client.query(
    `
    SELECT COALESCE(SUM(
      (EXTRACT(EPOCH FROM r.duration) / 3600.0) * pr.price_per_hour
    ), 0) AS total

    FROM bookings.flights f
    JOIN bookings.routes r ON r.route_no = f.route_no
    JOIN pricing_rules pr
      ON pr.route_no = r.route_no
     AND pr.fare_conditions = $1

    WHERE f.flight_id = ANY($2)
    `,
    [fareConditions, flightIds],
  );

  return Number(result.rows[0].total || 0).toFixed(2);
};

const calculateFlightPrice = async (client, flightId, fareConditions) => {
  const result = await client.query(
    `
    SELECT
      (EXTRACT(EPOCH FROM r.duration) / 3600.0) * pr.price_per_hour AS price

    FROM bookings.flights f
    JOIN bookings.routes r ON r.route_no = f.route_no
    JOIN pricing_rules pr
      ON pr.route_no = r.route_no
     AND pr.fare_conditions = $2

    WHERE f.flight_id = $1
    `,
    [flightId, fareConditions],
  );

  return Number(result.rows[0]?.price ?? 0);
};

const insertBooking = async (client, bookRef, now, totalAmount) => {
  await client.query(
    `
    INSERT INTO bookings.bookings(
      book_ref,
      book_date,
      total_amount
    )
    VALUES ($1, $2, $3)
    `,
    [bookRef, now, totalAmount],
  );
};

const insertTicket = async (
  client,
  ticketNo,
  bookRef,
  passengerId,
  passengerName,
  outbound,
) => {
  await client.query(
    `
    INSERT INTO bookings.tickets(
      ticket_no,
      book_ref,
      passenger_id,
      passenger_name,
      outbound
    )
    VALUES ($1, $2, $3, $4, $5)
    `,
    [ticketNo, bookRef, passengerId, passengerName, outbound],
  );
};

const insertSegment = async (
  client,
  ticketNo,
  flightId,
  fareConditions,
  price,
) => {
  await client.query(
    `
    INSERT INTO bookings.segments(
      ticket_no,
      flight_id,
      fare_conditions,
      price
    )
    VALUES ($1, $2, $3, $4)
    `,
    [ticketNo, flightId, fareConditions, price],
  );
};

const generateUniqueBookRef = async (client) => {
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

  for (let attempt = 0; attempt < 5; attempt++) {
    let ref = "";

    for (let i = 0; i < 6; i++) {
      ref += chars[Math.floor(Math.random() * chars.length)];
    }

    const exists = await client.query(
      `SELECT 1 FROM bookings.bookings WHERE book_ref = $1 LIMIT 1`,
      [ref],
    );

    if (exists.rowCount === 0) return ref;
  }

  throw new Error("Failed to generate book_ref");
};

const generateUniqueTicketNo = async (client) => {
  for (let attempt = 0; attempt < 5; attempt++) {
    let ticket = "000";

    for (let i = 0; i < 10; i++) {
      ticket += Math.floor(Math.random() * 10);
    }

    const exists = await client.query(
      `SELECT 1 FROM bookings.tickets WHERE ticket_no = $1 LIMIT 1`,
      [ticket],
    );

    if (exists.rowCount === 0) return ticket;
  }

  throw new Error("Failed to generate ticket_no");
};

module.exports = router;
