export type NavigationItem = {
  label: string;
  path: string;
  adminOnly?: boolean;
};

export const projectNavigation = {
  name: 'FOXYWHITE',
};

export const navigationItems: NavigationItem[] = [
  { label: 'Dashboard', path: '/' },
  { label: 'Shop', path: '/shop' },
  { label: 'Management', path: '/management' },
  { label: 'Admin Runtime', path: '/admin/runtime', adminOnly: true },
];
