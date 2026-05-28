<template>
  <div class="health-archive-detail-wrapper">
    <n-drawer v-model:show="showDrawer" :width="800" :mask-closable="false">
      <n-drawer-content :title="t('health.archiveDetail')" closable>
        <n-tabs v-model:value="activeTab" type="line" class="detail-tabs">
          <!-- 基本信息 Tab -->
          <n-tab-pane name="info" :tab="t('health.basicInfo')">
            <n-spin :show="archiveLoading">
              <n-descriptions :column="2" bordered size="small" label-placement="left">
                <n-descriptions-item :label="t('health.archiveNo')">
                  {{ archiveData?.archiveNo || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.customerName')">
                  {{ archiveData?.customerName || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.bloodType')">
                  {{ archiveData?.bloodType || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.phone')">
                  {{ archiveData?.phone || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.gender')">
                  {{ genderLabel }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.age')">
                  {{ archiveData?.age ?? '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.allergies')">
                  <n-tag v-if="archiveData?.allergies" size="small" type="warning">
                    {{ archiveData.allergies }}
                  </n-tag>
                  <span v-else>-</span>
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.pastMedicalHistory')" :span="2">
                  {{ archiveData?.pastMedicalHistory || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('health.familyHistory')" :span="2">
                  {{ archiveData?.familyHistory || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('common.createTime')">
                  {{ archiveData?.createTime || '-' }}
                </n-descriptions-item>
                <n-descriptions-item :label="t('common.updateTime')">
                  {{ archiveData?.updateTime || '-' }}
                </n-descriptions-item>
              </n-descriptions>
            </n-spin>
          </n-tab-pane>

          <!-- 生活习惯 Tab -->
          <n-tab-pane name="lifestyle" :tab="t('health.lifestyle')">
            <n-spin :show="archiveLoading">
              <n-form :model="lifestyleForm" label-placement="left" label-width="100" class="lifestyle-form">
                <n-form-item :label="t('health.smoking')">
                  <n-select v-model:value="lifestyleForm.smoking" :options="smokingOptions" :placeholder="t('health.lifestylePlaceholder')" />
                </n-form-item>
                <n-form-item :label="t('health.drinking')">
                  <n-select v-model:value="lifestyleForm.drinking" :options="drinkingOptions" :placeholder="t('health.lifestylePlaceholder')" />
                </n-form-item>
                <n-form-item :label="t('health.sleepQuality')">
                  <n-select v-model:value="lifestyleForm.sleepQuality" :options="sleepOptions" :placeholder="t('health.lifestylePlaceholder')" />
                </n-form-item>
                <n-form-item :label="t('health.exercise')">
                  <n-select v-model:value="lifestyleForm.exercise" :options="exerciseOptions" :placeholder="t('health.lifestylePlaceholder')" />
                </n-form-item>
                <n-form-item :label="t('health.diet')">
                  <n-select v-model:value="lifestyleForm.diet" :options="dietOptions" :placeholder="t('health.lifestylePlaceholder')" />
                </n-form-item>
              </n-form>
              <n-button type="primary" :loading="lifestyleSaving" @click="handleSaveLifestyle">{{ t('common.save') }}</n-button>
            </n-spin>
          </n-tab-pane>

          <!-- 健康检测数据 Tab -->
          <n-tab-pane name="healthData" :tab="t('health.healthDataTab')">
            <n-spin :show="archiveLoading">
              <n-form :model="healthDataForm" label-placement="left" label-width="120" class="health-data-form">
                <n-form-item :label="t('health.height')">
                  <n-input-number v-model:value="healthDataForm.height" :min="0" :max="300" style="width: 100%" />
                </n-form-item>
                <n-form-item :label="t('health.weight')">
                  <n-input-number v-model:value="healthDataForm.weight" :min="0" :max="500" style="width: 100%" />
                </n-form-item>
                <n-form-item :label="t('health.bloodPressure')">
                  <n-input v-model:value="healthDataForm.bloodPressure" :placeholder="t('health.bloodPressurePlaceholder')" />
                </n-form-item>
                <n-form-item :label="t('health.heartRate')">
                  <n-input-number v-model:value="healthDataForm.heartRate" :min="0" :max="300" style="width: 100%" />
                </n-form-item>
                <n-form-item :label="t('health.bloodSugar')">
                  <n-input-number v-model:value="healthDataForm.bloodSugar" :min="0" :step="0.1" style="width: 100%" />
                </n-form-item>
              </n-form>
              <n-button type="primary" :loading="healthDataSaving" @click="handleSaveHealthData">{{ t('common.save') }}</n-button>
            </n-spin>
          </n-tab-pane>

          <!-- 过敏史 Tab -->
          <n-tab-pane name="allergy" :tab="t('health.allergyHistory')">
            <div class="sub-table-toolbar">
              <n-button type="primary" size="small" @click="handleAddAllergy">
                {{ t('common.add') }}
              </n-button>
            </div>
            <n-data-table
              :columns="allergyColumns"
              :data="allergyList"
              :loading="allergyLoading"
              :pagination="false"
              :row-key="(row: HealthAllergy) => row.id"
              size="small"
            />
            <div v-if="allergyList.length === 0 && !allergyLoading" class="empty-tip">
              {{ t('common.noData') }}
            </div>
          </n-tab-pane>

          <!-- 病史 Tab -->
          <n-tab-pane name="history" :tab="t('health.medicalHistory')">
            <div class="sub-table-toolbar">
              <n-button type="primary" size="small" @click="handleAddHistory">
                {{ t('common.add') }}
              </n-button>
            </div>
            <n-data-table
              :columns="historyColumns"
              :data="historyList"
              :loading="historyLoading"
              :pagination="false"
              :row-key="(row: HealthMedicalHistory) => row.id"
              size="small"
            />
            <div v-if="historyList.length === 0 && !historyLoading" class="empty-tip">
              {{ t('common.noData') }}
            </div>
          </n-tab-pane>

          <!-- 疫苗接种 Tab -->
          <n-tab-pane name="vaccination" :tab="t('health.vaccination')">
            <div class="sub-table-toolbar">
              <n-button type="primary" size="small" @click="handleAddVaccination">
                {{ t('common.add') }}
              </n-button>
            </div>
            <n-data-table
              :columns="vaccinationColumns"
              :data="vaccinationList"
              :loading="vaccinationLoading"
              :pagination="false"
              :row-key="(row: HealthVaccination) => row.id"
              size="small"
            />
            <div v-if="vaccinationList.length === 0 && !vaccinationLoading" class="empty-tip">
              {{ t('common.noData') }}
            </div>
          </n-tab-pane>

          <!-- 体检记录 Tab -->
          <n-tab-pane name="examination" :tab="t('health.examinationRecord')">
            <div class="sub-table-toolbar">
              <n-button type="primary" size="small" @click="handleAddExam">
                {{ t('common.add') }}
              </n-button>
            </div>
            <n-data-table
              :columns="examColumns"
              :data="examList"
              :loading="examLoading"
              :pagination="false"
              :row-key="(row: HealthExamination) => row.id"
              size="small"
            />
            <div v-if="examList.length === 0 && !examLoading" class="empty-tip">
              {{ t('common.noData') }}
            </div>
          </n-tab-pane>
        </n-tabs>
      </n-drawer-content>
    </n-drawer>

    <!-- 过敏史编辑弹窗 -->
    <n-modal
      v-model:show="allergyModalVisible"
      preset="card"
      :title="allergyEditingId ? t('common.edit') : t('common.add')"
      style="width: 480px"
    >
      <n-form :model="allergyForm" label-placement="left" label-width="100">
        <n-form-item :label="t('health.allergen')" required>
          <n-input v-model:value="allergyForm.allergen" :placeholder="t('health.allergenPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.severity')">
          <n-select
            v-model:value="allergyForm.severity"
            :options="severityOptions"
            :placeholder="t('health.severityPlaceholder')"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="allergyModalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="allergySaving" @click="handleSaveAllergy">
            {{ t('common.save') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 病史编辑弹窗 -->
    <n-modal
      v-model:show="historyModalVisible"
      preset="card"
      :title="historyEditingId ? t('common.edit') : t('common.add')"
      style="width: 520px"
    >
      <n-form :model="historyForm" label-placement="left" label-width="100">
        <n-form-item :label="t('health.diseaseName')" required>
          <n-input v-model:value="historyForm.diseaseName" :placeholder="t('health.diseaseNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.diagnosisDate')">
          <n-date-picker v-model:value="historyForm.diagnosisDateTs" type="date" style="width: 100%" />
        </n-form-item>
        <n-form-item :label="t('health.treatmentStatus')">
          <n-select
            v-model:value="historyForm.treatmentStatus"
            :options="treatmentStatusOptions"
            :placeholder="t('health.treatmentStatusPlaceholder')"
          />
        </n-form-item>
        <n-form-item :label="t('health.remarks')">
          <n-input v-model:value="historyForm.remarks" type="textarea" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="historyModalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="historySaving" @click="handleSaveHistory">
            {{ t('common.save') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 疫苗接种编辑弹窗 -->
    <n-modal
      v-model:show="vaccinationModalVisible"
      preset="card"
      :title="vaccinationEditingId ? t('common.edit') : t('common.add')"
      style="width: 520px"
    >
      <n-form :model="vaccinationForm" label-placement="left" label-width="100">
        <n-form-item :label="t('health.vaccineName')" required>
          <n-input v-model:value="vaccinationForm.vaccineName" :placeholder="t('health.vaccineNamePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.vaccinateDate')">
          <n-date-picker v-model:value="vaccinationForm.vaccinateDateTs" type="date" style="width: 100%" />
        </n-form-item>
        <n-form-item :label="t('health.nextDoseDate')">
          <n-date-picker v-model:value="vaccinationForm.nextDoseDateTs" type="date" style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="vaccinationModalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="vaccinationSaving" @click="handleSaveVaccination">
            {{ t('common.save') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 体检记录编辑弹窗 -->
    <n-modal
      v-model:show="examModalVisible"
      preset="card"
      :title="examEditingId ? t('common.edit') : t('common.add')"
      style="width: 520px"
    >
      <n-form :model="examForm" label-placement="left" label-width="100">
        <n-form-item :label="t('health.examNo')">
          <n-input v-model:value="examForm.examNo" :placeholder="t('health.examNoPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.examDate')">
          <n-date-picker v-model:value="examForm.examDateTs" type="date" style="width: 100%" />
        </n-form-item>
        <n-form-item :label="t('health.examItem')" required>
          <n-input v-model:value="examForm.examItem" :placeholder="t('health.examItemPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.resultValue')">
          <n-input v-model:value="examForm.resultValue" :placeholder="t('health.resultValuePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.referenceRange')">
          <n-input v-model:value="examForm.referenceRange" :placeholder="t('health.referenceRangePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.isAbnormal')">
          <n-switch v-model:value="examForm.isAbnormal" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="examModalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="examSaving" @click="handleSaveExam">
            {{ t('common.save') }}
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, reactive, ref, watch } from 'vue';
  import {
    NButton,
    NDataTable,
    NDatePicker,
    NDescriptions,
    NDescriptionsItem,
    NDrawer,
    NDrawerContent,
    NForm,
    NFormItem,
    NInput,
    NInputNumber,
    NModal,
    NSelect,
    NSpace,
    NSpin,
    NSwitch,
    NTabPane,
    NTabs,
    NTag,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type {
    HealthAllergy,
    HealthArchive,
    HealthExamination,
    HealthMedicalHistory,
    HealthVaccination,
  } from '@/api/modules';
  import {
    deleteHealthAllergy,
    deleteHealthExamination,
    deleteHealthHistory,
    deleteHealthVaccination,
    getHealthAllergies,
    getHealthArchive,
    getHealthExaminations,
    getHealthHistories,
    getHealthVaccinations,
    saveHealthAllergy,
    saveHealthExamination,
    saveHealthHistory,
    saveHealthVaccination,
    updateHealthArchive,
  } from '@/api/modules';
  import useModal from '@/hooks/useModal';

  const Message = useMessage();
  const { t } = useI18n();
  const { openModal } = useModal();

  const props = defineProps<{
    archiveId: string;
    visible: boolean;
    initialTab?: string;
  }>();

  const activeTab = ref('info');

  const emit = defineEmits<{
    (e: 'update:visible', val: boolean): void;
  }>();

  const showDrawer = computed({
    get: () => props.visible,
    set: (val) => emit('update:visible', val),
  });

  // Archive data
  const archiveData = ref<HealthArchive | null>(null);
  const archiveLoading = ref(false);

  const genderLabel = computed(() => {
    const g = archiveData.value?.gender;
    if (g === 'MALE' || g === '男') return t('health.genderMale');
    if (g === 'FEMALE' || g === '女') return t('health.genderFemale');
    return g || '-';
  });

  const lifestyleLabelMap: Record<string, Record<string, string>> = {
    smoking: {
      NONE: 'health.smokingNone',
      QUIT: 'health.smokingQuit',
      OCCASIONAL: 'health.smokingOccasional',
      REGULAR: 'health.smokingRegular',
      HEAVY: 'health.smokingHeavy',
    },
    drinking: {
      NONE: 'health.drinkingNone',
      QUIT: 'health.drinkingQuit',
      OCCASIONAL: 'health.drinkingOccasional',
      REGULAR: 'health.drinkingRegular',
      HEAVY: 'health.drinkingHeavy',
    },
    sleepQuality: {
      NEVER: 'health.sleepNever',
      RARELY: 'health.sleepRarely',
      SOMETIMES: 'health.sleepSometimes',
      OFTEN: 'health.sleepOften',
      ALWAYS: 'health.sleepAlways',
    },
    exercise: {
      'NONE': 'health.exerciseNone',
      'ONCE_WEEK': 'health.exerciseOnceWeek',
      '2_3_WEEK': 'health.exercise2_3Week',
      'DAILY': 'health.exerciseDaily',
    },
    diet: {
      LIGHT: 'health.dietLight',
      BALANCED: 'health.dietBalanced',
      OILY: 'health.dietOily',
      IRREGULAR: 'health.dietIrregular',
    },
  };

  function getLifestyleLabel(category: string, value?: string) {
    if (!value) return '-';
    const key = lifestyleLabelMap[category]?.[value];
    return key ? t(key) : value;
  }

  // Allergy state
  const allergyList = ref<HealthAllergy[]>([]);
  const allergyLoading = ref(false);
  const allergyModalVisible = ref(false);
  const allergySaving = ref(false);
  const allergyEditingId = ref<string | undefined>();

  const allergyForm = reactive({
    allergen: '',
    severity: undefined as string | undefined,
  });

  // History state
  const historyList = ref<HealthMedicalHistory[]>([]);
  const historyLoading = ref(false);
  const historyModalVisible = ref(false);
  const historySaving = ref(false);
  const historyEditingId = ref<string | undefined>();

  const historyForm = reactive({
    diseaseName: '',
    diagnosisDateTs: null as number | null,
    treatmentStatus: undefined as string | undefined,
    remarks: '',
  });

  // Vaccination state
  const vaccinationList = ref<HealthVaccination[]>([]);
  const vaccinationLoading = ref(false);
  const vaccinationModalVisible = ref(false);
  const vaccinationSaving = ref(false);
  const vaccinationEditingId = ref<string | undefined>();

  const vaccinationForm = reactive({
    vaccineName: '',
    vaccinateDateTs: null as number | null,
    nextDoseDateTs: null as number | null,
  });

  // Exam state
  const examList = ref<HealthExamination[]>([]);
  const examLoading = ref(false);
  const examModalVisible = ref(false);
  const examSaving = ref(false);
  const examEditingId = ref<string | undefined>();

  // Lifestyle form state
  const lifestyleSaving = ref(false);
  const lifestyleForm = reactive({
    smoking: '' as string,
    drinking: '' as string,
    sleepQuality: '' as string,
    exercise: '' as string,
    diet: '' as string,
  });

  // Health data form state
  const healthDataSaving = ref(false);
  const healthDataForm = reactive({
    height: null as number | null,
    weight: null as number | null,
    bloodPressure: '',
    heartRate: null as number | null,
    bloodSugar: null as number | null,
  });

  // Lifestyle select options
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
  const sleepOptions = [
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

  watch(examModalVisible, (val) => {
    console.log('[DEBUG] examModalVisible changed to:', val);
  });
  const examForm = reactive({
    examNo: '',
    examDateTs: null as number | null,
    examItem: '',
    resultValue: '',
    referenceRange: '',
    isAbnormal: false,
  });

  // Options
  const severityOptions = [
    { label: '轻微', value: 'MILD' },
    { label: '中度', value: 'MODERATE' },
    { label: '严重', value: 'SEVERE' },
  ];

  const treatmentStatusOptions = [
    { label: '治疗中', value: 'TREATMENT' },
    { label: '进行中', value: 'PROGRESS' },
    { label: '已治愈', value: 'CURED' },
  ];

  // Severity display map
  const severityMap: Record<string, string> = {
    MILD: '轻微',
    MODERATE: '中度',
    SEVERE: '严重',
  };

  const treatmentStatusMap: Record<string, string> = {
    TREATMENT: '治疗中',
    PROGRESS: '进行中',
    CURED: '已治愈',
  };

  // Columns — Allergy
  const allergyColumns = [
    { title: t('health.allergen'), key: 'allergen', width: 160, render: (row: HealthAllergy) => row.allergen || '-' },
    {
      title: t('health.severity'),
      key: 'severity',
      width: 100,
      render: (row: HealthAllergy) => severityMap[row.severity || ''] || '-',
    },
    {
      title: t('common.action'),
      key: 'action',
      width: 140,
      render: (row: HealthAllergy) => {
        return h('div', { class: 'action-btns' }, [
          h(NButton, { size: 'small', text: true, onClick: () => handleEditAllergy(row) }, () => t('common.edit')),
          h(NButton, { size: 'small', text: true, type: 'error', onClick: () => handleDeleteAllergy(row) }, () =>
            t('common.delete')
          ),
        ]);
      },
    },
  ];

  // Columns — History
  const historyColumns = [
    {
      title: t('health.diseaseName'),
      key: 'diseaseName',
      width: 160,
      render: (row: HealthMedicalHistory) => row.diseaseName || '-',
    },
    {
      title: t('health.diagnosisDate'),
      key: 'diagnosisDate',
      width: 130,
      render: (row: HealthMedicalHistory) => row.diagnosisDate || '-',
    },
    {
      title: t('health.treatmentStatus'),
      key: 'treatmentStatus',
      width: 110,
      render: (row: HealthMedicalHistory) => treatmentStatusMap[row.treatmentStatus || ''] || '-',
    },
    { title: t('health.remarks'), key: 'remarks', ellipsis: { tooltip: true } },
    {
      title: t('common.action'),
      key: 'action',
      width: 140,
      render: (row: HealthMedicalHistory) => {
        return h('div', { class: 'action-btns' }, [
          h(NButton, { size: 'small', text: true, onClick: () => handleEditHistory(row) }, () => t('common.edit')),
          h(NButton, { size: 'small', text: true, type: 'error', onClick: () => handleDeleteHistory(row) }, () =>
            t('common.delete')
          ),
        ]);
      },
    },
  ];

  // Columns — Vaccination
  const vaccinationColumns = [
    {
      title: t('health.vaccineName'),
      key: 'vaccineName',
      width: 160,
      render: (row: HealthVaccination) => row.vaccineName || '-',
    },
    {
      title: t('health.vaccinateDate'),
      key: 'vaccinateDate',
      width: 130,
      render: (row: HealthVaccination) => row.vaccinateDate || '-',
    },
    {
      title: t('health.nextDoseDate'),
      key: 'nextDoseDate',
      width: 130,
      render: (row: HealthVaccination) => row.nextDoseDate || '-',
    },
    {
      title: t('common.action'),
      key: 'action',
      width: 140,
      render: (row: HealthVaccination) => {
        return h('div', { class: 'action-btns' }, [
          h(NButton, { size: 'small', text: true, onClick: () => handleEditVaccination(row) }, () => t('common.edit')),
          h(NButton, { size: 'small', text: true, type: 'error', onClick: () => handleDeleteVaccination(row) }, () =>
            t('common.delete')
          ),
        ]);
      },
    },
  ];

  // Columns — Exam
  function formatExamDate(ts: number | string | undefined): string {
    if (!ts) return '-';
    const tss = typeof ts === 'string' ? Number(ts) : ts;
    if (!tss) return '-';
    return new Date(tss).toISOString().slice(0, 10);
  }

  const examColumns = [
    { title: t('health.examNo'), key: 'examNo', width: 130, render: (row: any) => row.examNo || '-' },
    {
      title: t('health.examDate'),
      key: 'examDate',
      width: 120,
      render: (row: any) => formatExamDate(row.examDate),
    },
    {
      title: t('health.examItem'),
      key: 'examItem',
      width: 160,
      ellipsis: { tooltip: true },
      render: (row: any) => row.examItem || '-',
    },
    {
      title: t('health.resultValue'),
      key: 'resultValue',
      width: 100,
      render: (row: any) => row.resultValue || '-',
    },
    {
      title: t('health.referenceRange'),
      key: 'referenceRange',
      width: 110,
      render: (row: any) => row.referenceRange || '-',
    },
    {
      title: t('health.isAbnormal'),
      key: 'isAbnormal',
      width: 80,
      render: (row: any) =>
        row.isAbnormal ? h(NTag, { type: 'warning', size: 'small' }, () => t('common.yes')) : t('common.no'),
    },
    {
      title: t('common.action'),
      key: 'action',
      width: 140,
      render: (row: any) => {
        return h('div', { class: 'action-btns' }, [
          h(NButton, { size: 'small', text: true, onClick: () => handleEditExam(row) }, () => t('common.edit')),
          h(NButton, { size: 'small', text: true, type: 'error', onClick: () => handleDeleteExam(row) }, () =>
            t('common.delete')
          ),
        ]);
      },
    },
  ];

  function initLifestyleForm() {
    const d = archiveData.value;
    lifestyleForm.smoking = d?.smoking || '';
    lifestyleForm.drinking = d?.drinking || '';
    lifestyleForm.sleepQuality = d?.sleepQuality || '';
    lifestyleForm.exercise = d?.exercise || '';
    lifestyleForm.diet = d?.diet || '';
  }

  function initHealthDataForm() {
    const d = archiveData.value;
    healthDataForm.height = d?.height ?? null;
    healthDataForm.weight = d?.weight ?? null;
    healthDataForm.bloodPressure = d?.bloodPressure || '';
    healthDataForm.heartRate = d?.heartRate ?? null;
    healthDataForm.bloodSugar = d?.bloodSugar ?? null;
  }

  async function handleSaveLifestyle() {
    lifestyleSaving.value = true;
    try {
      await updateHealthArchive({
        id: props.archiveId,
        smoking: lifestyleForm.smoking || undefined,
        drinking: lifestyleForm.drinking || undefined,
        sleepQuality: lifestyleForm.sleepQuality || undefined,
        exercise: lifestyleForm.exercise || undefined,
        diet: lifestyleForm.diet || undefined,
      });
      Message.success(t('common.saveSuccess'));
      await loadArchiveData();
      initLifestyleForm();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      lifestyleSaving.value = false;
    }
  }

  async function handleSaveHealthData() {
    healthDataSaving.value = true;
    try {
      await updateHealthArchive({
        id: props.archiveId,
        height: healthDataForm.height ?? undefined,
        weight: healthDataForm.weight ?? undefined,
        bloodPressure: healthDataForm.bloodPressure || undefined,
        heartRate: healthDataForm.heartRate ?? undefined,
        bloodSugar: healthDataForm.bloodSugar ?? undefined,
      });
      Message.success(t('common.saveSuccess'));
      await loadArchiveData();
      initHealthDataForm();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      healthDataSaving.value = false;
    }
  }

  // Load all data when drawer opens
  watch(
    () => props.visible,
    async (val) => {
      if (val && props.archiveId) {
        activeTab.value = props.initialTab || 'info';
        await loadArchiveData();
        initLifestyleForm();
        initHealthDataForm();
        await Promise.all([loadAllergy(), loadHistory(), loadVaccination(), loadExam()]);
      }
    }
  );

  async function loadArchiveData() {
    archiveLoading.value = true;
    try {
      const res = await getHealthArchive(props.archiveId);
      archiveData.value = res || null;
    } catch (e) {
      console.error(e);
      Message.error(t('common.loadFailed'));
    } finally {
      archiveLoading.value = false;
    }
  }

  async function loadAllergy() {
    allergyLoading.value = true;
    try {
      const res = await getHealthAllergies(props.archiveId);
      allergyList.value = res || [];
    } catch (e) {
      console.error(e);
    } finally {
      allergyLoading.value = false;
    }
  }

  async function loadHistory() {
    historyLoading.value = true;
    try {
      const res = await getHealthHistories(props.archiveId);
      historyList.value = res || [];
    } catch (e) {
      console.error(e);
    } finally {
      historyLoading.value = false;
    }
  }

  async function loadVaccination() {
    vaccinationLoading.value = true;
    try {
      const res = await getHealthVaccinations(props.archiveId);
      vaccinationList.value = res || [];
    } catch (e) {
      console.error(e);
    } finally {
      vaccinationLoading.value = false;
    }
  }

  async function loadExam() {
    examLoading.value = true;
    try {
      const res = await getHealthExaminations(props.archiveId);
      examList.value = res || [];
    } catch (e) {
      console.error(e);
    } finally {
      examLoading.value = false;
    }
  }

  // ==================== Allergy CRUD ====================

  function handleAddAllergy() {
    console.log('[DEBUG] handleAddAllergy called');
    allergyEditingId.value = undefined;
    allergyForm.allergen = '';
    allergyForm.severity = undefined;
    allergyModalVisible.value = true;
    console.log('[DEBUG] allergyModalVisible set, value:', allergyModalVisible.value);
  }

  function handleEditAllergy(row: HealthAllergy) {
    allergyEditingId.value = row.id;
    allergyForm.allergen = row.allergen;
    allergyForm.severity = row.severity;
    allergyModalVisible.value = true;
  }

  async function handleSaveAllergy() {
    if (!allergyForm.allergen?.trim()) {
      Message.warning(t('health.allergenPlaceholder'));
      return;
    }
    allergySaving.value = true;
    try {
      await saveHealthAllergy({
        id: allergyEditingId.value,
        customerId: props.archiveId,
        allergen: allergyForm.allergen.trim(),
        severity: allergyForm.severity,
      });
      Message.success(t('common.saveSuccess'));
      allergyModalVisible.value = false;
      await loadAllergy();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      allergySaving.value = false;
    }
  }

  async function handleDeleteAllergy(row: HealthAllergy) {
    if (!row.id) return;
    openModal({
      type: 'warning',
      title: t('common.deleteConfirmTitle', { name: row.allergen }),
      content: t('health.deleteAllergyConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthAllergy(row.id!);
          Message.success(t('common.deleteSuccess'));
          await loadAllergy();
        } catch (e) {
          console.error(e);
          Message.error(t('common.deleteFailed'));
        }
      },
    });
  }

  // ==================== History CRUD ====================

  function handleAddHistory() {
    console.log('[DEBUG] handleAddHistory called');
    historyEditingId.value = undefined;
    historyForm.diseaseName = '';
    historyForm.diagnosisDateTs = null;
    historyForm.treatmentStatus = undefined;
    historyForm.remarks = '';
    historyModalVisible.value = true;
    console.log('[DEBUG] historyModalVisible set, value:', historyModalVisible.value);
  }

  function handleEditHistory(row: HealthMedicalHistory) {
    historyEditingId.value = row.id;
    historyForm.diseaseName = row.diseaseName;
    historyForm.diagnosisDateTs = row.diagnosisDate ? new Date(row.diagnosisDate).getTime() : null;
    historyForm.treatmentStatus = row.treatmentStatus;
    historyForm.remarks = row.remarks || '';
    historyModalVisible.value = true;
  }

  async function handleSaveHistory() {
    if (!historyForm.diseaseName?.trim()) {
      Message.warning(t('health.diseaseNamePlaceholder'));
      return;
    }
    historySaving.value = true;
    try {
      await saveHealthHistory({
        id: historyEditingId.value,
        customerId: props.archiveId,
        diseaseName: historyForm.diseaseName.trim(),
        diagnosisDate: historyForm.diagnosisDateTs ?? undefined,
        treatmentStatus: historyForm.treatmentStatus,
        remarks: historyForm.remarks,
      });
      Message.success(t('common.saveSuccess'));
      historyModalVisible.value = false;
      await loadHistory();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      historySaving.value = false;
    }
  }

  async function handleDeleteHistory(row: HealthMedicalHistory) {
    if (!row.id) return;
    openModal({
      type: 'warning',
      title: t('common.deleteConfirmTitle', { name: row.diseaseName }),
      content: t('health.deleteHistoryConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthHistory(row.id!);
          Message.success(t('common.deleteSuccess'));
          await loadHistory();
        } catch (e) {
          console.error(e);
          Message.error(t('common.deleteFailed'));
        }
      },
    });
  }

  // ==================== Vaccination CRUD ====================

  function handleAddVaccination() {
    console.log('[DEBUG] handleAddVaccination called');
    vaccinationEditingId.value = undefined;
    vaccinationForm.vaccineName = '';
    vaccinationForm.vaccinateDateTs = null;
    vaccinationForm.nextDoseDateTs = null;
    vaccinationModalVisible.value = true;
    console.log('[DEBUG] vaccinationModalVisible set, value:', vaccinationModalVisible.value);
  }

  function handleEditVaccination(row: HealthVaccination) {
    vaccinationEditingId.value = row.id;
    vaccinationForm.vaccineName = row.vaccineName;
    vaccinationForm.vaccinateDateTs = row.vaccinateDate ? new Date(row.vaccinateDate).getTime() : null;
    vaccinationForm.nextDoseDateTs = row.nextDoseDate ? new Date(row.nextDoseDate).getTime() : null;
    vaccinationModalVisible.value = true;
  }

  async function handleSaveVaccination() {
    if (!vaccinationForm.vaccineName?.trim()) {
      Message.warning(t('health.vaccineNamePlaceholder'));
      return;
    }
    vaccinationSaving.value = true;
    try {
      await saveHealthVaccination({
        id: vaccinationEditingId.value,
        customerId: props.archiveId,
        vaccineName: vaccinationForm.vaccineName.trim(),
        vaccinateDate: vaccinationForm.vaccinateDateTs ?? undefined,
        nextDoseDate: vaccinationForm.nextDoseDateTs ?? undefined,
      });
      Message.success(t('common.saveSuccess'));
      vaccinationModalVisible.value = false;
      await loadVaccination();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      vaccinationSaving.value = false;
    }
  }

  async function handleDeleteVaccination(row: HealthVaccination) {
    if (!row.id) return;
    openModal({
      type: 'warning',
      title: t('common.deleteConfirmTitle', { name: row.vaccineName }),
      content: t('health.deleteVaccinationConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthVaccination(row.id!);
          Message.success(t('common.deleteSuccess'));
          await loadVaccination();
        } catch (e) {
          console.error(e);
          Message.error(t('common.deleteFailed'));
        }
      },
    });
  }

  // ==================== Exam CRUD ====================

  function handleAddExam() {
    console.log('[DEBUG] handleAddExam called');
    examEditingId.value = undefined;
    examForm.examNo = '';
    examForm.examDateTs = null;
    examForm.examItem = '';
    examForm.resultValue = '';
    examForm.referenceRange = '';
    examForm.isAbnormal = false;
    examModalVisible.value = true;
    console.log('[DEBUG] examModalVisible set, value:', examModalVisible.value);
  }

  function handleEditExam(row: any) {
    examEditingId.value = row.id;
    examForm.examNo = row.examNo || '';
    examForm.examDateTs = row.examDate || null;
    examForm.examItem = row.examItem || '';
    examForm.resultValue = row.resultValue || '';
    examForm.referenceRange = row.referenceRange || '';
    examForm.isAbnormal = Boolean(row.isAbnormal);
    examModalVisible.value = true;
  }

  async function handleSaveExam() {
    if (!examForm.examItem?.trim()) {
      Message.warning(t('health.examItemRequired'));
      return;
    }
    examSaving.value = true;
    try {
      await saveHealthExamination({
        id: examEditingId.value,
        customerId: archiveData.value?.customerId || archiveData.value?.id,
        examNo: examForm.examNo,
        examDate: examForm.examDateTs,
        examItem: examForm.examItem.trim(),
        resultValue: examForm.resultValue,
        referenceRange: examForm.referenceRange,
        isAbnormal: examForm.isAbnormal,
      });
      Message.success(t('common.saveSuccess'));
      examModalVisible.value = false;
      await loadExam();
    } catch (e) {
      console.error(e);
      Message.error(t('common.saveFailed'));
    } finally {
      examSaving.value = false;
    }
  }

  async function handleDeleteExam(row: any) {
    if (!row.id) return;
    openModal({
      type: 'warning',
      title: t('common.deleteConfirmTitle', { name: row.examNo || t('health.examinationRecord') }),
      content: t('health.deleteExamConfirm'),
      positiveText: t('common.confirmDelete'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await deleteHealthExamination(row.id!);
          Message.success(t('common.deleteSuccess'));
          await loadExam();
        } catch (e) {
          console.error(e);
          Message.error(t('common.deleteFailed'));
        }
      },
    });
  }
</script>

<style lang="less" scoped>
  .detail-tabs {
    :deep(.n-tabs-tab) {
      font-size: 14px;
    }
  }

  .sub-table-toolbar {
    margin-bottom: 12px;
  }

  .empty-tip {
    text-align: center;
    color: var(--text-color-3);
    padding: 24px 0;
  }

  .action-btns {
    display: flex;
    gap: 8px;
  }
</style>
