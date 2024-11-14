import { useWebSocket } from '../contexts/WebSocketContext';
import { useState, useEffect } from 'react';
import { InfoHolder } from '../types';

const ConnectionStatus = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const ws = useWebSocket();
  const [connectionState, setConnectionState] = useState<number>();

  useEffect(() => {
    if (!ws) return;

    // Update state initially
    setConnectionState(ws.readyState);

    // Listen for WebSocket state changes
    const handleStateChange = () => {
      setConnectionState(ws.readyState);
    };

    ws.addEventListener('open', handleStateChange);
    ws.addEventListener('close', handleStateChange);
    ws.addEventListener('error', handleStateChange);

    return () => {
      ws.removeEventListener('open', handleStateChange);
      ws.removeEventListener('close', handleStateChange);
      ws.removeEventListener('error', handleStateChange);
    };
  }, [ws]);

  if (!infoHolder.info.canAccess) {
    return null;
  }

  if (!ws || connectionState === WebSocket.CONNECTING) {
    return <div className="connection-status in-progress" role="alert">Подключение...</div>;
  }

  if (connectionState === WebSocket.CLOSED || connectionState === WebSocket.CLOSING) {
    return <div className="connection-status lost" role="alert">Соединение потеряно. Переподключение...</div>;
  }

  return null;
};

export default ConnectionStatus;
