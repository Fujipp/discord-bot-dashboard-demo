export type NavigationItem = {
  label: string;
  path: string;
};

export const navigationItems: NavigationItem[] = [
  { label: 'Main', path: '/' },
  { label: 'Shop', path: '/shop' },
  { label: 'Management', path: '/management' },
];
