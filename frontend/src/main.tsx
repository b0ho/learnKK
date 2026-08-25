import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';
// Self-hosted Pretendard variable font (single woff2, all weights — bundled
// locally by Vite; no external CDN, satisfies C2; lighter than the 9 static weights).
import 'pretendard/dist/web/variable/pretendardvariable.css';
import './index.css';

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element #root not found');
}

createRoot(rootElement).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
