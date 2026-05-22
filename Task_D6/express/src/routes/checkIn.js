const pool = require("../db/pool");
const router = require("express").Router();

router.post("/check-in", async (req, res) => {
  const { ticket_no, seats } = req.body;

  if (!ticket_no || !seats || !Array.isArray(seats) || seats.length === 0) {
    return res.status(400).json({
      error: "Invalid request body",
    });
  }

  const client = await pool.connect();

  try {
    await client.query("BEGIN");

    const existing = await client.query(
      `
      SELECT
        bp.ticket_no,
        bp.flight_id,
        bp.seat_no,
        bp.boarding_no,
        bp.boarding_time
      FROM boarding_passes bp
      WHERE bp.ticket_no = $1
      ORDER BY bp.flight_id
      `,
      [ticket_no],
    );

    if (existing.rowCount > 0) {
      await client.query("COMMIT");

      return res.status(200).json({
        boarding_passes: existing.rows,
      });
    }

    const ticketSegments = await client.query(
      `
      SELECT
        s.flight_id,
        s.fare_conditions
      FROM segments s
      WHERE s.ticket_no = $1
      ORDER BY s.flight_id
      `,
      [ticket_no],
    );

    if (ticketSegments.rowCount === 0) {
      await client.query("ROLLBACK");

      return res.status(404).json({
        error: "Ticket has no flight segments",
      });
    }

    if (ticketSegments.rowCount !== seats.length) {
      await client.query("ROLLBACK");

      return res.status(400).json({
        error: `Expected ${ticketSegments.rowCount} segments`,
      });
    }

    const createdBoardingPasses = [];

    for (const ticketSegment of ticketSegments.rows) {
      const { flight_id, fare_conditions } = ticketSegment;

      const requestedSeats = seats.find((s) => s.flight_id === flight_id);

      if (!requestedSeats) {
        await client.query("ROLLBACK");

        return res.status(400).json({
          error: `Missing seat for flight ${flight_id}`,
        });
      }

      const { seat_no } = requestedSeats;

      const flightResult = await client.query(
        `
        SELECT
          f.scheduled_departure,
          r.airplane_code
        FROM flights f
        JOIN routes r
          ON r.route_no = f.route_no
        WHERE f.flight_id = $1
          AND f.scheduled_departure <@ r.validity
        LIMIT 1
        `,
        [flight_id],
      );

      if (flightResult.rowCount === 0) {
        await client.query("ROLLBACK");

        return res.status(404).json({
          error: `Flight ${flight_id} not found`,
        });
      }

      const { scheduled_departure, airplane_code } = flightResult.rows[0];

      await logAvialibleSeats(airplane_code, fare_conditions, flight_id);

      const seatExists = await client.query(
        `
        SELECT 1
        FROM seats s
        WHERE s.airplane_code = $1
          AND s.seat_no = $2
          AND s.fare_conditions = $3
        `,
        [airplane_code, seat_no, fare_conditions],
      );
      console.log(airplane_code, seat_no, fare_conditions, seatExists.rows);

      if (seatExists.rowCount === 0) {
        await client.query("ROLLBACK");

        return res.status(400).json({
          error: `Seat ${seat_no} does not exist or has invalid fare class`,
        });
      }

      const occupiedSeat = await client.query(
        `
        SELECT 1
        FROM boarding_passes bp
        WHERE bp.flight_id = $1
          AND bp.seat_no = $2
        `,
        [flight_id, seat_no],
      );

      if (occupiedSeat.rowCount > 0) {
        await client.query("ROLLBACK");

        return res.status(400).json({
          error: `Seat ${seat_no} already occupied on flight ${flight_id}`,
        });
      }

      const boardingNoResult = await client.query(
        `
        SELECT COALESCE(MAX(bp.boarding_no), 0) + 1 AS next_no
        FROM boarding_passes bp
        WHERE bp.flight_id = $1
        `,
        [flight_id],
      );

      const boarding_no = boardingNoResult.rows[0].next_no;

      const boarding_time = new Date(
        new Date(scheduled_departure).getTime() - 30 * 60 * 1000,
      );

      const inserted = await client.query(
        `
        INSERT INTO boarding_passes (
          ticket_no,
          flight_id,
          boarding_no,
          seat_no,
          boarding_time
        )
        VALUES ($1, $2, $3, $4, $5)
        RETURNING *
        `,
        [ticket_no, flight_id, boarding_no, seat_no, boarding_time],
      );

      createdBoardingPasses.push(inserted.rows[0]);
    }

    await client.query("COMMIT");

    return res.status(201).json({
      boarding_passes: createdBoardingPasses,
    });
  } catch (error) {
    await client.query("ROLLBACK");

    console.error(error);

    return res.status(500).json({
      error: "Database error",
      details: error.message,
    });
  } finally {
    client.release();
  }
});

const logAvialibleSeats = async (airplane_code, fare_condition, flight_id) => {
  // const response = await pool.query(
  //   `
  //   SELECT st.seat_no
  //   FROM seats st
  //   WHERE st.airplane_code = $1
  //     AND st.fare_conditions = $2
  //     AND NOT EXISTS (
  //         SELECT 1
  //         FROM boarding_passes bp
  //         WHERE bp.flight_id = $3
  //           AND bp.seat_no = st.seat_no
  //     )
  //   ORDER BY st.seat_no
  //   `,
  //   [airplane_code, fare_condition, flight_id],
  // );
  // console.log(response.rows);
};

module.exports = router;
