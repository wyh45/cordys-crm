<template>
  <div class="health-examination">
    <CrmCard no-content-padding hide-footer>
      <div class="px-[16px] pb-[16px] pt-[20px]">
        <n-space align="center" :size="12" class="filter-bar">
          <n-date-picker
            v-model:value="dateRange"
            type="daterange"
            clearable
            style="width: 260px"
            :is-date-disabled="(ts) => ts > Date.now()"
            @update:value="fetchData"
          />
          <n-button type="primary" :loading="loading" @click="fetchData">{{ t('common.search') }}</n-button>
        </n-space>

        <n-data-table
          :columns="columns"
          :data="pagedCustomers"
          :loading="loading"
          :bordered="false"
          :single-line="false"
          size="small"
          :row-props="() => ({ style: 'cursor: pointer' })"
          @row-click="handleRowClick"
        >
          <template #empty>
            <n-empty description="暂无异常客户数据" />
          </template>
        </n-data-table>

        <div v-if="customers.length > 0" style="display: flex; justify-content: flex-end; margin-top: 16px">
          <n-pagination
            v-model:page="page"
            v-model:page-size="pageSize"
            :item-count="customers.length"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            show-quick-jumper
          />
        </div>
      </div>
    </CrmCard>

    <n-modal v-model:show="interpretModalVisible" preset="card" title="AI解读详情" style="max-width: 700px">
      <n-spin :show="interpretModalLoading">
        <div
          v-if="interpretModalData"
          style="max-height: 500px; overflow: auto; white-space: pre-wrap; font-size: 14px; line-height: 1.8"
        >
          <div style="margin-bottom: 12px; font-size: 13px; color: var(--text-color-2)">
            客户：{{ interpretModalData.customerName }} | 建议类型：{{ interpretModalData.type }}
          </div>
          <n-divider style="margin: 8px 0" />
          {{ interpretModalData.content }}
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="interpretModalVisible = false">关闭</n-button>
          <n-button type="primary" @click="copyInterpretContent">复制</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    NButton,
    NDataTable,
    NDatePicker,
    NDivider,
    NEmpty,
    NModal,
    NPagination,
    NSpace,
    NSpin,
    NTag,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';

  import type { CustomerAbnormalDetail } from '@/api/modules';
  import { batchInterpretationStatus, getHealthExamAbnormalCustomers, getInterpretationHistory } from '@/api/modules';

  import { HealthRouteEnum } from '@/enums/routeEnum';

  const message = useMessage();
  const { t } = useI18n();
  const router = useRouter();

  const loading = ref(false);
  const dateRange = ref<[number, number] | null>(null);
  const customers = ref<CustomerAbnormalDetail[]>([]);
  const interpretedTypesMap = ref<Record<string, string[]>>({});
  const interpretModalVisible = ref(false);
  const interpretModalLoading = ref(false);
  const interpretModalData = ref<{ customerName: string; type: string; content: string } | null>(null);
  const page = ref(1);
  const pageSize = ref(20);

  const pagedCustomers = computed(() => {
    const start = (page.value - 1) * pageSize.value;
    return customers.value.slice(start, start + pageSize.value);
  });

  function maskPhone(phone: string | undefined): string {
    if (!phone || phone.length < 7) return '-';
    return `${phone.substring(0, 3)}****${phone.substring(phone.length - 4)}`;
  }

  const columns = [
    {
      title: '客户姓名',
      key: 'customerName',
      width: 120,
      render: (row: CustomerAbnormalDetail) => row.customerName || '-',
    },
    { title: '档案编号', key: 'archiveNo', width: 140, render: (row: CustomerAbnormalDetail) => row.archiveNo || '-' },
    { title: '手机号', key: 'phone', width: 140, render: (row: CustomerAbnormalDetail) => maskPhone(row.phone) },
    {
      title: '同步状态',
      key: 'synced',
      width: 90,
      render: (row: CustomerAbnormalDetail) => {
        const count = row.syncedCount || 0;
        if (count > 0) return h(NTag, { type: 'success', size: 'small' }, { default: () => `已同步 ${count}条` });
        return '-';
      },
    },
    {
      title: 'AI已解读',
      key: 'interpreted',
      width: 160,
      render: (row: CustomerAbnormalDetail) => {
        const types = interpretedTypesMap.value[row.archiveId || ''] || [];
        if (types.length === 0) return '-';
        return h(
          NSpace,
          { size: 4, wrap: true },
          {
            default: () =>
              types.map((type: string) =>
                h(
                  NButton,
                  {
                    size: 'tiny',
                    type: 'success',
                    ghost: true,
                    onClick: (e: Event) => {
                      e.stopPropagation();
                      openInterpretModal(row, type);
                    },
                  },
                  { default: () => type }
                )
              ),
          }
        );
      },
    },
    {
      title: '异常指标',
      key: 'abnormalItemNames',
      width: 280,
      render: (row: CustomerAbnormalDetail) => {
        const items = row.abnormalItemNames || [];
        if (items.length === 0) return '-';
        return h(
          NSpace,
          { size: 4, wrap: true },
          {
            default: () =>
              items
                .slice(0, 5)
                .map((name: string) => h(NTag, { type: 'warning', size: 'small' }, { default: () => name })),
          }
        );
      },
    },
    { title: '异常数', key: 'abnormalItems', width: 80 },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (row: CustomerAbnormalDetail) =>
        h(
          NSpace,
          { size: 'small' },
          {
            default: () => [
              h(
                NButton,
                {
                  type: 'primary',
                  size: 'small',
                  disabled: !row.archiveId,
                  onClick: () => router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: row.archiveId } }),
                },
                { default: () => 'AI解读' }
              ),
              h(
                NButton,
                {
                  type: 'info',
                  size: 'small',
                  disabled: !row.archiveId,
                  onClick: () =>
                    router.push({
                      name: HealthRouteEnum.HEALTH_EXAMINATION_DETAIL,
                      params: { archiveId: row.archiveId },
                    }),
                },
                { default: () => '查看详情' }
              ),
            ],
          }
        ),
    },
  ];

  function handleRowClick(row: CustomerAbnormalDetail) {
    if (!row.archiveId) return;
    router.push({
      name: HealthRouteEnum.HEALTH_EXAMINATION_DETAIL,
      params: { archiveId: row.archiveId },
    });
  }

  async function openInterpretModal(row: CustomerAbnormalDetail, type: string) {
    interpretModalVisible.value = true;
    interpretModalLoading.value = true;
    interpretModalData.value = null;
    try {
      const all = await getInterpretationHistory({ page: 1, pageSize: 200 });
      const records = Array.isArray(all)
        ? all.filter((r: any) => r.archiveId === row.archiveId && r.suggestionType === type)
        : [];
      const match = records.length > 0 ? records[0] : null;
      interpretModalData.value = {
        customerName: row.customerName || '-',
        type,
        content: match?.interpretation || '暂无解读内容',
      };
    } catch {
      interpretModalData.value = { customerName: row.customerName || '-', type, content: '加载失败' };
    } finally {
      interpretModalLoading.value = false;
    }
  }

  async function copyInterpretContent() {
    if (!interpretModalData.value?.content) return;
    try {
      await navigator.clipboard.writeText(interpretModalData.value.content);
      message.success('复制成功');
    } catch {
      message.error('复制失败');
    }
  }

  async function fetchData() {
    page.value = 1;
    loading.value = true;
    try {
      const params: any = {};
      if (dateRange.value) {
        const [startDate, endDate] = dateRange.value;
        params.startDate = startDate;
        params.endDate = endDate + 86399999;
      }
      const res = await getHealthExamAbnormalCustomers(params);
      customers.value = Array.isArray(res) ? res : [];
      const archiveIds = customers.value.map((c) => c.archiveId).filter(Boolean) as string[];
      if (archiveIds.length > 0) {
        const status = await batchInterpretationStatus(archiveIds);
        interpretedTypesMap.value = status || {};
      }
    } catch (error: any) {
      message.error(error?.message || t('common.loadFailed'));
    } finally {
      loading.value = false;
    }
  }

  fetchData();
</script>

<style lang="less" scoped>
  .health-examination {
    height: 100%;
    overflow: auto;
  }
  .filter-bar {
    margin-bottom: 16px;
  }
</style>
