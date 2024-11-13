import { useEffect, useState, useCallback } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Info, InfoHolder, State, WebSocketMessage } from './types'
import backendUrls from './util/backendUrls'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Login from './pages/Login'
import Register from './pages/Register'
import Main from './pages/Main'
import VolunteerAccess from './pages/VolunteerAccess'
import ActiveBalloons from './pages/ActiveBalloons'
import { GlobalError } from './components/GlobalError'
import { Provider } from 'react-redux';
import { store } from './store/store';
import { useDispatch } from 'react-redux';
import { setProblems } from './store/problemsSlice';
import { updateBalloon, deleteBalloon, setBalloons } from './store/balloonsSlice';
import { WebSocketContext } from './contexts/WebSocketContext';
import DeliveredBalloons from './pages/DeliveredBalloons'
import VolunteerRating from './pages/VolunteerRating'
import Standings from './pages/Standings'

function AppContent() {
  const dispatch = useDispatch();
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'));
  const [info, setInfo] = useState<Info>({ contestName: 'Unknown', status: 'loading' });
  const [ws, setWs] = useState<WebSocket | null>(null);

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
        headers: token ? {
          Authorization: `Bearer ${token}`
        } : undefined
      });
      const data = await response.json() as Info;
      setInfo({ ...data, status: 'success' })
    } catch (exc) {
      console.error('Error fetching info:', exc)
      setInfo(prevInfo => ({ ...prevInfo, status: 'error' }))
    }
  }, [token]);

  useEffect(() => {
    void fetchInfo();
  }, [fetchInfo]);

  useEffect(() => {
    if (!token) {
      if (ws) {
        ws.close();
        setWs(null);
      }
      return;
    }

    const websocket = new WebSocket(
      `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}${backendUrls.eventStream()}`
    );
    setWs(websocket);

    websocket.onopen = () => {
      websocket.send(token);
    };

    websocket.onmessage = (event) => {
      const message: WebSocketMessage = JSON.parse(event.data);
      
      if ('type' in message) {
        switch (message.type) {
          case 'problemsUpdated':
            dispatch(setProblems(message.problems));
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
        const state = message as State;
        dispatch(setProblems(state.problems));
        dispatch(setBalloons(state.balloons));
      }
    };

    return () => {
      websocket.close();
      setWs(null);
    };
  }, [token, dispatch]);

  if (info.status === 'loading') {
    return (
      <div className="global-error">
        <h1>Loading...</h1>
        <p>Please wait while we load the application.</p>
      </div>
    );
  }

  const infoHolder: InfoHolder = { token, setToken: setTokenWithStorage, info, fetchInfo };

  return (
    <WebSocketContext.Provider value={ws}>
      <Navbar infoHolder={infoHolder} />
      <Routes>
        <Route path="/" element={<Main infoHolder={infoHolder} />} />
        <Route path="/queue" element={<ActiveBalloons infoHolder={infoHolder} />} />
        <Route path="/delivered" element={<DeliveredBalloons infoHolder={infoHolder} />} />
        <Route path="/standings" element={<Standings infoHolder={infoHolder} />} />
        <Route path="/rating" element={<VolunteerRating infoHolder={infoHolder} />} />
        <Route path="/access" element={<VolunteerAccess infoHolder={infoHolder} />} />
        <Route path="/login" element={<Login infoHolder={infoHolder} />} />
        <Route path="/register" element={<Register infoHolder={infoHolder} />} />
        <Route path="*" element={
          <GlobalError title="Not Found" message="The page you&apos;re looking for does not exist." />
        } />
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
