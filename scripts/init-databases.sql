-- Each service owns its own database, so services can't reach into each other's tables.
CREATE DATABASE users;
CREATE DATABASE events;
CREATE DATABASE bookings;
