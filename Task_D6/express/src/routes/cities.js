const pool = require("../db/pool");
const router = require("express").Router();

router.get("/cities", async (req, res) => {
  try {
    const result = await pool.query(`
      SELECT DISTINCT city
      FROM bookings.airports
      ORDER BY city
    `);

    res.status(200).json(result.rows.map((r) => r.city));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Database error" });
  }
});

router.get("/cities/:city/airports", async (req, res) => {
  const { city } = req.params;
  if (!city) {
    res.status(404).json({ error: "City not found" });
  }

  try {
    const result = await pool.query(
      `        
        SELECT airport_code, airport_name, city
        FROM airports
        WHERE city = $1`,
      [city],
    );
    if (result.rowCount === 0) {
      res.status(404).json({ error: "City not found" });
    }
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

module.exports = router;
