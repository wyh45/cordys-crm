import { AgentRouteEnum } from '@/enums/routeEnum';
import { DEFAULT_LAYOUT } from '../base';
const agent = {
    path: '/agent',
    name: AgentRouteEnum.AGENT,
    redirect: '/agent/index',
    component: DEFAULT_LAYOUT,
    meta: {
        locale: 'menu.agent',
        permissions: ['AGENT:READ'],
        icon: 'iconicon_bot',
        hideChildrenInMenu: true,
        collapsedLocale: 'menu.agent',
    },
    children: [
        {
            path: 'index',
            name: AgentRouteEnum.AGENT_INDEX,
            component: () => import('@/views/agent/index.vue'),
            meta: {
                locale: 'menu.agent',
                permissions: ['AGENT:READ'],
            },
        },
    ],
};
export default agent;
//# sourceMappingURL=agent.js.map