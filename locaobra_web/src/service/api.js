import axios from "axios"

// URL da API. Em dev usa o VITE_API_URL (.env) ou cai no localhost.
// Em produção (build), o Vite injeta o valor de VITE_API_URL no bundle.
const api = axios.create({
    baseURL: 'https://locaobra.onrender.com',
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