import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.tsx'
import { ThemeProvider } from './components/ThemeProvider'

async function enableMocks() {
  // Mocks run only in the dev server. In a production build import.meta.env.DEV is false, so this whole
  // branch (and the msw code behind the dynamic import) is removed from the bundle.
  if (!(import.meta.env.DEV && import.meta.env.VITE_USE_MOCKS === 'true')) return
  const { worker } = await import('./mocks/browser')
  await worker.start({ onUnhandledFrame: 'bypass' })
}

enableMocks().then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <ThemeProvider>
        <App />
      </ThemeProvider>
    </StrictMode>,
  )
})
