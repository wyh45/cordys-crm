import { cloneDeep } from 'lodash-es';
import dayjs from 'dayjs';
import useTableStore from '@/hooks/useTableStore';
import useAppStore from '@/store/modules/app';
import useViewStore from '@/store/modules/view';
const tableStore = useTableStore();
const appStore = useAppStore();
const viewStore = useViewStore();
export default function useTable(loadListFunc, props, 
// 数据处理的回调函数
dataTransform) {
    const defaultProps = {
        bordered: false,
        loading: false, // 加载效果
        data: [], // 表格数据
        columns: [],
        tableRowKey: 'id', // 表格行的key
        showPagination: true, // 是否显示分页
        containerClass: '',
        ...props,
        crmPagination: {
            page: 1,
            itemCount: 0,
            pageSize: appStore.pageSize,
            showSizePicker: appStore.showSizePicker,
            showQuickJumper: appStore.showQuickJumper,
            ...props?.crmPagination,
        },
    };
    const propsRes = ref(cloneDeep(defaultProps));
    const filterItem = ref([]); // 筛选
    const keyword = ref('');
    const code = ref(0); // 状态码
    // 高级筛选
    const advanceFilter = reactive({ searchMode: 'AND', conditions: [] });
    // 如果表格设置了tableKey，设置缓存的分页大小
    if (propsRes.value.showPagination && propsRes.value.tableKey) {
        tableStore.getPageSize(propsRes.value.tableKey).then((res) => {
            if (propsRes.value.crmPagination && res) {
                propsRes.value.crmPagination.pageSize = res;
            }
        });
    }
    // 加载效果
    function setLoading(status) {
        propsRes.value.loading = status;
    }
    // 设置请求参数
    const loadListParams = ref({});
    function setLoadListParams(params) {
        loadListParams.value = params || {};
        if (propsRes.value.crmPagination) {
            propsRes.value.crmPagination.page = 1;
        }
    }
    // 获取分页参数
    async function getPaginationParams() {
        const { page, pageSize } = propsRes.value.crmPagination;
        if (propsRes.value.tableKey) {
            const cachedPageSize = await tableStore.getPageSize(propsRes.value.tableKey);
            return { current: page, pageSize: cachedPageSize || pageSize };
        }
        return { current: page, pageSize };
    }
    // 重置表头筛选
    function resetFilterParams() {
        filterItem.value = [];
    }
    // 设置 advanceFilter
    function setAdvanceFilter(v) {
        advanceFilter.searchMode = v.searchMode;
        advanceFilter.conditions = v.conditions;
        // 基础筛选都清空
        loadListParams.value.filter = {};
        loadListParams.value.keyword = '';
        keyword.value = '';
        resetFilterParams();
        if (propsRes.value.crmPagination) {
            propsRes.value.crmPagination.page = 1;
        }
    }
    /**
     * 分页设置
     * @param page 当前页
     * @param total 总页数
     */
    function setPagination(page, total) {
        if (propsRes.value.crmPagination) {
            propsRes.value.crmPagination.page = page;
            if (total !== undefined) {
                propsRes.value.crmPagination.itemCount = total;
            }
        }
    }
    const tableQueryParams = ref({}); // 表格请求参数集合
    const sortItem = ref(); // 排序
    const paginationType = ref(); // 分页类型
    async function getPaginationType(tableKey) {
        paginationType.value = await tableStore.getTablePaginationType(tableKey);
    }
    function processRecordItem(item, originalData) {
        if (item.updateTime) {
            item.updateTime = dayjs(item.updateTime).format('YYYY-MM-DD HH:mm:ss');
        }
        if (item.createTime) {
            item.createTime = dayjs(item.createTime).format('YYYY-MM-DD HH:mm:ss');
        }
        if (dataTransform) {
            item = dataTransform(item, originalData);
        }
        return item;
    }
    async function loadList(isPageChange = false, refreshId = undefined) {
        if (!loadListFunc || propsRes.value.loading)
            return;
        setLoading(true);
        await getPaginationType(propsRes.value.tableKey);
        try {
            tableQueryParams.value = {
                ...(!propsRes.value.showPagination ? {} : await getPaginationParams()),
                sort: sortItem.value,
                combineSearch: advanceFilter,
                ...loadListParams.value,
                filters: filterItem.value,
            };
            let refreshPage = 0;
            if (refreshId !== undefined) {
                refreshPage = Math.max(Math.ceil((propsRes.value.data.findIndex((item) => item.id === refreshId) + 1) / appStore.pageSize), 1);
            }
            const res = await loadListFunc({
                ...tableQueryParams.value,
                current: refreshId ? refreshPage : tableQueryParams.value.current,
            });
            if (props?.isReturnNativeResponse) {
                code.value = res.data.code;
            }
            const data = props?.isReturnNativeResponse ? res.data.data : res;
            if (!propsRes.value.showPagination && (Array.isArray(data.list) || Array.isArray(data))) {
                propsRes.value.data = (data.list || data).map((item) => processRecordItem(item, data));
                setPagination(1, (data.list || data).length);
            }
            else {
                const tmpArr = data;
                if (isPageChange && paginationType.value === 'scrollPagination') {
                    tmpArr.list?.forEach((item) => {
                        propsRes.value.data.push(processRecordItem(item, tmpArr));
                    });
                    setPagination(tmpArr.current, tmpArr.total);
                }
                else if (refreshId) {
                    const oldData = propsRes.value.data.find((item) => item.id === refreshId);
                    const newData = tmpArr.list?.find((item) => item.id === refreshId);
                    if (oldData && newData) {
                        Object.assign(oldData, processRecordItem(newData, tmpArr));
                    }
                }
                else {
                    propsRes.value.data = tmpArr.list?.map((item) => processRecordItem(item, tmpArr));
                    setPagination(tmpArr.current, tmpArr.total);
                }
            }
            if (paginationType.value === 'scrollPagination') {
                // 解决分页数据不足一页时不能触发滚动的问题
                nextTick(() => {
                    if ((data.list || data).length === 0 ||
                        (propsRes.value.crmPagination?.itemCount &&
                            propsRes.value.crmPagination?.page &&
                            propsRes.value.crmPagination?.pageSize &&
                            propsRes.value.crmPagination.itemCount <=
                                propsRes.value.crmPagination.page * propsRes.value.crmPagination.pageSize)) {
                        return;
                    }
                    if (propsRes.value.containerClass) {
                        const tableScrollElement = document.querySelector(propsRes.value.containerClass)?.querySelector('.v-vl');
                        const listElement = document.querySelector(propsRes.value.containerClass)?.querySelector('.v-vl-items');
                        if (tableScrollElement &&
                            listElement &&
                            tableScrollElement.clientHeight >= listElement.clientHeight &&
                            propsRes.value.crmPagination?.page) {
                            setPagination(propsRes.value.crmPagination.page + 1);
                            loadList(true);
                        }
                    }
                });
            }
        }
        catch (error) {
            propsRes.value.data = [];
            // eslint-disable-next-line no-console
            console.error(error);
            if (props?.isReturnNativeResponse) {
                code.value = error.code;
            }
            throw error;
        }
        finally {
            setLoading(false);
        }
    }
    // 事件触发组
    const propsEvent = ref({
        // 分页触发
        pageChange: async (page) => {
            setPagination(page);
            await loadList(true);
        },
        // 修改每页显示条数触发
        pageSizeChange: async (pageSize) => {
            if (propsRes.value.crmPagination && typeof propsRes.value.crmPagination === 'object') {
                propsRes.value.crmPagination.pageSize = pageSize;
                // 如果表格设置了tableKey，缓存分页大小
                if (propsRes.value.tableKey) {
                    await tableStore.setPageSize(propsRes.value.tableKey, pageSize);
                }
            }
            loadList();
        },
        // 排序触发
        sorterChange: async (sortObj) => {
            // 如果有视图，就存储表头排序
            if (propsRes.value.tableKey && loadListParams.value.viewId) {
                await viewStore.setViewSort(propsRes.value.tableKey, loadListParams.value.viewId, sortObj);
            }
            sortItem.value = sortObj;
            setPagination(1);
            loadList();
        },
        // 筛选触发
        filterChange: (filters) => {
            filterItem.value = filters.map((item) => ({
                ...item,
                multipleValue: item.multipleValue ?? filterItem.value.find((i) => i.name === item.name)?.multipleValue ?? false,
            }));
            setPagination(1);
            loadList();
        },
    });
    return {
        propsRes,
        propsEvent,
        tableQueryParams,
        setLoading,
        setLoadListParams,
        loadList,
        setPagination,
        advanceFilter,
        setAdvanceFilter,
        code,
        filterItem,
    };
}
//# sourceMappingURL=useTable.js.map