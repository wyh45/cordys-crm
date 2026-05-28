<template>
  <div class="exam-detail">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pb-[16px] pt-[20px]">
        <n-space align="center" class="header-bar">
          <n-button text @click="goBack">
            <template #icon
              ><n-icon><ArrowBackOutline /></n-icon
            ></template>
            {{ t('common.back') }}
          </n-button>
          <n-h3 style="margin: 0">{{ archive?.customerName || t('health.examDetail') }}</n-h3>
          <n-space>
            <n-button type="primary" :disabled="!exams.length" @click="handleAiInterpret">
              {{ t('health.aiInterpret') }}
            </n-button>
          </n-space>
        </n-space>

        <n-data-table
          :columns="columns"
          :data="exams"
          :loading="loading"
          :bordered="false"
          :single-line="false"
          size="small"
          style="margin-top: 16px"
        >
          <template #empty>
            <n-empty :description="t('health.noExamData')" />
          </template>
        </n-data-table>
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { h, ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { NButton, NDataTable, NEmpty, NH3, NIcon, NSpace, NTag, useMessage } from 'naive-ui';
  import { ArrowBackOutline } from '@vicons/ionicons5';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import type { HealthArchive, HealthExamination } from '@/api/modules';
  import { getHealthArchive, getHealthExaminations } from '@/api/modules';

  import { HealthRouteEnum } from '@/enums/routeEnum';

  const message = useMessage();
  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();

  const loading = ref(false);
  const archive = ref<HealthArchive | null>(null);
  const exams = ref<HealthExamination[]>([]);

  const columns = [
    { title: '体检号', key: 'examNo', width: 180, render: (row: HealthExamination) => row.examNo || '-' },
    { title: '检查项目', key: 'examItem', width: 140 },
    { title: '检查值', key: 'resultValue', width: 120 },
    { title: '参考范围', key: 'referenceRange', width: 160 },
    {
      title: '结果标记',
      key: 'resultFlag',
      width: 80,
      render: (row: HealthExamination) => {
        if (row.resultFlag === '↑') return h(NTag, { type: 'error', size: 'small' }, { default: () => '↑ 偏高' });
        if (row.resultFlag === '↓') return h(NTag, { type: 'info', size: 'small' }, { default: () => '↓ 偏低' });
        return '-';
      },
    },
    {
      title: '异常',
      key: 'isAbnormal',
      width: 80,
      render: (row: HealthExamination) =>
        row.isAbnormal
          ? h(NTag, { type: 'error', size: 'small' }, { default: () => '异常' })
          : h(NTag, { type: 'success', size: 'small' }, { default: () => '正常' }),
    },
    {
      title: '检查日期',
      key: 'examDate',
      width: 160,
      render: (row: HealthExamination) => (row.examDate ? new Date(row.examDate).toLocaleDateString() : '-'),
    },
  ];

  function goBack() {
    router.push({ name: HealthRouteEnum.HEALTH_EXAMINATION });
  }

  function handleAiInterpret() {
    router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: route.params.archiveId as string } });
  }

  async function fetchData() {
    loading.value = true;
    try {
      const archiveId = route.params.archiveId as string;
      const [archiveData, examData] = await Promise.all([
        getHealthArchive(archiveId),
        getHealthExaminations(archiveId),
      ]);
      archive.value = archiveData;
      exams.value = Array.isArray(examData) ? examData : [];
    } catch (error: any) {
      message.error(error?.message || t('common.loadFailed'));
    } finally {
      loading.value = false;
    }
  }

  fetchData();
</script>

<style lang="less" scoped>
  .exam-detail {
    height: 100%;
    overflow: auto;
  }
  .header-bar {
    display: flex;
    justify-content: space-between;
    width: 100%;
  }
</style>
