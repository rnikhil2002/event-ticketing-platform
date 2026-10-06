import { FormEvent, useEffect, useState } from "react";
import { api, hasToken, setToken, User } from "./api";
import EventList from "./EventList";
import SeatPicker from "./SeatPicker";
import MyBookings from "./MyBookings";

type View = { page: "events" } | { page: "event"; id: string } | { page: "bookings" };

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [view, setView] = useState<View>({ page: "events" });

  useEffect(() => {
    if (hasToken()) api.me().then(setUser).catch(() => setToken(""));
  }, []);

  if (!user) return <Login onLogin={setUser} />;

  return (
    <div>
      <header className="top">
        <button className="brand" onClick={() => setView({ page: "events" })}>🎟 Tickets</button>
        <div className="grow" />
        <button className="ghost" onClick={() => setView({ page: "bookings" })}>My bookings</button>
        <span className="muted">{user.name}</span>
        <button className="ghost" onClick={() => { setToken(""); setUser(null); }}>Log out</button>
      </header>
      <main>
        {view.page === "events" && <EventList onOpen={(id) => setView({ page: "event", id })} />}
        {view.page === "event" && <SeatPicker eventId={view.id} onDone={() => setView({ page: "bookings" })} />}
        {view.page === "bookings" && <MyBookings />}
      </main>
    </div>
  );
}

function Login({ onLogin }: { onLogin: (u: User) => void }) {
  const [mode, setMode] = useState<"login" | "register">("login");
  const [form, setForm] = useState({ email: "", name: "", password: "" });
  const [error, setError] = useState("");

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const res = mode === "login" ? await api.login(form.email, form.password) : await api.register(form.email, form.name, form.password);
      setToken(res.token);
      onLogin(res.user);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  return (
    <div className="center">
      <form className="panel auth" onSubmit={submit}>
        <h1>{mode === "login" ? "Log in" : "Create account"}</h1>
        {mode === "register" && <input placeholder="Name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />}
        <input type="email" placeholder="Email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input type="password" placeholder="Password (8+ characters)" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        {error && <p className="error">{error}</p>}
        <button>{mode === "login" ? "Log in" : "Sign up"}</button>
        <button type="button" className="link" onClick={() => setMode(mode === "login" ? "register" : "login")}>
          {mode === "login" ? "New here? Create an account" : "Have an account? Log in"}
        </button>
      </form>
    </div>
  );
}
