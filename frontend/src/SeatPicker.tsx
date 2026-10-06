import { useEffect, useMemo, useRef, useState } from "react";
import { api, Availability, EventDetail, Hold, money } from "./api";

export default function SeatPicker({ eventId, onDone }: { eventId: string; onDone: () => void }) {
  const [event, setEvent] = useState<EventDetail | null>(null);
  const [avail, setAvail] = useState<Availability>({ eventId, booked: [], held: [] });
  const [selected, setSelected] = useState<string[]>([]);
  const [hold, setHold] = useState<Hold | null>(null);
  const [secondsLeft, setSecondsLeft] = useState(0);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const checkoutKey = useRef<string>("");

  const refresh = () => api.availability(eventId).then(setAvail).catch(() => {});

  useEffect(() => {
    api.event(eventId).then(setEvent).catch((e) => setError(e.message));
    refresh();
    const t = setInterval(refresh, 5000); // keep the seat map fresh while people book
    return () => clearInterval(t);
  }, [eventId]);

  useEffect(() => {
    if (!hold) return;
    const tick = () => setSecondsLeft(Math.max(0, Math.round((new Date(hold.expiresAt).getTime() - Date.now()) / 1000)));
    tick();
    const t = setInterval(tick, 1000);
    return () => clearInterval(t);
  }, [hold]);

  const taken = useMemo(() => new Set([...avail.booked, ...avail.held]), [avail]);

  if (error && !event) return <p className="error">{error}</p>;
  if (!event) return <p>Loading...</p>;

  const toggle = (seat: string) => {
    if (hold || taken.has(seat)) return;
    setSelected((s) => (s.includes(seat) ? s.filter((x) => x !== seat) : s.length < 10 ? [...s, seat] : s));
  };

  const reserve = async () => {
    setError("");
    setBusy(true);
    try {
      const h = await api.hold(event.id, selected);
      checkoutKey.current = crypto.randomUUID();
      setHold(h);
    } catch (e) {
      setError((e as Error).message);
      setSelected([]);
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const pay = async () => {
    if (!hold) return;
    setBusy(true);
    setError("");
    try {
      await api.confirm(hold.holdId, checkoutKey.current);
      onDone();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    if (hold) await api.release(hold.holdId).catch(() => {});
    setHold(null);
    setSelected([]);
    refresh();
  };

  const rows = Array.from({ length: event.rows }, (_, r) => String.fromCharCode(65 + r));

  return (
    <section>
      <h2>{event.name}</h2>
      <p className="muted">{event.venue} · {new Date(event.startsAt).toLocaleString()} · {money(event.priceCents)} per seat</p>

      <div className="stage">STAGE</div>
      <div className="seatmap">
        {rows.map((row) => (
          <div key={row} className="seatrow">
            <span className="rowlabel">{row}</span>
            {Array.from({ length: event.seatsPerRow }, (_, i) => {
              const id = `${row}${i + 1}`;
              const cls = avail.booked.includes(id) ? "sold" : selected.includes(id) ? "mine" : avail.held.includes(id) ? "held" : "free";
              return (
                <button key={id} className={`seat ${cls}`} title={id} onClick={() => toggle(id)} disabled={cls === "sold" || cls === "held"}>
                  {i + 1}
                </button>
              );
            })}
          </div>
        ))}
      </div>
      <div className="legend">
        <span><i className="seat free" /> Available</span>
        <span><i className="seat mine" /> Your pick</span>
        <span><i className="seat held" /> Being booked</span>
        <span><i className="seat sold" /> Sold</span>
      </div>

      <div className="panel checkout">
        {!hold ? (
          <>
            <span>{selected.length ? `${selected.sort().join(", ")} · ${money(selected.length * event.priceCents)}` : "Pick up to 10 seats"}</span>
            <button disabled={!selected.length || busy} onClick={reserve}>Reserve seats</button>
          </>
        ) : (
          <>
            <span>
              Holding {hold.seats.join(", ")} for <b>{Math.floor(secondsLeft / 60)}:{String(secondsLeft % 60).padStart(2, "0")}</b> · Total {money(hold.totalCents)}
            </span>
            <button className="ghost" onClick={cancel}>Cancel</button>
            <button disabled={busy || secondsLeft === 0} onClick={pay}>{busy ? "Processing..." : "Pay now"}</button>
          </>
        )}
      </div>
      {error && <p className="error">{error}</p>}
    </section>
  );
}
