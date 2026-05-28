import usePermission from '@/hooks/usePermission';
import useAppStore from '@/store/modules/app';
import { featureRouteMap, NO_RESOURCE_ROUTE_NAME, WHITE_LIST } from '../constants';
import NProgress from 'nprogress';
export default function setupPermissionGuard(router) {
    router.beforeEach(async (to, from, next) => {
        const Permission = usePermission();
        const permissionsAllow = Permission.accessRouter(to);
        const appStore = useAppStore();
        const currentMenuConfig = appStore.moduleConfigList.filter((e) => e.enable).map((e) => e.moduleKey);
        const moduleId = Object.keys(featureRouteMap).find((key) => to.name?.includes(key));
        if (moduleId && featureRouteMap[moduleId] && !currentMenuConfig.includes(featureRouteMap[moduleId])) {
            next({
                name: NO_RESOURCE_ROUTE_NAME,
            });
        }
        const exist = WHITE_LIST.find((el) => el.name === to.name);
        if (exist || permissionsAllow) {
            next();
        }
        else {
            next({
                name: NO_RESOURCE_ROUTE_NAME,
            });
        }
        NProgress.done();
    });
}
//# sourceMappingURL=permission.js.map