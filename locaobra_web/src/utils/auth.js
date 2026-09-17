export const getUserRole = () => {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.tipo;
    } catch (error) {
        console.error("Erro ao ler role do token:", error);
        return null;
    }
};

export const getUserId = () => {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.id;
    } catch (error) {
        console.error("Erro ao ler ID do token:", error);
        return null;
    }
};

export const getUserEmail = () => {
    const token = localStorage.getItem('token');
    if (!token) return null;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.email || payload.sub;
    } catch (error) {
        console.error("Erro ao ler email do token:", error);
        return null;
    }
};