import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Routes from './routes.jsx'
import './styles/global.css'
import { AuthProvider } from './utils/useAuth'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <AuthProvider>
            <Routes />
        </AuthProvider>
    </StrictMode>,
)
