import { Link, useLocation } from 'react-router-dom';
import { InfoHolder } from '../types';
import { HTMLProps } from 'react';
import HallSelector from './HallSelector';

const NavLink = ({ to, children }: { to: string, children: React.ReactNode }) => {
  const location = useLocation();

  function getLinkClass(path: string): HTMLProps<HTMLAnchorElement> {
    return location.pathname === path ? { 'className': 'active', 'aria-current': 'page' } : {};
  };

  return <Link {...getLinkClass(to)} to={to}>{children}</Link>;
};

const Navbar = ({ infoHolder }: { infoHolder: InfoHolder }) => {
  const location = useLocation();

  return (
    <nav className="navbar navbar-expand-lg navbar-light bg-light">
      {!infoHolder.info.login
        ? (
            <>
              <NavLink to="/login">Вход</NavLink>
              {infoHolder.info.canRegister && <NavLink to="/register">Регистрация</NavLink>}
            </>
          )
        : (
            <>
              {infoHolder.info.canAccess && (
                <>
                  {['/', '/delivered', '/standings'].includes(location.pathname) && <HallSelector />}
                  <NavLink to="/">Очередь</NavLink>
                  <NavLink to="/delivered">Доставлено</NavLink>
                  <NavLink to="/standings">Таблица</NavLink>
                  <NavLink to="/rating">Волонтёры</NavLink>
                </>
              )}
              {infoHolder.info.canManage && <NavLink to="/access">Доступ</NavLink>}
            </>
          )}
    </nav>
  );
};

export default Navbar;
