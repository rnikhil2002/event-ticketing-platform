export interface User { id: string; email: string; name: string }
export interface EventSummary { id: string; name: string; venue: string; startsAt: string; priceCents: number; totalSeats: number }
export interface EventDetail { id: string; name: string; venue: string; startsAt: string; priceCents: number; rows: number; seatsPerRow: number; seats: string[] }
export interface Hold { holdId: string; eventId: string; seats: string[]; totalCents: number; expiresAt: string }
export interface Booking { id: string; eventId: string; seats: string[]; totalCents: number; paymentRef: string; createdAt: string }
export interface Availability { eventId: string; booked: string[]; held: string[] }

let token = localStorage.getItem("token") ?? "";
export const setToken = (t: string) => {
  token = t;
  t ? localStorage.setItem("token", t) : localStorage.removeItem("token");
};
export const hasToken = () => !!token;

async function call<T>(method: string, path: string, body?: unknown, headers: Record<string, string> = {}): Promise<T> {
  const res = await fetch(path, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
    body: body ? JSON.stringify(body) : undefined,
  });
  if (res.status === 204) return undefined as T;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error ?? "Request failed");
  return data as T;
}

type Auth = { token: string; user: User };

export const api = {
  register: (email: string, name: string, password: string) => call<Auth>("POST", "/api/users/register", { email, name, password }),
  login: (email: string, password: string) => call<Auth>("POST", "/api/users/login", { email, password }),
  me: () => call<User>("GET", "/api/users/me"),
  events: () => call<EventSummary[]>("GET", "/api/events"),
  event: (id: string) => call<EventDetail>("GET", `/api/events/${id}`),
  createEvent: (e: { name: string; venue: string; startsAt: string; priceCents: number; rows: number; seatsPerRow: number }) =>
    call<EventDetail>("POST", "/api/events", e),
  availability: (eventId: string) => call<Availability>("GET", `/api/bookings/events/${eventId}/availability`),
  hold: (eventId: string, seats: string[]) => call<Hold>("POST", "/api/bookings/holds", { eventId, seats }),
  release: (holdId: string) => call<void>("DELETE", `/api/bookings/holds/${holdId}`),
  // The idempotency key is created once per checkout, so a double click or retry can't charge twice.
  confirm: (holdId: string, idempotencyKey: string) =>
    call<Booking>("POST", "/api/bookings", { holdId }, { "Idempotency-Key": idempotencyKey }),
  myBookings: () => call<Booking[]>("GET", "/api/bookings/me"),
};

export const money = (cents: number) => `$${(cents / 100).toFixed(2)}`;
