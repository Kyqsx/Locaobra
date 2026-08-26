import axios from "axios"

// URL da API. Em dev usa o VITE_API_URL (.env/.env.local) ou cai no Render de
// produção. Em produção (build), o Vite injeta o valor de VITE_API_URL no bundle
// (definido no painel da Vercel, ou usa o .env versionado como fallback).
const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'https://locaobra.onrender.com',
    // sem Content-Type fixo — Axios detecta automaticamente:
    // JSON object → application/json
    // FormData   → multipart/form-data; boundary=...
});

api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;