<template>
  <div class="health-archive-table flex h-full flex-col">
    <div class="table-toolbar">
      <n-space :size="12">
        <n-button type="primary" @click="handleAdd">{{ t('common.add') }}</n-button>
        <n-button
          type="primary"
          ghost
          class="n-btn-outline-primary"
          :disabled="checkedRowKeys.length === 0"
          @click="handleBatchDelete"
        >
          {{ t('common.batchDelete') }}
        </n-button>
      </n-space>
      <CrmAdvanceFilter
        ref="tableAdvanceFilterRef"
        v-model:keyword="keyword"
        :custom-fields-config-list="customFieldsFilterConfig"
        :filter-config-list="filterConfigList"
        @adv-search="handleAdvSearch"
        @keyword-search="searchData"
      />
    </div>
    <div class="min-h-0 flex-1">
      <CrmTable
        ref="crmTableRef"
        v-model:checked-row-keys="checkedRowKeys"
        :data="tableData"
        :loading="tableLoading"
        :columns="tableColumns"
        :action-config="actionConfig"
        :crm-pagination="crmPagination"
        bordered
        table-row-key="id"
        show-pagination
        pagination-type="pagePagination"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
        @batch-action="handleBatchAction"
        @refresh="searchData"
      />
    </div>

    <!-- Archive form modal -->
    <n-modal v-model:show="archiveFormVisible" preset="card" :title="archiveFormTitle" style="width: 640px">
      <n-form
        ref="archiveFormRef"
        :model="archiveForm"
        :rules="archiveFormRules"
        label-placement="left"
        label-width="100"
      >
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('health.customerName')" path="customerName">
              <n-input v-model:value="archiveForm.customerName" :placeholder="t('health.customerNamePlaceholder')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.phone')" path="phone">
              <n-input v-model:value="archiveForm.phone" :placeholder="t('health.phonePlaceholder')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.gender')" path="gender">
              <n-select
                v-model:value="archiveForm.gender"
                :options="genderOptions"
                :placeholder="t('health.genderPlaceholder')"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.age')" path="age">
              <n-input-number v-model:value="archiveForm.age" :min="0" :max="150" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.bloodType')" path="bloodType">
              <n-select
                v-model:value="archiveForm.bloodType"
                :options="bloodTypeOptions"
                :placeholder="t('health.bloodTypePlaceholder')"
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.idcardNo')" path="idcardNo">
              <n-input v-model:value="archiveForm.idcardNo" :placeholder="t('health.idcardNoPlaceholder')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.height')" path="height">
              <n-input-number v-model:value="archiveForm.height" :min="0" :max="300" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.weight')" path="weight">
              <n-input-number v-model:value="archiveForm.weight" :min="0" :max="500" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.bloodPressure')" path="bloodPressure">
              <n-input v-model:value="archiveForm.bloodPressure" :placeholder="t('health.bloodPressurePlaceholder')" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.heartRate')" path="heartRate">
              <n-input-number v-model:value="archiveForm.heartRate" :min="0" :max="300" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.bloodSugar')" path="bloodSugar">
              <n-input-number
                v-model:value="archiveForm.bloodSugar"
                :min="0"
                :max="50"
                :step="0.1"
                style="width: 100%"
                :placeholder="t('health.bloodSugarPlaceholder')"
              />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-divider />
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item :label="t('health.smoking')" path="smoking">
              <n-select
                v-model:value="archiveForm.smoking"
                :options="smokingOptions"
                :placeholder="t('health.lifestylePlaceholder')"
                clearable
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.drinking')" path="drinking">
              <n-select
                v-model:value="archiveForm.drinking"
                :options="drinkingOptions"
                :placeholder="t('health.lifestylePlaceholder')"
                clearable
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.sleepQuality')" path="sleepQuality">
              <n-select
                v-model:value="archiveForm.sleepQuality"
                :options="sleepQualityOptions"
                :placeholder="t('health.lifestylePlaceholder')"
                clearable
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.exercise')" path="exercise">
              <n-select
                v-model:value="archiveForm.exercise"
                :options="exerciseOptions"
                :placeholder="t('health.lifestylePlaceholder')"
                clearable
              />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item :label="t('health.diet')" path="diet">
              <n-select
                v-model:value="archiveForm.diet"
                :options="dietOptions"
                :placeholder="t('health.lifestylePlaceholder')"
                clearable
              />
            </n-form-item>
          </n-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="archiveFormVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="archiveFormSaving" @click="handleArchiveFormSave">
            {{ t('common.save') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal
      v-model:show="showSyncConfirmDialog"
      preset="card"
      :title="t('health.syncConfirmTitle')"
      :bordered="false"
      :segmented="false"
      style="width: 480px"
      :mask-closable="!syncLoading"
    >
      <template v-if="syncTargetRow">
        <n-spin :show="syncSearching">
          <template v-if="syncError">
            <n-alert type="error" :title="syncError" style="margin-bottom: 12px" />
          </template>
          <template v-else-if="syncMatchedCustomer">
            <n-alert type="success" style="margin-bottom: 12px">
              已匹配CRM客户：{{ syncMatchedCustomer.customerName }}
            </n-alert>
            <n-spin :show="syncCheckingNew" size="small">
              <template v-if="syncCheckingNew">
                <p style="color: var(--text-color-3); font-size: 13px">正在检测新增数据...</p>
              </template>
              <template v-else-if="syncError">
                <n-alert type="warning" :title="syncError" style="margin-bottom: 12px" />
              </template>
              <template v-else>
                <p style="margin-bottom: 12px">
                  {{ syncNewItems.length > 0 ? '检测到以下新增数据：' : '无新增数据可同步' }}
                </p>
                <n-space v-if="syncNewItems.length > 0" :size="4" wrap style="margin-bottom: 12px">
                  <n-tag v-for="item in syncNewItems" :key="item" type="info" size="small">{{ item }}</n-tag>
                </n-space>
              </template>
            </n-spin>
          </template>
        </n-spin>
      </template>
      <template #footer>
        <n-space justify="end">
          <n-button @click="closeSyncDialog">{{ t('common.cancel') }}</n-button>
          <n-button
            type="primary"
            :loading="syncLoading"
            :disabled="
              syncSearching || syncCheckingNew || !!syncError || !syncMatchedCustomer || syncNewItems.length === 0
            "
            @click="confirmSync"
          >
            {{ t('health.syncConfirmButton') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <HealthArchiveDetail
      v-model:visible="detailDrawerVisible"
      :archive-id="detailArchiveId"
      :initial-tab="detailInitialTab"
    />
  </div>
</template>

<script setup lang="ts">
  import { computed, h, reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    DataTableRowKey,
    NAlert,
    NButton,
    NDivider,
    NDropdown,
    NForm,
    NFormItem,
    NGi,
    NGrid,
    NInput,
    NInputNumber,
    NModal,
    NSelect,
    NSpace,
    NSpin,
    NTag,
    useMessage,
  } from 'naive-ui';

  import { SpecialColumnEnum } from '@lib/shared/enums/tableEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { validatePhone } from '@lib/shared/method/validate';

  import CrmAdvanceFilter from '@/components/pure/crm-advance-filter/index.vue';
  import { FilterForm, FilterFormItem, FilterResult } from '@/components/pure/crm-advance-filter/type';
  import CrmTable from '@/components/pure/crm-table/index.vue';
  import { BatchActionConfig } from '@/components/pure/crm-table/type';
  import HealthArchiveDetail from './healthArchiveDetail.vue';

  import type { HealthArchiveListItem } from '@/api/modules';
  import {
    addHealthArchive,
    deleteHealthArchive,
    getHealthArchive,
    getHealthArchiveList,
    getHealthExaminations,
    matchHealthArchiveCustomer,
    syncHealthArchive,
    updateHealthArchive,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  import { HealthRouteEnum } from '@/enums/routeEnum';

  const Message = useMessage();
  const { openModal } = useModal();
  const { t } = useI18n();
  const router = useRouter();

  const props = defineProps<{
    customerId?: string;
    readonly?: boolean;
  }>();

  const crmTableRef = ref<InstanceType<typeof CrmTable>>();
  const checkedRowKeys = ref<DataTableRowKey[]>([]);
  const keyword = ref('');
  const detailDrawerVisible = ref(false);
  const detailArchiveId = ref('');
  const detailInitialTab = ref('info');
  const showSyncConfirmDialog = ref(false);
  const syncSearching = ref(false);
  const syncLoading = ref(false);
  const syncTargetRow = ref<HealthArchiveListItem | null>(null);
  const syncMatchedCustomer = ref<{ customerId: string; customerName: string } | null>(null);
  const syncError = ref('');
  const syncNewItems = ref<string[]>([]);
  const syncCheckingNew = ref(false);
  const filterConfigList = ref<FilterFormItem[]>([]);

  // Archive form modal state
  const archiveFormVisible = ref(false);
  const archiveFormSaving = ref(false);
  const archiveFormRef = ref<any>(null);
  const isEditMode = ref(false);
  const editingArchiveId = ref('');
  const archiveForm = reactive({
    customerName: '',
    phone: '',
    gender: null as string | null,
    age: null as number | null,
    bloodType: null as string | null,
    idcardNo: '',
    height: null as number | null,
    weight: null as number | null,
    bloodPressure: '',
    heartRate: null as number | null,
    smoking: null as string | null,
    drinking: null as string | null,
    sleepQuality: null as string | null,
    exercise: null as string | null,
    diet: null as string | null,
    bloodSugar: null as number | null,
  });
  const archiveFormRules = {
    customerName: { required: true, message: () => t('health.customerNameRequired'), trigger: 'blur' },
    phone: [
      { required: true, message: () => t('health.phoneRequired'), trigger: ['input', 'blur'] },
      {
        validator: (_rule: unknown, value: string) => {
          if (!value) return true;
          return validatePhone(value) ? true : new Error(t('health.phoneInvalid'));
        },
        trigger: ['input', 'blur'],
      },
    ],
    gender: { required: true, message: () => t('health.genderRequired'), trigger: ['blur'] },
    age: { required: true, message: () => t('health.ageRequired'), trigger: ['input', 'blur'] },
  };
  const archiveFormTitle = computed(() => (isEditMode.value ? t('common.edit') : t('common.add')));
  const genderOptions = [
    { label: t('health.genderMale'), value: 'MALE' },
    { label: t('health.genderFemale'), value: 'FEMALE' },
  ];
  const bloodTypeOptions = ['A', 'B', 'AB', 'O'].map((v) => ({ label: v, value: v }));

  const smokingOptions = [
    { label: t('health.smokingNone'), value: 'NONE' },
    { label: t('health.smokingQuit'), value: 'QUIT' },
    { label: t('health.smokingOccasional'), value: 'OCCASIONAL' },
    { label: t('health.smokingRegular'), value: 'REGULAR' },
    { label: t('health.smokingHeavy'), value: 'HEAVY' },
  ];
  const drinkingOptions = [
    { label: t('health.drinkingNone'), value: 'NONE' },
    { label: t('health.drinkingQuit'), value: 'QUIT' },
    { label: t('health.drinkingOccasional'), value: 'OCCASIONAL' },
    { label: t('health.drinkingRegular'), value: 'REGULAR' },
    { label: t('health.drinkingHeavy'), value: 'HEAVY' },
  ];
  const sleepQualityOptions = [
    { label: t('health.sleepNever'), value: 'NEVER' },
    { label: t('health.sleepRarely'), value: 'RARELY' },
    { label: t('health.sleepSometimes'), value: 'SOMETIMES' },
    { label: t('health.sleepOften'), value: 'OFTEN' },
    { label: t('health.sleepAlways'), value: 'ALWAYS' },
  ];
  const exerciseOptions = [
    { label: t('health.exerciseNone'), value: 'NONE' },
    { label: t('health.exerciseOnceWeek'), value: 'ONCE_WEEK' },
    { label: t('health.exercise2_3Week'), value: '2_3_WEEK' },
    { label: t('health.exerciseDaily'), value: 'DAILY' },
  ];
  const dietOptions = [
    { label: t('health.dietLight'), value: 'LIGHT' },
    { label: t('health.dietBalanced'), value: 'BALANCED' },
    { label: t('health.dietOily'), value: 'OILY' },
    { label: t('health.dietIrregular'), value: 'IRREGULAR' },
  ];

  const tableLoading = ref(false);
  const tableData = ref<HealthArchiveListItem[]>([]);
  const syncedCounts = ref<Record<string, number>>({});
  const pagination = reactive({
    page: 1,
    pageSize: 20,
    itemCount: 0,
    showSizePicker: true,
    showQuickJumper: true,
  });
  const crmPagination = computed(() => ({ ...pagination }));
  const customFieldsFilterConfig = ref([]);

  function handlePageChange(page: number) {
    pagination.page = page;
    fetchData();
  }

  function handlePageSizeChange(pageSize: number) {
    pagination.pageSize = pageSize;
    pagination.page = 1;
    fetchData();
  }

  const tableColumns = ref([
    {
      type: SpecialColumnEnum.SELECTION,
      width: 46,
      fixed: 'left',
    },
    {
      title: t('health.archiveNo'),
      key: 'archiveNo',
      width: 120,
    },
    {
      title: t('health.customerName'),
      key: 'customerName',
      width: 150,
    },
    {
      title: t('health.bloodType'),
      key: 'bloodType',
      width: 100,
    },
    {
      title: t('health.allergies'),
      key: 'allergies',
      width: 150,
    },
    {
      title: t('health.pastMedicalHistory'),
      key: 'pastMedicalHistory',
      width: 200,
    },
    {
      title: t('health.familyHistory'),
      key: 'familyHistory',
      width: 200,
    },
    {
      title: t('health.height'),
      key: 'height',
      width: 80,
    },
    {
      title: t('health.weight'),
      key: 'weight',
      width: 80,
    },
    {
      title: t('health.bloodPressure'),
      key: 'bloodPressure',
      width: 120,
    },
    {
      title: t('health.heartRate'),
      key: 'heartRate',
      width: 80,
    },
    {
      title: t('health.syncStatus'),
      key: 'syncedCount',
      width: 120,
      render: (row: HealthArchiveListItem) => {
        const count = syncedCounts.value[row.id] || 0;
        if (count > 0) {
          return h(NTag, { type: 'success', size: 'small' }, { default: () => `已同步 ${count}条` });
        }
        return '-';
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 160,
    },
    {
      title: t('common.action'),
      key: 'operation',
      width: 200,
      fixed: 'right',
      render: (row: HealthArchiveListItem) => {
        const synced = (syncedCounts.value[row.id] || 0) > 0;
        const moreOptions = [
          {
            label: t('health.addExamRecord'),
            key: 'addExam',
          },
          {
            label: synced ? `已同步 ${syncedCounts.value[row.id]}条` : t('health.syncToExam'),
            key: 'syncToExam',
          },
          { label: t('common.delete'), key: 'delete' },
        ];
        return h(
          NSpace,
          { size: 'small' },
          {
            default: () => [
              h(
                NButton,
                { size: 'small', type: 'primary', onClick: () => handleOperationSelect(row, 'detail') },
                { default: () => t('health.viewDetail') }
              ),
              h(
                NButton,
                { size: 'small', onClick: () => handleOperationSelect(row, 'edit') },
                { default: () => t('common.edit') }
              ),
              h(
                NDropdown,
                {
                  trigger: 'click',
                  options: moreOptions,
                  onSelect: (key: string) => handleOperationSelect(row, key),
                },
                {
                  default: () => h(NButton, { size: 'small' }, { default: () => t('common.more') }),
                }
              ),
            ],
          }
        );
      },
    },
  ] as any);

  const actionConfig: BatchActionConfig = {
    baseAction: [],
  };

  function resetArchiveForm() {
    archiveForm.customerName = '';
    archiveForm.phone = '';
    archiveForm.gender = null;
    archiveForm.age = null;
    archiveForm.bloodType = null;
    archiveForm.idcardNo = '';
    archiveForm.height = null;
    archiveForm.weight = null;
    archiveForm.bloodPressure = '';
    archiveForm.heartRate = null;
    archiveForm.smoking = null;
    archiveForm.drinking = null;
    archiveForm.sleepQuality = null;
    archiveForm.exercise = null;
    archiveForm.diet = null;
    archiveForm.bloodSugar = null;
    isEditMode.value = false;
    editingArchiveId.value = '';
  }

  function handleAdd() {
    resetArchiveForm();
    archiveFormVisible.value = true;
  }

  async function handleEdit(row: HealthArchiveListItem) {
    resetArchiveForm();
    isEditMode.value = true;
    editingArchiveId.value = row.id;
    try {
      const detail = (await getHealthArchive(row.id)) as any;
      if (detail) {
        archiveForm.customerName = detail.customerName || '';
        archiveForm.phone = detail.phone || '';
        // normalize gender from backend (stores Chinese) to form value (MALE/FEMALE)
        const rawGender = detail.gender;
        if (rawGender === '男') {
          archiveForm.gender = 'MALE';
        } else if (rawGender === '女') {
          archiveForm.gender = 'FEMALE';
        } else {
          archiveForm.gender = rawGender || null;
        }
        archiveForm.age = detail.age ?? null;
        archiveForm.bloodType = detail.bloodType || null;
        archiveForm.idcardNo = detail.idcardNo || '';
        archiveForm.height = detail.height ?? null;
        archiveForm.weight = detail.weight ?? null;
        archiveForm.bloodPressure = detail.bloodPressure || '';
        archiveForm.heartRate = detail.heartRate ?? null;
        archiveForm.bloodSugar = detail.bloodSugar ?? null;
        archiveForm.smoking = detail.smoking || null;
        archiveForm.drinking = detail.drinking || null;
        archiveForm.sleepQuality = detail.sleepQuality || null;
        archiveForm.exercise = detail.exercise || null;
        archiveForm.diet = detail.diet || null;
      }
    } catch {
      // use table row data as fallback
    }
    archiveFormVisible.value = true;
  }

  async function handleArchiveFormSave() {
    try {
      await archiveFormRef.value?.validate();
    } catch {
      return;
    }
    archiveFormSaving.value = true;
    try {
      const data: any = {
        customerName: archiveForm.customerName,
        phone: archiveForm.phone,
        gender: archiveForm.gender,
        age: archiveForm.age,
        bloodType: archiveForm.bloodType,
        idcardNo: archiveForm.idcardNo,
        height: archiveForm.height,
        weight: archiveForm.weight,
        bloodPressure: archiveForm.bloodPressure,
        heartRate: archiveForm.heartRate,
        bloodSugar: archiveForm.bloodSugar,
        smoking: archiveForm.smoking,
        drinking: archiveForm.drinking,
        sleepQuality: archiveForm.sleepQuality,
        exercise: archiveForm.exercise,
        diet: archiveForm.diet,
      };
      if (isEditMode.value) {
        data.id = editingArchiveId.value;
        await updateHealthArchive(data);
      } else {
        await addHealthArchive(data);
      }
      Message.success(t('common.saveSuccess'));
      archiveFormVisible.value = false;
      resetArchiveForm();
      searchData();
    } catch (error: any) {
      Message.error(error?.message || t('common.saveFailed'));
    } finally {
      archiveFormSaving.value = false;
    }
  }

  function handleDelete(row: HealthArchiveListItem) {
    openModal({
      type: 'error',
      title: t('common.deleteConfirmTitle', { name: row.customerName || row.archiveNo }),
      content: t('health.deleteArchiveConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthArchive(row.id);
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
      content: t('health.batchDeleteArchiveConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await Promise.all(checkedRowKeys.value.map((id) => deleteHealthArchive(id.toString())));
          Message.success(t('common.deleteSuccess'));
          checkedRowKeys.value = [];
          searchData();
        } catch (error) {
          console.error(error);
        }
      },
    });
  }

  async function handleSync(row: HealthArchiveListItem) {
    syncTargetRow.value = row;
    syncMatchedCustomer.value = null;
    syncError.value = '';
    syncNewItems.value = [];
    syncSearching.value = true;
    syncCheckingNew.value = false;
    showSyncConfirmDialog.value = true;

    try {
      const matchResult: any = await matchHealthArchiveCustomer(row.id);
      if (matchResult?.matched) {
        syncMatchedCustomer.value = {
          customerId: matchResult.customerId,
          customerName: matchResult.customerName,
        };
        await checkNewSyncData(row.id);
      } else {
        syncError.value = matchResult?.reason || t('health.syncFailNoMatch');
      }
    } catch {
      syncError.value = t('health.syncFailNoMatch');
    } finally {
      syncSearching.value = false;
    }
  }

  const SYNC_FIELD_MAP: Record<string, string> = {
    height: '身高',
    weight: '体重',
    bloodPressure: '血压',
    heartRate: '心率',
    bloodSugar: '血糖',
    smoking: '吸烟',
    drinking: '饮酒',
    sleepQuality: '熬夜',
    exercise: '运动频率',
    diet: '饮食习惯',
  };

  async function checkNewSyncData(archiveId: string) {
    syncCheckingNew.value = true;
    syncNewItems.value = [];
    try {
      const [archive, exams] = await Promise.all([
        getHealthArchive(archiveId).catch(() => null),
        getHealthExaminations(archiveId).catch(() => []),
      ]);
      if (!archive) return;
      const syncedItems = new Set(
        (Array.isArray(exams) ? exams : []).filter((e: any) => e.examNo?.startsWith('SYNC')).map((e: any) => e.examItem)
      );
      Object.entries(SYNC_FIELD_MAP).forEach(([field, label]) => {
        const val = (archive as any)[field];
        if (val != null && val !== '' && !syncedItems.has(label)) {
          syncNewItems.value.push(label);
        }
      });
    } catch (e) {
      console.error('[healthArchive] checkNewSyncData error:', e);
    } finally {
      syncCheckingNew.value = false;
    }
  }

  function closeSyncDialog() {
    if (syncLoading.value || syncSearching.value || syncCheckingNew.value) return;
    showSyncConfirmDialog.value = false;
    syncError.value = '';
    syncMatchedCustomer.value = null;
    syncNewItems.value = [];
  }

  async function confirmSync() {
    if (!syncTargetRow.value || !syncMatchedCustomer.value || syncNewItems.value.length === 0) return;
    syncLoading.value = true;
    try {
      const result: any = await syncHealthArchive(syncTargetRow.value.id);
      if (result?.success) {
        showSyncConfirmDialog.value = false;
        Message.success(t('health.syncSuccess', { count: result?.createdCount ?? 0 }));
        await fetchData();
      } else {
        syncError.value = result?.reason || t('health.syncFailNoMatch');
      }
    } catch {
      syncError.value = t('health.syncFailNoMatch');
    } finally {
      syncLoading.value = false;
    }
  }

  async function handleOperationSelect(row: HealthArchiveListItem, key: string) {
    switch (key) {
      case 'detail':
        detailArchiveId.value = row.id;
        detailInitialTab.value = 'info';
        detailDrawerVisible.value = true;
        break;
      case 'addExam':
        detailArchiveId.value = row.id;
        detailInitialTab.value = 'examination';
        detailDrawerVisible.value = true;
        break;
      case 'edit':
        handleEdit(row);
        break;
      case 'delete':
        handleDelete(row);
        break;
      case 'syncToExam':
        await handleSync(row);
        break;
      default:
        break;
    }
  }

  async function fetchData() {
    if (tableLoading.value) return;
    tableLoading.value = true;
    try {
      const res = (await getHealthArchiveList({
        page: pagination.page,
        pageSize: pagination.pageSize,
      })) as any;
      tableData.value = res.list || [];
      syncedCounts.value = res.syncedCounts || {};
      pagination.itemCount = res.total || 0;
    } catch (e) {
      console.error('[healthArchive] fetchData error:', e);
    } finally {
      tableLoading.value = false;
    }
  }

  async function searchData() {
    pagination.page = 1;
    await fetchData();
  }

  function handleAdvSearch(_filter: FilterResult, _isAdvancedMode: boolean, _originalForm?: FilterForm) {
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

  fetchData();
</script>

<style lang="less" scoped>
  .table-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
</style>
