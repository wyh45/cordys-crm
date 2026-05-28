import { HealthRouteEnum } from '@/enums/routeEnum';

import { DEFAULT_LAYOUT } from '../base';
import type { AppRouteRecordRaw } from '../types';

const health: AppRouteRecordRaw = {
  path: '/health',
  name: HealthRouteEnum.HEALTH,
  component: DEFAULT_LAYOUT,
  meta: {
    locale: 'menu.health',
    permissions: ['HEALTH:READ'],
    icon: 'icon-icon_health',
    hideChildrenInMenu: true,
    collapsedLocale: 'menu.health',
  },
  children: [
    {
      path: 'archive',
      name: HealthRouteEnum.HEALTH_ARCHIVE,
      component: () => import('@/views/health/archive/index.vue'),
      meta: {
        locale: 'menu.healthArchive',
        isTopMenu: true,
        permissions: ['HEALTH:READ'],
      },
    },
    {
      path: 'examination',
      name: HealthRouteEnum.HEALTH_EXAMINATION,
      component: () => import('@/views/health/examination/index.vue'),
      meta: {
        locale: 'menu.healthExamination',
        isTopMenu: true,
        permissions: ['HEALTH:READ'],
      },
    },
    {
      path: 'examination/detail/:archiveId',
      name: HealthRouteEnum.HEALTH_EXAMINATION_DETAIL,
      component: () => import('@/views/health/examination/detail.vue'),
      meta: {
        permissions: ['HEALTH:READ'],
      },
    },
    {
      path: 'follow',
      name: HealthRouteEnum.HEALTH_FOLLOW,
      component: () => import('@/views/health/follow/index.vue'),
      meta: {
        locale: 'menu.healthFollow',
        isTopMenu: true,
        permissions: ['HEALTH:READ'],
      },
    },
    {
      path: 'knowledge',
      name: HealthRouteEnum.HEALTH_KNOWLEDGE,
      component: () => import('@/views/health/knowledge/index.vue'),
      meta: {
        locale: 'menu.healthKnowledge',
        isTopMenu: true,
        permissions: ['HEALTH:READ'],
      },
    },
    {
      path: 'ai',
      name: HealthRouteEnum.HEALTH_AI,
      component: () => import('@/views/health/ai/index.vue'),
      meta: {
        locale: 'menu.healthAi',
        isTopMenu: true,
        permissions: ['HEALTH:READ'],
      },
    },
  ],
};

export default health;
