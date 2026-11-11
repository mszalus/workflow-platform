import { useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { AppShell, Avatar, Button, Group, NavLink, Stack, Text } from '@mantine/core';
import { useAuth } from '../shared-ui';
import { keycloakUsersUrl } from '../auth/keycloakUrl';
import { useIsAdmin } from '../auth/useIsAdmin';
import { BORDER, SUBTLE_INK, SURFACE } from '../theme';
import CreateItemDialog from './CreateItemDialog';
import NotificationBell from './NotificationBell';
import ProjectSwitcher from './ProjectSwitcher';

const userLinks = [
  { to: '/my-work', label: 'My work' },
  { to: '/items', label: 'Items' },
  { to: '/board', label: 'Board' },
];

const adminLinks = [
  { to: '/admin/projects', label: 'Projects' },
  { to: '/admin/workflows', label: 'Workflows' },
  { to: '/admin/fields', label: 'Fields' },
  { to: '/admin/audit', label: 'Audit log' },
];

function NavLinks({ links }: { links: { to: string; label: string }[] }) {
  const { pathname } = useLocation();
  return links.map((link) => (
    <NavLink
      key={link.to}
      component={Link}
      to={link.to}
      label={link.label}
      active={pathname.startsWith(link.to)}
      style={{ borderRadius: 3 }}
    />
  ));
}

export default function Layout() {
  const { username, tenantId, logout } = useAuth();
  const isAdmin = useIsAdmin();
  const [creating, setCreating] = useState(false);

  return (
    <AppShell header={{ height: 56 }} navbar={{ width: 240, breakpoint: 0 }} padding="xl">
      <AppShell.Header style={{ borderColor: BORDER }}>
        <Group h="100%" px="md" justify="space-between">
          <Group gap="xl">
            <Text fw={700} size="lg" c="brand.6">
              WFP Tracker
            </Text>
            <Button onClick={() => setCreating(true)}>+ Create</Button>
          </Group>
          <Group gap="md">
            <NotificationBell />
            <Group gap="xs">
              <Avatar size="sm" color="brand" radius="xl">
                {username?.charAt(0).toUpperCase()}
              </Avatar>
              <Text size="sm">
                {username} · {tenantId}
              </Text>
            </Group>
            <Button variant="subtle" color="gray" size="compact-sm" onClick={logout}>
              Log out
            </Button>
          </Group>
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="md" style={{ background: SURFACE, borderColor: BORDER }}>
        <Stack gap="lg">
          <ProjectSwitcher />
          <Stack gap={2}>
            <NavLinks links={userLinks} />
          </Stack>
          {isAdmin && (
            <Stack gap={2}>
              <Text size="xs" fw={700} c={SUBTLE_INK} px="sm" mb={4}>
                ADMIN
              </Text>
              <NavLinks links={adminLinks} />
              <NavLink
                component="a"
                href={keycloakUsersUrl}
                target="_blank"
                rel="noreferrer"
                label="Users and groups"
                style={{ borderRadius: 3 }}
              />
            </Stack>
          )}
        </Stack>
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet />
      </AppShell.Main>
      {creating && <CreateItemDialog onClose={() => setCreating(false)} />}
    </AppShell>
  );
}
