const pool = require("../db/pool");
const router = require("express").Router();

const Days = {
  0: "Mon",
  1: "Tue",
  2: "Wed",
  3: "Thu",
  4: "Fri",
  5: "Sat",
  6: "Sun",
};

router.get("/airports", async (req, res) => {
  try {
    const result = await pool.query(`
        SELECT airport_code, airport_name, city
        FROM airports
        ORDER BY city
    `);

    res.status(200).json(
      result.rows.map((el) => {
        return {
          airport_code: el.airport_code,
          airport_name: el.airport_name,
          city: el.city,
        };
      }),
    );
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
});

router.get("/airports/:airport_code/inbound", async (req, res) => {
  const { airport_code } = req.params;

  if (!airport_code) {
    return res.status(400).json({ error: "Airport code not found" });
  }

  try {
    const result = await pool.query(
      `
      SELECT
          r.route_no,
          r.days_of_week,
          r.scheduled_time,
          (r.scheduled_time + r.duration) AS arrival_time,
          a.airport_code,
          a.airport_name,
          a.city
      FROM bookings.routes r
      JOIN bookings.airports a
          ON r.departure_airport = a.airport_code
      WHERE r.arrival_airport = $1;
      `,
      [airport_code],
    );

    res.status(200).json(
      result.rows.map((el) => {
        const daysOfWeek = el.days_of_week.map((day) => Days[day]);

        return {
          days_of_week: daysOfWeek,
          arrival_time: el.arrival_time,
          route_no: el.route_no,
          origin: {
            airport_code: el.airport_code,
            airport_name: el.airport_name,
            city: el.city,
          },
        };
      }),
    );
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
});
router.get("/airports/:airport_code/outbound", async (req, res) => {
  const { airport_code } = req.params;

  if (!airport_code) {
    return res.status(400).json({ error: "Airport code not found" });
  }

  try {
    const result = await pool.query(
      `
        SELECT
            r.route_no,
            r.days_of_week,
            r.scheduled_time AS departure_time,
            a.airport_code,
            a.airport_name,
            a.city
        FROM bookings.routes r
        JOIN bookings.airports a
            ON r.arrival_airport = a.airport_code
        WHERE r.departure_airport = $1;
      `,
      [airport_code],
    );

    res.status(200).json(
      result.rows.map((el) => {
        const daysOfWeek = el.days_of_week.map((day) => Days[day]);

        return {
          days_of_week: daysOfWeek,
          departure_time: el.departure_time,
          route_no: el.route_no,
          destination: {
            airport_code: el.airport_code,
            airport_name: el.airport_name,
            city: el.city,
          },
        };
      }),
    );
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
});

module.exports = router;
