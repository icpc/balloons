import { useState, FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import backendUrls from '../util/backendUrls';
import { useTranslation } from 'react-i18next';

const Login = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const navigate = useNavigate();
  const { t } = useTranslation();
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
        setError(t('auth.errors.invalidCredentials'));
        return;
      }

      if (response.ok) {
        const data = await response.json() as { token: string };
        infoHolder.setToken(data.token);
        navigate('/');
      }
    } catch (err) {
      console.error(err);
      setError(t('auth.errors.loginFailed'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <main>
      <h1 className="sr-only">{t('auth.login')}</h1>
      <form onSubmit={(e) => { void handleSubmit(e); }}>
        <label htmlFor="login">{t('auth.username')}</label>
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
        <label htmlFor="password">{t('auth.password')}</label>
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
          {isLoading ? t('common.loading') : t('auth.loginButton')}
        </button>
      </form>
    </main>
  );
};

export default Login;
