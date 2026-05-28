import { useUserStore, useVisitStore } from '@/store';
/**
 * 判断账户是否第一次访问
 * @param key 自定义 key
 * @param needTimeStamp  是否需要加上时间戳
 */
export default function useVisit(key, needTimeStamp = false) {
    const userStore = useUserStore();
    const visitStore = useVisitStore();
    const localKey = `${userStore.userInfo.id}-${key}-${needTimeStamp ? new Date().getTime() : ''}`;
    const addVisited = () => {
        visitStore.addVisitedKey(localKey);
    };
    const getIsVisited = () => visitStore.getIsVisited(localKey);
    return {
        addVisited,
        getIsVisited,
    };
}
//# sourceMappingURL=useVisit.js.map