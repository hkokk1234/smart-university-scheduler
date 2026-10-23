import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App } from './app/App'
import './index.css'
import { ApiError } from '@/lib/api'

const client =new QueryClient({ defaultOptions: { queries: { staleTime: 60_000, retry: (count, error) => count < 1 && !(error instanceof ApiError && [400, 401, 403, 404].includes(error.status)) } } })
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><QueryClientProvider client={client}><BrowserRouter><App /></BrowserRouter></QueryClientProvider></React.StrictMode>,
)
