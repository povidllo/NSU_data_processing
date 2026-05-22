const express = require("express");
const citiesRouter = require("./routes/cities");
const airportsRouter = require("./routes/airports");
const bookingsRouter = require("./routes/bookings");
const checkInRouter = require("./routes/checkIn");
require("dotenv").config();

const app = express();

app.use(express.json());

app.use(citiesRouter);
app.use(airportsRouter);
app.use(bookingsRouter);
app.use(checkInRouter);

const PORT = process.env.PORT || 3000;

app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});
