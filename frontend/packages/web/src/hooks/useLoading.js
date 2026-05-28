import { ref } from 'vue';
/**
 * loading显示隐藏
 * @param initValue 初始化值
 * @returns 调用方法
 */
export default function useLoading(initValue = false) {
    const loading = ref(initValue);
    const setLoading = (value) => {
        loading.value = value;
    };
    const toggle = () => {
        loading.value = !loading.value;
    };
    return {
        loading,
        setLoading,
        toggle,
    };
}
//# sourceMappingURL=useLoading.js.map