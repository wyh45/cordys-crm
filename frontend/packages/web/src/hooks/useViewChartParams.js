import { useRoute } from 'vue-router';
import { OperatorEnum } from '@lib/shared/enums/commonEnum';
import { FieldTypeEnum } from '@lib/shared/enums/formDesignEnum';
import { getSessionStorageTempState, setSessionStorageTempState } from '@lib/shared/method/local-storage';
import { multipleValueTypeList } from '@/components/business/crm-form-create/config';
export const STORAGE_VIEW_CHART_KEY = 'viewChartParams';
export default function useViewChartParams() {
    const route = useRoute();
    // 获取整个存储的参数
    function getAllChartParams() {
        return getSessionStorageTempState(STORAGE_VIEW_CHART_KEY, true);
    }
    // 存储图表参数对象
    /**
     * @param chartKey 图表url上携带的对应的cartKey,跳转之前生成的随机id  http://localhost:5173/#/opportunity/opt?chartKey=176104198889700000
     * @param data 图表参数
     */
    function setViewChartParams(chartKey, data) {
        sessionStorage.removeItem(STORAGE_VIEW_CHART_KEY);
        let allChartParams = getAllChartParams();
        if (!allChartParams) {
            allChartParams = {};
        }
        allChartParams[chartKey] = data;
        const chartData = {
            [chartKey]: data,
        };
        setSessionStorageTempState(STORAGE_VIEW_CHART_KEY, chartData);
    }
    // 获取对应chartKey参数
    function getChartKeyParams(chartKey) {
        const allConditionParams = getAllChartParams();
        if (allConditionParams) {
            return allConditionParams[chartKey] || null;
        }
        return null;
    }
    // 获取图表参数用于table列表回显
    function getChartConditions(chartKey) {
        const chartParams = getChartKeyParams(chartKey);
        if (chartParams) {
            const { viewId, formModel } = chartParams;
            const list = (formModel.list || []);
            const filterFormList = list.map((e) => {
                if (e.type === FieldTypeEnum.INPUT_NUMBER) {
                    return { ...e, value: e.operator === OperatorEnum.EMPTY ? '' : Number(e.value) };
                }
                return e;
            });
            const conditions = filterFormList.map((item) => ({
                value: item.value,
                operator: item.operator,
                name: item.dataIndex ?? '',
                multipleValue: multipleValueTypeList.includes(item.type),
                type: item.type,
            }));
            const searchMode = formModel.searchMode;
            return {
                viewId,
                formModel: {
                    searchMode,
                    list: filterFormList,
                },
                filterResult: {
                    searchMode,
                    conditions,
                },
            };
        }
        return null;
    }
    const isInitViewChartQuery = ref(true);
    function initTableViewChartParams(callback) {
        if (isInitViewChartQuery.value && route.query.chartKey) {
            const conditionParams = getChartConditions(route.query.chartKey);
            if (conditionParams) {
                callback?.(conditionParams);
            }
        }
        isInitViewChartQuery.value = false;
    }
    function getChartViewId() {
        if (route.query.chartKey && isInitViewChartQuery.value) {
            const conditionParams = getChartConditions(route.query.chartKey);
            return conditionParams?.viewId ?? '';
        }
    }
    return {
        setViewChartParams,
        getChartConditions,
        initTableViewChartParams,
        isInitViewChartQuery,
        getChartViewId,
    };
}
//# sourceMappingURL=useViewChartParams.js.map