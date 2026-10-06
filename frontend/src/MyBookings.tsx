import { useEffect, useState } from "react";
import { api, Booking, money } from "./api";

export default function MyBookings() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  useEffect(() => { api.myBookings().then(setBookings); }, []);

  return (
    <section>
      <h2>My bookings</h2>
      {bookings.length === 0 && <p className="muted">No bookings yet.</p>}
      {bookings.map((b) => (
        <div key={b.id} className="panel booking">
          <strong>Seats {b.seats.join(", ")}</strong>
          <span>{money(b.totalCents)}</span>
          <span className="muted">Booked {new Date(b.createdAt).toLocaleString()} · ref {b.paymentRef.slice(0, 12)}</span>
        </div>
      ))}
    </section>
  );
}
