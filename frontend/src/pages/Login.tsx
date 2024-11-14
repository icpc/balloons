import { useState, FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import backendUrls from '../util/backendUrls';

const Login = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const navigate = useNavigate();
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  if (infoHolder.info.login) {
    return <Navigate to="/" />;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setIsLoading(true);

    try {
      const response = await fetch(backendUrls.login(), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ login, password }),
      });

      if (response.status === 403) {
        setError('Неверный логин или пароль');
        return;
      }

      if (response.ok) {
        const data = await response.json() as { token: string };
        infoHolder.setToken(data.token);
        navigate('/');
      }
    } catch (err) {
      console.error(err);
      setError('Произошла ошибка при входе');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main>
      <h1 className="sr-only">Вход</h1>
      <form onSubmit={(e) => { void handleSubmit(e); }}>
        <label htmlFor="login">Логин</label>
        <input
          type="text"
          id="login"
          value={login}
          onChange={e => setLogin(e.target.value)}
          disabled={isLoading}
          required
          autoComplete="username"
          autoCapitalize="none"
          autoCorrect="false"
        />
        <label htmlFor="password">Пароль</label>
        <input
          type="password"
          id="password"
          value={password}
          onChange={e => setPassword(e.target.value)}
          disabled={isLoading}
          required
        />
        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}
        <button
          type="submit"
          disabled={isLoading}
        >
          {isLoading ? 'Загрузка...' : 'Войти'}
        </button>
      </form>
    </main>
  );
};

export default Login;
