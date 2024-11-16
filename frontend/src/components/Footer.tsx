import { useNavigate } from 'react-router-dom';
import { InfoHolder } from '../types';
import { useTranslation } from 'react-i18next';

const Footer = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogout = () => {
    infoHolder.setToken(null);
    navigate('/');
  };

  return (
    <footer>
      {infoHolder.info?.login && (
        <>
          <span>
            {t('auth.loggedInAs')}
&nbsp;
            <strong>{infoHolder.info.login}</strong>
          </span>
          <a onClick={handleLogout}>{t('auth.logout')}</a>
        </>
      )}
      <span>
        <a href="https://github.com/nsychev/balloons-reborn" target="_blank" rel="noopener noreferrer">Open&nbsp;source</a>
      </span>
    </footer>
  );
};

export default Footer;
