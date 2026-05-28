/**
 * Mock Data for Dev Mode
 *
 * Provides mock API response data when running without backend.
 * This enables the frontend to render pages independently.
 */
/**
 * Default login logo SVG
 */
export const DEFAULT_LOGIN_LOGO = '/images/login-logo.svg';
/**
 * Default login banner image
 */
export const DEFAULT_LOGIN_BANNER = '/images/login-banner.png';
/**
 * Mock page configuration for dev mode
 */
export const MOCK_PAGE_CONFIG = {
    title: 'Cordys CRM',
    icon: [],
    loginLogo: [{ url: DEFAULT_LOGIN_LOGO }],
    loginImage: [{ url: DEFAULT_LOGIN_BANNER }],
    slogan: 'login.form.title',
    style: 'default',
    theme: 'default',
    customStyle: '#f9fbfb',
    customTheme: '#008d91',
};
/**
 * Mock module configuration for dev mode
 * Enables all modules by default
 */
export const MOCK_MODULE_CONFIG = [
    { moduleKey: 'DASHBOARD', enable: true },
    { moduleKey: 'AGENT', enable: true },
    { moduleKey: 'HOME', enable: true },
    { moduleKey: 'CUSTOMER_MANAGEMENT', enable: true },
    { moduleKey: 'CONTRACT', enable: true },
    { moduleKey: 'ORDER', enable: true },
    { moduleKey: 'CLUE_MANAGEMENT', enable: true },
    { moduleKey: 'BUSINESS_MANAGEMENT', enable: true },
    { moduleKey: 'PRODUCT_MANAGEMENT', enable: true },
    { moduleKey: 'TENDER', enable: true },
];
/**
 * Mock user info for dev mode
 */
export const MOCK_USER_INFO = {
    id: 'dev-user-id',
    name: 'Developer',
    email: 'dev@example.com',
    password: '',
    enable: true,
    createTime: Date.now(),
    updateTime: Date.now(),
    language: 'zh-CN',
    lastOrganizationId: 'dev-org-id',
    phone: '',
    source: 'LOCAL',
    createUser: '',
    updateUser: '',
    platformInfo: '',
    avatar: '',
    permissionIds: ['*'],
    organizationIds: ['dev-org-id'],
    csrfToken: 'dev-mode-csrf-token',
    sessionId: 'dev-mode-session-id',
    roles: [{ id: 'admin', name: 'Administrator', dataScope: 'ALL' }],
    userId: 'dev-user-id',
    userName: 'Developer',
    departmentName: 'Development',
    departmentId: 'dev-dept-id',
    defaultPwd: false,
};
/**
 * Mock routes for dev mode
 * Provides basic routes when backend is not available
 */
export const MOCK_ROUTES = [
    {
        path: '/workbench',
        name: 'workbench',
        component: () => import('@/views/workbench/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/customer',
        name: 'customer',
        component: () => import('@/views/customer/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/clueManagement',
        name: 'clueManagement',
        component: () => import('@/views/clueManagement/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/opportunity',
        name: 'opportunity',
        component: () => import('@/views/opportunity/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/contract',
        name: 'contract',
        component: () => import('@/views/contract/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/order',
        name: 'order',
        component: () => import('@/views/order/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/product',
        name: 'product',
        component: () => import('@/views/product/index.vue'),
        meta: { requiresAuth: true },
    },
    {
        path: '/dashboard',
        name: 'dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { requiresAuth: true },
    },
];
/**
 * Mock version info for dev mode
 */
export const MOCK_VERSION_INFO = {
    currentVersion: '1.0.0-dev',
    releaseDate: new Date().toISOString().split('T')[0],
    latestVersion: '',
    architecture: 'standalone-dev',
    copyright: 'Dev Mode',
    hasNewVersion: false,
};
//# sourceMappingURL=mockData.js.map