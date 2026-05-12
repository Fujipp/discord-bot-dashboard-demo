export type BotStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE';
export type SubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELED';
export type FeatureCategory = 'MODERATION' | 'MUSIC' | 'SUPPORT' | 'ANALYTICS' | 'AUTOMATION' | 'AI';
export type BotBillingMode = 'FREE' | 'PAID';

export type FeatureResponse = {
  id: number;
  code: string;
  name: string;
  description: string;
  monthlyPriceCents: number;
  currency: string;
  category: FeatureCategory;
};

export type BotFeatureResponse = {
  subscriptionId: number;
  featureId: number;
  code: string;
  name: string;
  status: SubscriptionStatus;
  currentPeriodEnd: string | null;
  autoRenew: boolean;
};

export type BotResponse = {
  id: number;
  discordApplicationId: string;
  pm2ProcessName: string | null;
  name: string;
  avatarUrl: string | null;
  status: BotStatus;
  billingMode: BotBillingMode;
  monthlyPriceCents: number;
  serverCount: number;
  commandCount: number;
  uptimePercent: number;
  hostedRegion: string;
  lastHeartbeatAt: string | null;
  activeFeatures: BotFeatureResponse[];
};

export type BillingSummaryResponse = {
  status: SubscriptionStatus;
  monthlyTotalCents: number;
  currency: string;
  currentPeriodEnd: string | null;
  cancelAtPeriodEnd: boolean;
};

export type DashboardSummaryResponse = {
  botCount: number;
  onlineBotCount: number;
  connectedServerCount: number;
  commandCount: number;
  activeFeatureCount: number;
};

export type CustomerDashboardResponse = {
  summary: DashboardSummaryResponse;
  billing: BillingSummaryResponse;
  bots: BotResponse[];
  availableFeatures: FeatureResponse[];
};

export type RuntimeProcessResponse = {
  name: string;
  status: string;
  pid: number;
  cpu: number;
  memoryBytes: number;
  restartCount: number;
  uptimeMillis: number;
};

export type RuntimeCommandResponse = {
  processName: string;
  action: string;
  success: boolean;
  output: string;
  processes: RuntimeProcessResponse[];
};

export type RuntimeHostMetricsResponse = {
  diskTotalBytes: number;
  diskUsedBytes: number;
  diskAvailableBytes: number;
  diskUsedPercent: number;
  memoryTotalBytes: number;
  memoryUsedBytes: number;
  memoryAvailableBytes: number;
  swapTotalBytes: number;
  swapUsedBytes: number;
  loadOneMinute: number;
  uptime: string;
};

export type RuntimeHostOptionResponse = {
  id: string;
  name: string;
  host: string;
  region: string;
  primary: boolean;
};

export type AdminUserOptionResponse = {
  id: number;
  email: string;
  username: string;
  role: 'USER' | 'ADMIN';
};

export type AdminRuntimeBotResponse = {
  id: number;
  ownerUserId: number;
  ownerEmail: string | null;
  ownerUsername: string | null;
  name: string;
  pm2ProcessName: string;
  status: BotStatus;
  billingMode: BotBillingMode;
  monthlyPriceCents: number;
};

export type AdminRuntimeProcessResponse = {
  runtime: RuntimeProcessResponse;
  bot: AdminRuntimeBotResponse | null;
};

export type AdminRuntimeDashboardResponse = {
  selectedHostId: string;
  hosts: RuntimeHostOptionResponse[];
  host: RuntimeHostMetricsResponse;
  processes: AdminRuntimeProcessResponse[];
  users: AdminUserOptionResponse[];
};

export type AdminProcessAssignmentPayload = {
  ownerUserId: number;
  botName: string;
  billingMode: BotBillingMode;
  monthlyPriceCents: number;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export async function getCustomerDashboard(accessToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/customer/dashboard`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not load customer dashboard');
  }

  return data as CustomerDashboardResponse;
}

export async function getRuntimeProcesses(accessToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/customer/runtime/processes`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not load pm2 runtime processes');
  }

  return data as RuntimeProcessResponse[];
}

export async function runRuntimeAction(
  accessToken: string,
  processName: string,
  action: 'start' | 'stop' | 'restart',
) {
  const response = await fetch(
    `${API_BASE_URL}/api/customer/runtime/processes/${encodeURIComponent(processName)}/${action}`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not run pm2 runtime action');
  }

  return data as RuntimeCommandResponse;
}

export async function getAdminRuntimeDashboard(
  accessToken: string,
  options: { hostId?: string; userSearch?: string } = {},
) {
  const params = new URLSearchParams();
  if (options.hostId) params.set('hostId', options.hostId);
  if (options.userSearch) params.set('userSearch', options.userSearch);

  const query = params.toString();
  const response = await fetch(`${API_BASE_URL}/api/admin/runtime/processes${query ? `?${query}` : ''}`, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not load admin runtime dashboard');
  }

  return data as AdminRuntimeDashboardResponse;
}

export async function assignRuntimeProcess(
  accessToken: string,
  hostId: string,
  processName: string,
  payload: AdminProcessAssignmentPayload,
) {
  const params = new URLSearchParams({ hostId });
  const response = await fetch(
    `${API_BASE_URL}/api/admin/runtime/processes/${encodeURIComponent(processName)}/assignment?${params}`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    },
  );

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not assign pm2 process');
  }

  return data as AdminRuntimeProcessResponse;
}

export async function runAdminRuntimeAction(
  accessToken: string,
  hostId: string,
  processName: string,
  action: 'start' | 'stop' | 'restart',
) {
  const params = new URLSearchParams({ hostId });
  const response = await fetch(
    `${API_BASE_URL}/api/admin/runtime/processes/${encodeURIComponent(processName)}/${action}?${params}`,
    {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  );

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(data?.message ?? 'Could not run pm2 runtime action');
  }

  return data as RuntimeProcessResponse;
}
