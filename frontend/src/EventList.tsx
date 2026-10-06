import { FormEvent, useEffect, useState } from "react";
import { api, EventSummary, money } from "./api";

export default function EventList({ onOpen }: { onOpen: (id: string) => void }) {
  const [events, setEvents] = useState<EventSummary[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ name: "", venue: "", date: "", price: "25", rows: "5", seatsPerRow: "10" });
  const [error, setError] = useState("");

  const load = () => api.events().then(setEvents);
  useEffect(() => { load(); }, []);

  const create = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const ev = await api.createEvent({
        name: form.name,
        venue: form.venue,
        startsAt: new Date(form.date).toISOString(),
        priceCents: Math.round(Number(form.price) * 100),
        rows: Number(form.rows),
        seatsPerRow: Number(form.seatsPerRow),
      });
      onOpen(ev.id);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <section>
      <div className="row">
        <h2>Upcoming events</h2>
        <div className="grow" />
        <button onClick={() => setShowForm(!showForm)}>{showForm ? "Cancel" : "+ New event"}</button>
      </div>
      {showForm && (
        <form className="panel form" onSubmit={create}>
          <input placeholder="Event name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input placeholder="Venue" value={form.venue} onChange={(e) => setForm({ ...form, venue: e.target.value })} />
          <input type="datetime-local" value={form.date} onChange={(e) => setForm({ ...form, date: e.target.value })} />
          <label>Price ($)<input type="number" min="0" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} /></label>
          <label>Rows<input type="number" min="1" max="26" value={form.rows} onChange={(e) => setForm({ ...form, rows: e.target.value })} /></label>
          <label>Seats per row<input type="number" min="1" max="50" value={form.seatsPerRow} onChange={(e) => setForm({ ...form, seatsPerRow: e.target.value })} /></label>
          {error && <p className="error">{error}</p>}
          <button>Create event</button>
        </form>
      )}
      <div className="cards">
        {events.length === 0 && <p className="muted">No upcoming events yet. Create one to get started.</p>}
        {events.map((e) => (
          <button key={e.id} className="event-card" onClick={() => onOpen(e.id)}>
            <strong>{e.name}</strong>
            <span>{e.venue}</span>
            <span className="muted">{new Date(e.startsAt).toLocaleString()}</span>
            <span className="price">{money(e.priceCents)} · {e.totalSeats} seats</span>
          </button>
        ))}
      </div>
    </section>
  );
}
