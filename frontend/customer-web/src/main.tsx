import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './features/search/search.css'
import './features/restaurant/restaurant.css'
import './features/cart/cart.css'
import './features/checkout/checkout.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
