import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Routes from './routes.jsx'
import './styles/global.css'
import './styles/shared.css'

createRoot(document.getElementById('root')).render(
    <StrictMode>
        <Routes />
    </StrictMode>,
)
