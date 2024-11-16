import { useEffect, useState, useCallback, useRef } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { Info, InfoHolder, WebSocketMessage } from './types';
import backendUrls from './util/backendUrls';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import Login from './pages/Login';
import Register from './pages/Register';
import VolunteerAccess from './pages/VolunteerAccess';
import ActiveBalloons from './pages/ActiveBalloons';
import { GlobalError } from './components/GlobalError';
import { Provider } from 'react-redux';
import { store } from './store/store';
import { useDispatch } from 'react-redux';
import { setContest } from './store/contestSlice';
import { updateBalloon, deleteBalloon, setBalloons } from './store/balloonsSlice';
import { WebSocketContext } from './contexts/WebSocketContext';
import DeliveredBalloons from './pages/DeliveredBalloons';
import VolunteerRating from './pages/VolunteerRating';
import Standings from './pages/Standings';
import ConnectionStatus from './components/ConnectionStatus';
import { useTranslation } from 'react-i18next';
import i18n from './i18n/config';

const RECONNECT_DELAY = 3000; // 3 seconds

function AppContent() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [info, setInfo] = useState<Info>({ status: 'loading', language: 'EN' });
  const [ws, setWs] = useState<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<number>();

  const setTokenWithStorage = useCallback((newToken: string | null) => {
    if (newToken) {
      localStorage.setItem('token', newToken);
    } else {
      localStorage.removeItem('token');
    }
    setToken(newToken);
  }, []);

  const fetchInfo = useCallback(async () => {
    try {
      const response = await fetch(backendUrls.getInfo(), {
        headers: token
          ? {
              Authorization: `Bearer ${token}`,
            }
          : undefined,
      });
      const data = await response.json() as Info;
      setInfo({ ...data, status: 'success' });
    } catch (exc) {
      console.error('Error fetching info:', exc);
      setInfo(prevInfo => ({ ...prevInfo, status: 'error' }));
    }
  }, [token]);

  useEffect(() => {
    void fetchInfo();
  }, [fetchInfo]);

  useEffect(() => {
    if (info.language) {
      void i18n.changeLanguage(info.language.toLowerCase());
    }
  }, [info.language]);

  const createWebSocket = useCallback(() => {
    const websocket = new WebSocket(
      `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${backendUrls.eventStream()}`,
    );

    websocket.onopen = () => {
      websocket.send(token!);
      // Clear any pending reconnection attempts
      if (reconnectTimeoutRef.current) {
        window.clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = undefined;
      }
    };

    websocket.onclose = () => {
      // Schedule reconnection
      if (!reconnectTimeoutRef.current) {
        reconnectTimeoutRef.current = window.setTimeout(() => {
          reconnectTimeoutRef.current = undefined;
          if (token && info.canAccess) {
            setWs(createWebSocket());
          }
        }, RECONNECT_DELAY);
      }
    };

    websocket.onmessage = (event: MessageEvent<string>) => {
      const message = JSON.parse(event.data) as WebSocketMessage;

      if ('type' in message) {
        switch (message.type) {
          case 'contestUpdated':
            dispatch(setContest(message.contest));
            break;
          case 'balloonUpdated':
            dispatch(updateBalloon(message.balloon));
            break;
          case 'balloonDeleted':
            dispatch(deleteBalloon(message.runId));
            break;
        }
      } else {
        // Handle initial State
        const state = message;
        dispatch(setContest(state.contest));
        dispatch(setBalloons(state.balloons));
      }
    };

    return websocket;
  }, [token, dispatch, info.canAccess]);

  useEffect(() => {
    if (!token || !info.canAccess) {
      if (ws) {
        ws.close();
        setWs(null);
      }
      return;
    }

    const websocket = createWebSocket();
    setWs(websocket);

    return () => {
      websocket.close();
      if (reconnectTimeoutRef.current) {
        window.clearTimeout(reconnectTimeoutRef.current);
      }
      setWs(null);
    };
    // we shouldn't add ws here
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, info.canAccess, createWebSocket]);

  if (info.status === 'loading') {
    return (
      <GlobalError
        title={t('common.loading')}
        message={t('app.fetchingData')}
      />
    );
  }

  const infoHolder: InfoHolder = { token, setToken: setTokenWithStorage, info, fetchInfo };

  return (
    <WebSocketContext.Provider value={ws}>
      <ConnectionStatus infoHolder={infoHolder} />
      <Navbar infoHolder={infoHolder} />
      <Routes>
        <Route path="/" element={<ActiveBalloons infoHolder={infoHolder} />} />
        <Route path="/delivered" element={<DeliveredBalloons infoHolder={infoHolder} />} />
        <Route path="/standings" element={<Standings infoHolder={infoHolder} />} />
        <Route path="/rating" element={<VolunteerRating infoHolder={infoHolder} />} />
        <Route path="/access" element={<VolunteerAccess infoHolder={infoHolder} />} />
        <Route path="/login" element={<Login infoHolder={infoHolder} />} />
        <Route path="/register" element={<Register infoHolder={infoHolder} />} />
        <Route
          path="*"
          element={(
            <GlobalError
              title="404"
              message={t('errors.pageNotFound')}
            />
          )}
        />
      </Routes>
      <Footer infoHolder={infoHolder} />
    </WebSocketContext.Provider>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Provider store={store}>
        <AppContent />
      </Provider>
    </BrowserRouter>
  );
}

export default App;
