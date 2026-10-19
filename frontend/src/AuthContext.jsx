import { createContext, useContext, useState } from "react";
import api from "./api";

// Keeps the logged-in user in one place so any component can read it.
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem("user");
    return stored ? JSON.parse(stored) : null;
  });

  async function login(email, password) {
    const response = await api.post("/auth/login", { email, password });
    saveSession(response.data);
    return response.data;
  }

  async function register(name, email, password, role) {
    const response = await api.post("/auth/register", { name, email, password, role });
    saveSession(response.data);
    return response.data;
  }

  function saveSession(data) {
    const currentUser = {
      id: data.userId,
      name: data.name,
      email: data.email,
      role: data.role,
    };
    localStorage.setItem("token", data.token);
    localStorage.setItem("user", JSON.stringify(currentUser));
    setUser(currentUser);
  }

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
