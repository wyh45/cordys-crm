import { defineStore } from 'pinia';
// 用于记录用户是否第一次访问某些功能，会持久化存储
const useVisitStore = defineStore('visit', {
    state: () => ({
        visitedKeys: [],
    }),
    actions: {
        addVisitedKey(key) {
            this.visitedKeys.push(key);
        },
        deleteVisitedKey(key) {
            this.visitedKeys = this.visitedKeys.filter((item) => item !== key);
        },
        getIsVisited(key) {
            return this.visitedKeys.includes(key);
        },
    },
    persist: {
        paths: ['visitedKeys'],
    },
});
export default useVisitStore;
//# sourceMappingURL=visit.js.map