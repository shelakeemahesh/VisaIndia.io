import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

const HASH_ROUTE_MAP: Record<string, string> = {
  '#categories': '/visa-categories',
  '#requirements': '/requirements',
  '#how-it-works': '/how-it-works',
  '#existing': '/existing-applications',
  '#existing-applications': '/existing-applications',
  '#notices': '/notices',
  '#help': '/help',
  '#oci': '/visa-categories#oci',
};

export const HashRedirector: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    if (location.pathname === '/' && location.hash) {
      const targetRoute = HASH_ROUTE_MAP[location.hash.toLowerCase()];
      if (targetRoute) {
        navigate(targetRoute, { replace: true });
      }
    }
  }, [location, navigate]);

  return null;
};
