import { ref, watch } from 'vue';
import { getProductOptions } from '@/api/modules';
export const remoteFilterApiMap = {
    products: getProductOptions,
};
/**
 * Hook: 根据 filterApiKey 获取远程 filterOptions
 * @param filterApiKey string
 */
export default function useRemoteFilterOptions(filterApiKey) {
    const filterOptions = ref([]);
    async function fetchFilterOptions() {
        if (!filterApiKey)
            return;
        const api = remoteFilterApiMap[filterApiKey];
        if (!api) {
            return [];
        }
        try {
            const result = await api();
            filterOptions.value =
                (result || []).map((e) => ({
                    value: e.id,
                    label: e.name,
                })) ?? [];
        }
        catch (err) {
            // eslint-disable-next-line no-console
            console.error(err);
        }
    }
    watch(() => filterApiKey, fetchFilterOptions, { immediate: true });
    return {
        filterOptions,
        refresh: fetchFilterOptions,
    };
}
//# sourceMappingURL=useRemoteFilterOptions.js.map