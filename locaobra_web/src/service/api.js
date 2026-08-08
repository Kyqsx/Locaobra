import axios from "axios"

const api = axios.create({
    baseURL: 'http://localhost:8080',
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