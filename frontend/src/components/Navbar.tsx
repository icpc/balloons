import { Link, useLocation } from 'react-router-dom';
import { InfoHolder } from '../types';
import { HTMLProps } from 'react';
import HallSelector from './HallSelector';
import { useTranslation } from 'react-i18next';

const NavLink = ({ to, children }: { to: string, children: React.ReactNode }) => {
  const location = useLocation();

  function getLinkClass(path: string): HTMLProps<HTMLAnchorElement> {
    return location.pathname === path ? { 'className': 'active', 'aria-current': 'page' } : {};
  };

  return <Link {...getLinkClass(to)} to={to}>{children}</Link>;
};

const Navbar = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const location = useLocation();
  const { t } = useTranslation();

  return (
    <nav className="navbar navbar-expand-lg navbar-light bg-light">
      {!infoHolder.info.login
        ? (
            <>
              <NavLink to="/login">{t('auth.login')}</NavLink>
              {infoHolder.info.canRegister && <NavLink to="/register">{t('auth.register')}</NavLink>}
            </>
          )
        : (
            <>
              {infoHolder.info.canAccess && (
                <>
                  {['/', '/delivered', '/standings'].includes(location.pathname) && <HallSelector />}
                  <NavLink to="/">{t('navigation.queue')}</NavLink>
                  <NavLink to="/delivered">{t('navigation.delivered')}</NavLink>
                  <NavLink to="/standings">{t('navigation.standings')}</NavLink>
                  <NavLink to="/rating">{t('navigation.volunteers')}</NavLink>
                </>
              )}
              {infoHolder.info.canManage && <NavLink to="/access">{t('navigation.access')}</NavLink>}
            </>
          )}
    </nav>
  );
};

export default Navbar;
