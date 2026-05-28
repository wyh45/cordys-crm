<script setup lang="ts">
  import { computed } from 'vue';

  interface Props {
    /** 图标名称（如 iconicon_user） */
    name?: string;
    /** 图标颜色 */
    color?: string;
    /** 图标宽度 */
    width?: string;
    /** 图标高度 */
    height?: string;
    /** van-badge content */
    content?: number | string;
    /** van-badge dot 模式 */
    dot?: boolean;
    /** @deprecated 请使用 name，保留是为了向后兼容旧调用方 */
    type?: string;
  }
  const props = withDefaults(defineProps<Props>(), {
    name: '',
    color: '#000',
    width: '1rem',
    height: '1rem',
    dot: false,
  });
  const iconName = computed(() => `#${props.name || props.type || ''}`);
</script>

<template>
  <van-badge :content="props.content" :dot="props.dot">
    <svg class="c-icon" aria-hidden="true">
      <use :xlink:href="iconName" :fill="color" />
    </svg>
  </van-badge>
</template>

<style scoped lang="less">
  .c-icon {
    @apply relative;

    width: v-bind(width);
    height: v-bind(height);
    color: transparent; // 解决部分图标线条填充色问题
  }
</style>
