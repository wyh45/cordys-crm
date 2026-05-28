<template>
  <div class="health-follow-table">
    <div v-if="tableInitError" style="padding: 40px; text-align: center; color: var(--text-color-3)">
      <p>表格初始化失败，请刷新页面重试</p>
      <p style="font-size: 12px; margin-top: 8px">{{ tableInitError }}</p>
    </div>
    <CrmTable
      v-else
      ref="crmTableRef"
      v-model:checked-row-keys="checkedRowKeys"
      v-bind="propsRes"
      :columns="tableColumns"
      :action-config="actionConfig"
      @page-change="propsEvent.pageChange"
      @page-size-change="propsEvent.pageSizeChange"
      @sorter-change="propsEvent.sorterChange"
      @filter-change="propsEvent.filterChange"
      @batch-action="handleBatchAction"
      @refresh="searchData"
    >
      <template #actionLeft>
        <div class="flex items-center gap-[12px]">
          <n-button v-permission="['HEALTH:ADD']" type="primary" @click="handleAdd">
            {{ t('common.add') }}
          </n-button>
          <n-button
            v-permission="['HEALTH:DELETE']"
            type="primary"
            ghost
            class="n-btn-outline-primary"
            :disabled="checkedRowKeys.length === 0"
            @click="handleBatchDelete"
          >
            {{ t('common.batchDelete') }}
          </n-button>
          <n-divider vertical />
        </div>
      </template>
      <template #actionRight>
        <CrmAdvanceFilter
          ref="tableAdvanceFilterRef"
          v-model:keyword="keyword"
          :custom-fields-config-list="customFieldsFilterConfig"
          :filter-config-list="filterConfigList"
          @adv-search="handleAdvSearch"
          @keyword-search="searchData"
        />
      </template>
    </CrmTable>

    <n-modal v-model:show="formCreateVisible" preset="card" title="添加随访记录" style="width:520px">
      <n-form ref="followFormRef" :model="followForm" label-placement="top">
        <n-form-item label="客户姓名" path="customerName">
          <n-input v-model:value="followForm.customerName" placeholder="请输入客户姓名" />
        </n-form-item>
        <n-form-item label="随访日期" path="followUpDate" required>
          <n-date-picker v-model:value="followForm.followUpDate" type="date" style="width:100%" />
        </n-form-item>
        <n-form-item label="随访方式" path="followUpType">
          <n-checkbox-group v-model:value="followForm.followUpTypes">
            <n-checkbox value="PHONE">电话随访</n-checkbox>
            <n-checkbox value="SMS">短信随访</n-checkbox>
          </n-checkbox-group>
        </n-form-item>
        <n-form-item label="症状" path="symptoms">
          <n-input v-model:value="followForm.symptoms" type="textarea" placeholder="请输入症状描述" />
        </n-form-item>
        <n-form-item label="诊断" path="diagnosis">
          <n-input v-model:value="followForm.diagnosis" placeholder="请输入诊断" />
        </n-form-item>
        <n-form-item label="处方" path="prescription">
          <n-input v-model:value="followForm.prescription" type="textarea" placeholder="请输入处方" />
        </n-form-item>
        <n-form-item label="下次随访日期" path="nextFollowUpDate">
          <n-date-picker v-model:value="followForm.nextFollowUpDate" type="date" style="width:100%" />
        </n-form-item>
        <n-form-item label="备注" path="remarks">
          <n-input v-model:value="followForm.remarks" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="formCreateVisible = false">取消</n-button>
          <n-button type="primary" :loading="formSaving" @click="handleFormSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, nextTick, reactive, ref } from 'vue';
  import {
    DataTableRowKey,
    NButton,
    NDatePicker,
    NDivider,
    NForm,
    NFormItem,
    NInput,
    NModal,
    NSelect,
    NSpace,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';

  import type { HealthFollowListItem, HealthPushListItem } from '@/api/modules';
  import {
    deleteHealthFollow,
    getHealthFollowList,
    getHealthPushByCustomer,
    markHealthFollowContacted,
    saveHealthFollow,
  } from '@/api/modules';
  import useFormCreateTable from '@/hooks/useFormCreateTable';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();

  const props = defineProps<{
    customerId?: string;
    readonly?: boolean;
  }>();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const filterConfigList = ref<FilterFormItem[]>([]);
  const contactingRows = ref<Set<string>>(new Set());
  const contactedCache = ref<Set<string>>(new Set());
  const pushHistoryCache = ref<Map<string, HealthPushListItem[]>>(new Map());

  const formCreateVisible = ref(false);
  const formSaving = ref(false);
  const followFormRef = ref<any>(null);
  const followForm = reactive({
    customerName: '',
    followUpDate: null as number | null,
    followUpTypes: ['PHONE'] as string[],
    symptoms: '',
    diagnosis: '',
    prescription: '',
    nextFollowUpDate: null as number | null,
    remarks: '',
  });
  const followTypeOptions = [
    { label: '电话随访', value: 'PHONE' },
    { label: '短信随访', value: 'SMS' },
    { label: '上门随访', value: 'VISIT' },
    { label: '其他', value: 'OTHER' },
  ];

  const tableInitError = ref('');
  const tableReady = ref(false);

  const propsRes = ref<any>({
    showSetting: false,
    hiddenTotal: false,
    hiddenAllScreen: false,
    hiddenRefresh: false,
    hiddenBackToTop: false,
    showPagination: true,
    crmPagination: { page: 1, pageSize: 20, itemCount: 0 },
    loading: false,
    data: [],
    customTotal: false,
  });
  const propsEvent = ref<any>({});
  let loadList = async () => {};
  let setAdvanceFilter = (_filter: any) => {};
  const customFieldsFilterConfig = ref([]);

  async function initTable() {
    try {
      const result = await useFormCreateTable({
        formKey: 'healthFollow' as any,
        containerClass: '.health-follow-table',
      });
      const res = result.useTableRes;
      propsRes.value = { ...propsRes.value, ...(res.propsRes || {}) };
      propsEvent.value = res.propsEvent || {};
      loadList = res.loadList || (async () => {});
      setAdvanceFilter = res.setAdvanceFilter || (() => {});
      customFieldsFilterConfig.value = result.customFieldsFilterConfig || [];
      tableReady.value = true;
      nextTick(() => searchData());
    } catch (err: any) {
      console.error('[healthFollowTable] useFormCreateTable failed:', err);
      tableInitError.value = err?.message || String(err);
    }
  }

  initTable();

  function handleContact(row: HealthFollowListItem) {
    const key = row.customerId || row.id;
    if (contactedCache.value.has(key)) return;

    contactingRows.value.add(key);
    const archiveId = row.customerId; // use customerId as archiveId for now
    markHealthFollowContacted(archiveId)
      .then(() => {
        contactedCache.value.add(key);
        Message.success(t('health.contacted'));
      })
      .catch((error) => {
        console.error(error);
        Message.error(error?.message || t('common.saveFailed'));
      })
      .finally(() => {
        contactingRows.value.delete(key);
        searchData();
      });
  }

  function loadPushHistory(row: HealthFollowListItem) {
    if (!row.customerId) return;
    if (pushHistoryCache.value.has(row.customerId)) return;
    getHealthPushByCustomer(row.customerId)
      .then((res: any) => {
        pushHistoryCache.value.set(row.customerId, (res || []) as HealthPushListItem[]);
      })
      .catch(() => {});
  }

  const tableColumns = ref([
    {
      type: 'selection' as const,
      fixed: 'left' as const,
      width: 46,
    },
    {
      title: t('health.followUpDate'),
      dataKey: 'followUpDate',
      width: 120,
    },
    {
      title: t('health.customerName'),
      dataKey: 'customerName',
      width: 120,
    },
    {
      title: t('health.followUpType'),
      dataKey: 'followUpType',
      width: 100,
    },
    {
      title: t('health.symptoms'),
      dataKey: 'symptoms',
      width: 160,
    },
    {
      title: t('health.diagnosis'),
      dataKey: 'diagnosis',
      width: 160,
    },
    {
      title: t('health.prescription'),
      dataKey: 'prescription',
      width: 160,
    },
    {
      title: t('health.nextFollowUpDate'),
      dataKey: 'nextFollowUpDate',
      width: 120,
    },
    {
      title: t('health.aiPushHistory'),
      key: 'pushHistory',
      width: 120,
      render: (row: HealthFollowListItem) => {
        loadPushHistory(row);
        const records = pushHistoryCache.value.get(row.customerId || '');
        if (!records || records.length === 0) return '-';
        return h('span', { style: 'color: var(--primary-color); font-size: 12px' }, t('health.aiPushed'));
      },
    },
    {
      title: t('health.contact'),
      key: 'contactAction',
      width: 120,
      render: (row: HealthFollowListItem) => {
        const key = row.customerId || row.id;
        const contacted = contactedCache.value.has(key);
        const contacting = contactingRows.value.has(key);
        const isPhone = row.followUpType === 'PHONE' || (row as any).followMethod === 'PHONE';
        const label = isPhone ? '已电话回访' : '已短信回访';
        return h(
          'button',
          {
            class: contacted ? 'contact-btn contacted' : 'contact-btn',
            disabled: contacted || contacting,
            onClick: () => handleContact(row),
          },
          contacting ? '...' : label
        );
      },
    },
    {
      title: t('health.remarks'),
      dataKey: 'remarks',
      width: 150,
    },
    {
      title: t('common.createTime'),
      dataKey: 'createTime',
      width: 150,
    },
  ] as any);

  const actionConfig: BatchActionConfig = {
    baseAction: [],
  };

  function handleAdd() {
    followForm.customerName = '';
    followForm.followUpDate = null;
    followForm.followUpTypes = ['PHONE'];
    followForm.symptoms = '';
    followForm.diagnosis = '';
    followForm.prescription = '';
    followForm.nextFollowUpDate = null;
    followForm.remarks = '';
    formCreateVisible.value = true;
  }

  async function handleFormSave() {
    formSaving.value = true;
    try {
      await saveHealthFollow({
        customerName: followForm.customerName || undefined,
        followUpDate: followForm.followUpDate ? String(followForm.followUpDate) : '',
        followUpType: followForm.followUpTypes.join(','),
        symptoms: followForm.symptoms || undefined,
        diagnosis: followForm.diagnosis || undefined,
        prescription: followForm.prescription || undefined,
        nextFollowUpDate: followForm.nextFollowUpDate ? String(followForm.nextFollowUpDate) : undefined,
        remarks: followForm.remarks || undefined,
      } as any);
      Message.success('添加成功');
      formCreateVisible.value = false;
      searchData();
    } catch (err: any) {
      Message.error(err?.message || '添加失败');
    } finally {
      formSaving.value = false;
    }
  }

  function handleDelete(row: HealthFollowListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.customerName || row.followUpDate }),
      content: t('health.deleteFollowConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthFollow(row.id);
          Message.success(t('common.deleteSuccess'));
          searchData();
        } catch (error) {
          console.error(error);
        }
      },
    });
  }

  function handleBatchDelete() {
    openModal({
      type: 'error',
      title: t('common.batchDeleteTitle', { number: checkedRowKeys.value.length }),
      content: t('health.batchDeleteFollowConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await Promise.all(checkedRowKeys.value.map((id) => deleteHealthFollow(id.toString())));
          Message.success(t('common.deleteSuccess'));
          checkedRowKeys.value = [];
          searchData();
        } catch (error) {
          console.error(error);
        }
      },
    });
  }

  async function searchData() {
    await loadList();
  }

  function handleAdvSearch(filter: FilterResult, isAdvancedMode: boolean, originalForm?: FilterForm) {
    setAdvanceFilter(filter);
    searchData();
  }

  function handleBatchAction(item: { key: string }) {
    switch (item.key) {
      case 'batchDelete':
        handleBatchDelete();
        break;
      default:
        break;
    }
  }

</script>

<style lang="less" scoped>
  .contact-btn {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 4px;
    border: 1px solid var(--primary-color);
    background: var(--primary-color);
    color: #fff;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover:not(:disabled) {
      opacity: 0.85;
    }

    &.contacted {
      background: transparent;
      color: var(--text-color-3);
      border-color: var(--divider-color);
      cursor: default;
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
</style>
