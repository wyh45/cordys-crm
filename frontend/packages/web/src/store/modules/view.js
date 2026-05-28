import { defineStore } from 'pinia';
import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
import { FieldTypeEnum, FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
import { internalConditionsMap, viewApiMap } from '@/components/business/crm-view-select/config';
import { getOpportunityStageConfig } from '@/api/modules';
import useLocalForage from '@/hooks/useLocalForage';
import useUserStore from '@/store/modules/user';
const typeToKeyMap = {
    [FieldTypeEnum.DATA_SOURCE]: 'selectedRows',
    [FieldTypeEnum.DATA_SOURCE_MULTIPLE]: 'selectedRows',
    [FieldTypeEnum.DEPARTMENT]: 'selectedUserList',
    [FieldTypeEnum.DEPARTMENT_MULTIPLE]: 'selectedUserList',
    [FieldTypeEnum.MEMBER]: 'selectedUserList',
    [FieldTypeEnum.MEMBER_MULTIPLE]: 'selectedUserList',
};
// 视图
const useViewStore = defineStore('view', {
    persist: true,
    state: () => ({
        customViews: [],
        internalViews: [],
    }),
    actions: {
        getInternalKey(type) {
            return `internal_views_${type}`;
        },
        async getInternalViews(key) {
            const { getItem } = useLocalForage();
            const data = await getItem(key);
            return data ?? [];
        },
        async setInternalViews(key, data) {
            const { setItem } = useLocalForage();
            await setItem(key, data);
        },
        async getActiveView(key) {
            const { getItem } = useLocalForage();
            const data = await getItem(`active-view-${key}`);
            return data;
        },
        async setActiveView(key, viewId) {
            const { setItem } = useLocalForage();
            await setItem(`active-view-${key}`, viewId);
        },
        async getViewSort(tableKey, viewId) {
            const { getItem } = useLocalForage();
            const data = await getItem(`view-sort-${tableKey}-${viewId}`);
            return data;
        },
        async setViewSort(tableKey, viewId, data) {
            const { setItem } = useLocalForage();
            await setItem(`view-sort-${tableKey}-${viewId}`, data);
        },
        async getOpportunitySuccessStageOption() {
            try {
                const stageConfig = await getOpportunityStageConfig();
                const successStage = stageConfig?.stageConfigList?.find((i) => i.type === 'END' && i.rate === '100');
                return [
                    {
                        dataIndex: 'stage',
                        type: FieldTypeEnum.SELECT_MULTIPLE,
                        operator: OperatorEnum.IN,
                        value: [successStage?.id],
                    },
                ];
            }
            catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
                return [
                    {
                        dataIndex: 'stage',
                        type: FieldTypeEnum.SELECT_MULTIPLE,
                        operator: OperatorEnum.IN,
                        value: [],
                    },
                ];
            }
        },
        async resolveTypeOptions(type) {
            switch (type) {
                case FormDesignKeyEnum.BUSINESS:
                    return {
                        opportunitySuccessOptions: await this.getOpportunitySuccessStageOption(),
                    };
                default:
                    return {};
            }
        },
        resolveInternalConditionList(params) {
            const { type, tabName, optionItem } = params;
            // 商机成功阶段
            if (tabName === OpportunitySearchTypeEnum.OPPORTUNITY_SUCCESS) {
                return optionItem.opportunitySuccessOptions ?? [];
            }
            // 报价单我的视图
            if (type === FormDesignKeyEnum.OPPORTUNITY_QUOTATION && tabName === CustomerSearchTypeEnum.SELF) {
                return internalConditionsMap[CustomerSearchTypeEnum.SELF_QUOTATION] ?? [];
            }
            return internalConditionsMap[tabName] ?? [];
        },
        async loadInternalViews(type, internalList) {
            const userStore = useUserStore();
            const stored = await this.getInternalViews(this.getInternalKey(type));
            const listMap = internalList.reduce((map, item) => {
                map[item.name] = item;
                return map;
            }, {});
            const storedIds = new Set(stored.map((i) => i.id));
            const optItem = (await this.resolveTypeOptions(type)) ?? null;
            const merged = [
                // 优先使用 stored 的顺序和设置
                ...stored
                    .filter((item) => listMap[item.id])
                    .map((item) => {
                    const def = listMap[item.id];
                    return {
                        ...item,
                        name: def.tab,
                        list: this.resolveInternalConditionList({
                            type,
                            tabName: def.name,
                            optionItem: optItem,
                        }),
                        type: 'internal',
                        searchMode: item.searchMode ?? 'AND',
                    };
                }),
                // 添加 stored 中没有的新项
                ...internalList
                    .filter((item) => !storedIds.has(item.name))
                    .map((item) => ({
                    id: item.name,
                    name: item.tab,
                    enable: true,
                    fixed: true,
                    type: 'internal',
                    searchMode: 'AND',
                    list: this.resolveInternalConditionList({
                        type,
                        tabName: item.name,
                        optionItem: optItem,
                    }),
                })),
            ];
            // admin 不显示部门视图
            if (userStore.userInfo.id === 'admin') {
                this.internalViews = merged.filter((item) => item.id !== CustomerSearchTypeEnum.DEPARTMENT);
            }
            else {
                this.internalViews = merged;
            }
            await this.setInternalViews(this.getInternalKey(type), this.internalViews);
        },
        async loadCustomViews(type) {
            try {
                this.customViews = await viewApiMap.list[type]();
            }
            catch (error) {
                // eslint-disable-next-line no-console
                console.log(error);
            }
        },
        // 获取详情
        async getViewDetail(type, option) {
            let res;
            if (option.type === 'internal') {
                res = option;
            }
            else {
                try {
                    res = await viewApiMap.detail[type](option.id);
                    res.list = (res.conditions ?? []).map((item) => {
                        const options = res.optionMap?.[item.name]?.filter((i) => item.value?.includes(i.id)) ?? [];
                        const keyText = typeToKeyMap[item.type];
                        return {
                            ...item,
                            dataIndex: item.name,
                            ...(keyText ? { [keyText]: options } : {}),
                        };
                    });
                }
                catch (error) {
                    // eslint-disable-next-line no-console
                    console.log(error);
                    return {};
                }
            }
            return {
                name: res.name,
                list: res.list,
                searchMode: res.searchMode,
            };
        },
        // 拖拽
        async toggleDrag(type, params) {
            const isInternal = this.internalViews.some((item) => item.id === params.moveId);
            if (isInternal) {
                const movedItem = this.internalViews.splice(params.oldIndex, 1)[0];
                this.internalViews.splice(params.newIndex, 0, movedItem);
                await this.setInternalViews(this.getInternalKey(type), this.internalViews);
            }
            else {
                try {
                    await viewApiMap.drag[type](params);
                    this.loadCustomViews(type);
                }
                catch (error) {
                    // eslint-disable-next-line no-console
                    console.log(error);
                }
            }
        },
        // 固定/取消固定
        async toggleFixed(type, option) {
            if (option.type === 'internal') {
                const index = this.internalViews.findIndex((i) => i.id === option.id);
                if (index !== -1)
                    this.internalViews[index].fixed = !this.internalViews[index].fixed;
                this.setInternalViews(this.getInternalKey(type), this.internalViews);
            }
            else {
                try {
                    await viewApiMap.fixed[type](option.id);
                    this.loadCustomViews(type);
                }
                catch (error) {
                    // eslint-disable-next-line no-console
                    console.log(error);
                }
            }
        },
        async toggleEnabled(type, option) {
            if (option.type === 'internal') {
                const index = this.internalViews.findIndex((i) => i.id === option.id);
                if (index !== -1)
                    this.internalViews[index].enable = !this.internalViews[index].enable;
                this.setInternalViews(this.getInternalKey(type), this.internalViews);
            }
            else {
                try {
                    await viewApiMap.enable[type](option.id);
                }
                catch (error) {
                    // eslint-disable-next-line no-console
                    console.log(error);
                }
            }
        },
    },
});
export default useViewStore;
//# sourceMappingURL=view.js.map