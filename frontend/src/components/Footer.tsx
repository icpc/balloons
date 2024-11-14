import { useNavigate } from 'react-router-dom';
import { InfoHolder } from '../types';

const Footer = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    infoHolder.setToken(null);
    navigate('/');
  };

  return (
    <footer>
      {infoHolder.info?.login && (
        <>
          <span>
            Вы вошли как
            <strong>{infoHolder.info.login}</strong>
          </span>
          <a onClick={handleLogout}>Выйти</a>
        </>
      )}
      <span>
        <a href="https://github.com/nsychev/balloons-reborn" target="_blank" rel="noopener noreferrer">Open&nbsp;source</a>
      </span>

    </footer>
  );
};

export default Footer;
