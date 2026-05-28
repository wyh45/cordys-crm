<template>
  <div ref="chartRef" style="width: 100%; height: 260px; margin-bottom: 16px"></div>
</template>

<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
  import * as echarts from 'echarts';

  interface Assessment {
    diseaseName: string;
    riskLevel: string;
  }

  const props = defineProps<{ assessments: Assessment[] }>();
  const chartRef = ref<HTMLDivElement | null>(null);
  let instance: echarts.ECharts | null = null;

  const colors = ['#67c23a', '#85ce61', '#e6a23c', '#e6a23c', '#f56c6c', '#f56c6c', '#f5222d'];

  function render() {
    if (!chartRef.value || !props.assessments?.length) return;
    if (!instance) instance = echarts.init(chartRef.value);
    const data = props.assessments.map((a) => {
      const match = (a.riskLevel || '').match(/\d+/);
      return { name: a.diseaseName, level: parseInt(match?.[0] || '0', 10) };
    });
    instance.setOption(
      {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        grid: { left: 100, right: 40, top: 20, bottom: 40 },
        xAxis: { type: 'value', min: 0, max: 7, interval: 1, name: '风险等级' },
        yAxis: { type: 'category', data: data.map((d) => d.name), axisLabel: { fontSize: 13 } },
        series: [
          {
            type: 'bar',
            data: data.map((d) => ({
              value: d.level,
              itemStyle: { color: colors[d.level - 1] || '#ccc' },
            })),
            label: { show: true, position: 'right', formatter: (p: any) => `${data[p.dataIndex]?.level ?? ''}级` },
          },
        ],
      },
      true
    );
  }

  onMounted(() => render());
  watch(
    () => props.assessments,
    () => render(),
    { deep: true }
  );
  onBeforeUnmount(() => {
    instance?.dispose();
    instance = null;
  });
</script>
