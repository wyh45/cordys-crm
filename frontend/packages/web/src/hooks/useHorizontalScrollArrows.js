import { nextTick, onMounted, onUnmounted, ref } from 'vue';
export default function useHorizontalScrollArrows(containerRef) {
    const showArrows = ref(false);
    const canScrollLeft = ref(false);
    const canScrollRight = ref(false);
    const updateScrollStatus = () => {
        const el = containerRef.value;
        if (!el)
            return;
        const { scrollLeft, scrollWidth, clientWidth } = el;
        showArrows.value = scrollWidth > clientWidth;
        canScrollLeft.value = scrollLeft > 0;
        canScrollRight.value = scrollLeft + clientWidth < scrollWidth - 1;
    };
    const scrollLeft = () => {
        containerRef.value?.scrollBy({ left: -400, behavior: 'smooth' });
    };
    const scrollRight = () => {
        containerRef.value?.scrollBy({ left: 400, behavior: 'smooth' });
    };
    onMounted(() => {
        nextTick(() => {
            updateScrollStatus();
            containerRef.value?.addEventListener('scroll', updateScrollStatus);
        });
        window.addEventListener('resize', updateScrollStatus);
    });
    onUnmounted(() => {
        containerRef.value?.removeEventListener('scroll', updateScrollStatus);
        window.removeEventListener('resize', updateScrollStatus);
    });
    return {
        showArrows,
        canScrollLeft,
        canScrollRight,
        scrollLeft,
        scrollRight,
        updateScrollStatus,
    };
}
//# sourceMappingURL=useHorizontalScrollArrows.js.map