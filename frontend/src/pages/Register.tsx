import { useState, FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import backendUrls from '../util/backendUrls';

const Register = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const navigate = useNavigate();
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  if (infoHolder.info?.login) {
    return <Navigate to="/" />;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 6) {
      setError('Пароль должен содержать минимум 6 символов');
      return;
    }

    if (password !== confirmPassword) {
      setError('Пароли не совпадают');
      return;
    }

    setIsLoading(true);

    try {
      const response = await fetch(backendUrls.register(), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ login, password }),
      });

      if (response.status === 409) {
        setError('Пользователь с таким логином уже существует');
        return;
      }

      if (response.ok) {
        const data = await response.json() as { token: string };
        infoHolder.setToken(data.token);
        navigate('/');
      }
    }
    catch (err) {
      console.error(err);
      setError('Произошла ошибка при регистрации');
    }
    finally {
      setIsLoading(false);
    }
  };

  return (
    <main>
      <h1 className="sr-only">Регистрация</h1>
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
          minLength={6}
        />
        <label htmlFor="confirmPassword">Повторите пароль</label>
        <input
          type="password"
          id="confirmPassword"
          value={confirmPassword}
          onChange={e => setConfirmPassword(e.target.value)}
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
          {isLoading ? 'Загрузка...' : 'Зарегистрироваться'}
        </button>
      </form>
    </main>
  );
};

export default Register;
