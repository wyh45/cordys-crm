<template>
  <div class="health-ai">
    <n-grid :cols="7" :x-gap="24" responsive="screen">
      <n-gi :span="3">
        <n-card :bordered="false" class="panel-card input-panel" title="体检数据">
          <template #header-extra>
            <n-tag type="info" size="small">AI 智能解读</n-tag>
          </template>

          <n-form ref="formRef" :model="form" label-placement="top" size="medium" class="ai-form">
            <n-form-item label="选择健康档案" path="archiveId" required>
              <n-select
                v-model:value="form.archiveId"
                :options="archiveOptions"
                :loading="archiveLoading"
                placeholder="搜索并选择患者健康档案..."
                filterable
                clearable
                class="uniform-select"
                @update:value="handleArchiveChange"
              />
            </n-form-item>

            <n-form-item label="建议类型" path="suggestionTypes">
              <n-checkbox-group v-model:value="form.suggestionTypes">
                <n-space :size="12" wrap>
                  <n-checkbox v-for="opt in suggestionTypeOptions" :key="opt.value" :value="opt.value">
                    {{ opt.label }}
                    <n-tag
                      v-if="interpretedTypes.has(opt.value)"
                      size="tiny"
                      type="success"
                      :bordered="false"
                      style="margin-left: 4px"
                      >已解读</n-tag
                    >
                  </n-checkbox>
                </n-space>
              </n-checkbox-group>
            </n-form-item>

            <n-alert v-if="patientInfo" type="info" :bordered="false" class="patient-summary">
              <template #header>
                <span class="patient-name">{{ patientInfo.customerName || '--' }}</span>
                <n-tag size="tiny" :bordered="false" style="margin-left: 8px">{{ patientInfo.gender || '--' }}</n-tag>
                <n-tag
                  v-if="patientInfo.source"
                  size="tiny"
                  :type="
                    patientInfo.source === 'both'
                      ? 'warning'
                      : patientInfo.source === 'abnormal_exam'
                      ? 'error'
                      : 'info'
                  "
                  style="margin-left: 4px"
                >
                  {{ sourceLabel(patientInfo.source) }}
                </n-tag>
              </template>
              <div class="patient-meta">
                <span v-if="patientInfo.age">年龄：{{ patientInfo.age }} 岁</span>
                <span v-if="patientInfo.phone">手机：{{ maskedPhone }}</span>
              </div>
            </n-alert>

            <n-collapse v-if="form.archiveId" class="archive-detail-collapse" :default-expanded-names="['health']">
              <n-collapse-item name="health" title="健康档案概览">
                <n-spin :show="archiveDetailLoading" size="small">
                  <n-grid :cols="2" :x-gap="12" :y-gap="8" class="detail-grid">
                    <n-gi v-if="archiveDetail?.height || archiveDetail?.weight">
                      <span class="detail-label">身高/体重</span>
                      <span class="detail-value"
                        >{{ archiveDetail?.height || '-' }}cm / {{ archiveDetail?.weight || '-' }}kg</span
                      >
                    </n-gi>
                    <n-gi v-if="archiveDetail?.bloodPressure">
                      <span class="detail-label">血压</span>
                      <span class="detail-value">{{ archiveDetail?.bloodPressure }} mmHg</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.bloodSugar != null">
                      <span class="detail-label">血糖</span>
                      <span class="detail-value">{{ archiveDetail?.bloodSugar }} mmol/L</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.heartRate">
                      <span class="detail-label">心率</span>
                      <span class="detail-value">{{ archiveDetail?.heartRate }} 次/分</span>
                    </n-gi>
                  </n-grid>
                  <n-divider
                    v-if="
                      archiveDetail?.smoking ||
                      archiveDetail?.drinking ||
                      archiveDetail?.sleepQuality ||
                      archiveDetail?.exercise ||
                      archiveDetail?.diet
                    "
                    style="margin: 8px 0"
                  />
                  <n-grid :cols="3" :x-gap="12" :y-gap="8" class="detail-grid">
                    <n-gi v-if="archiveDetail?.smoking">
                      <span class="detail-label">吸烟</span>
                      <span class="detail-value">{{ lifestyleLabel('smoking', archiveDetail?.smoking) }}</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.drinking">
                      <span class="detail-label">饮酒</span>
                      <span class="detail-value">{{ lifestyleLabel('drinking', archiveDetail?.drinking) }}</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.sleepQuality">
                      <span class="detail-label">熬夜</span>
                      <span class="detail-value">{{
                        lifestyleLabel('sleepQuality', archiveDetail?.sleepQuality)
                      }}</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.exercise">
                      <span class="detail-label">运动</span>
                      <span class="detail-value">{{ lifestyleLabel('exercise', archiveDetail?.exercise) }}</span>
                    </n-gi>
                    <n-gi v-if="archiveDetail?.diet">
                      <span class="detail-label">饮食</span>
                      <span class="detail-value">{{ lifestyleLabel('diet', archiveDetail?.diet) }}</span>
                    </n-gi>
                  </n-grid>
                  <n-divider v-if="archiveDetail?.allergies" style="margin: 8px 0" />
                  <div v-if="archiveDetail?.allergies" class="detail-row">
                    <span class="detail-label">过敏史</span>
                    <span class="detail-value">{{ archiveDetail?.allergies }}</span>
                  </div>
                  <n-divider v-if="allergies.length > 0" style="margin: 8px 0" />
                  <div v-if="allergies.length > 0" class="detail-row">
                    <span class="detail-label">过敏记录</span>
                    <n-space :size="4" wrap>
                      <n-tag v-for="a in allergies" :key="a.id" size="small" type="warning">{{ a.allergen }}</n-tag>
                    </n-space>
                  </div>
                  <n-divider v-if="archiveDetail?.pastMedicalHistory" style="margin: 8px 0" />
                  <div v-if="archiveDetail?.pastMedicalHistory" class="detail-row">
                    <span class="detail-label">既往病史</span>
                    <span class="detail-value">{{ archiveDetail?.pastMedicalHistory }}</span>
                  </div>
                  <n-divider v-if="histories.length > 0" style="margin: 8px 0" />
                  <div v-if="histories.length > 0" class="detail-row">
                    <span class="detail-label">病史记录</span>
                    <n-space :size="4" wrap>
                      <n-tag v-for="h in histories" :key="h.id" size="small" type="error">{{ h.diseaseName }}</n-tag>
                    </n-space>
                  </div>
                  <n-divider v-if="vaccinations.length > 0" style="margin: 8px 0" />
                  <div v-if="vaccinations.length > 0" class="detail-row">
                    <span class="detail-label">疫苗接种</span>
                    <n-space :size="4" wrap>
                      <n-tag v-for="v in vaccinations" :key="v.id" size="small" type="success">{{
                        v.vaccineName
                      }}</n-tag>
                    </n-space>
                  </div>
                  <n-divider v-if="examList.length > 0" style="margin: 8px 0" />
                  <div v-if="examList.length > 0" class="detail-row">
                    <span class="detail-label">体检记录</span>
                    <n-space :size="4" wrap>
                      <n-tag
                        v-for="e in examList.slice(0, 10)"
                        :key="e.id"
                        size="small"
                        :type="e.isAbnormal ? 'error' : 'default'"
                        >{{ e.examItem }}: {{ e.resultValue || '-' }}</n-tag
                      >
                      <span
                        v-if="examList.length > 10"
                        style="font-size: 12px; color: var(--text-color-3); margin-left: 4px"
                        >...等{{ examList.length }}条</span
                      >
                    </n-space>
                  </div>
                  <n-divider v-if="archiveDetail?.familyHistory" style="margin: 8px 0" />
                  <div v-if="archiveDetail?.familyHistory" class="detail-row">
                    <span class="detail-label">家族病史</span>
                    <span class="detail-value">{{ archiveDetail?.familyHistory }}</span>
                  </div>
                </n-spin>
              </n-collapse-item>
            </n-collapse>
            <n-divider />
            <n-space justify="center" :size="16">
              <n-button
                type="primary"
                :loading="interpreting"
                :disabled="!canInterpret"
                size="large"
                round
                @click="handleInterpret()"
                >开始 AI 解读</n-button
              >
              <n-button
                type="warning"
                :loading="assessingRisk"
                :disabled="!form.archiveId"
                size="large"
                round
                @click="handleRiskAssess"
                >AI 风险评估</n-button
              >
              <n-button
                type="info"
                :disabled="!form.archiveId || interpretHistory.length === 0"
                size="large"
                round
                @click="openSmsDialog"
                >短信推送</n-button
              >
              <n-button size="large" round @click="handleReset">刷新</n-button>
            </n-space>
          </n-form>
        </n-card>
      </n-gi>

      <n-gi :span="4">
        <n-card :bordered="false" class="panel-card result-panel" title="解读报告">
          <template #header-extra>
            <n-space :size="8">
              <n-button v-if="hasResult" text size="small" @click="handleCopy">复制</n-button>
              <n-button
                v-if="hasResult && patientInfo?.customerId"
                type="primary"
                ghost
                size="small"
                @click="handlePushToPatient"
                >推送给患者</n-button
              >
            </n-space>
          </template>

          <div v-if="!hasResult && !interpreting" class="result-state empty-state">
            <n-empty description="选择患者档案并点击「开始 AI 解读」获取健康风险评估报告" style="padding: 40px 0" />
          </div>

          <div v-else-if="hasResult" class="result-structured">
            <n-alert v-if="interpreting" type="info" :bordered="false" style="margin-bottom: 12px">
              AI 正在分析中，已生成的内容实时显示如下...
            </n-alert>
            <n-tabs v-model:value="activeResultTab" type="card" animated>
              <n-tab-pane v-for="type in Object.keys(result.results)" :key="type" :name="type" :tab="type">
                <div class="section-body">{{ stripThink(stripJson(result.results[type])) }}</div>
              </n-tab-pane>
            </n-tabs>
            <n-divider v-if="uninterpretedTypes.length > 0 && !interpreting" style="margin: 16px 0" />
            <div v-if="uninterpretedTypes.length > 0 && !interpreting" class="add-more-section">
              <span class="add-more-hint">还可添加解读：</span>
              <n-space :size="8">
                <n-button
                  v-for="t in uninterpretedTypes"
                  :key="t"
                  size="small"
                  ghost
                  type="primary"
                  @click="addInterpretType(t)"
                  >{{ t }}</n-button
                >
              </n-space>
            </div>
          </div>

          <div v-else class="result-state interpreting-state">
            <n-spin size="large" />
            <p class="state-title">AI 正在分析体检数据...</p>
            <p class="state-desc">正在综合评估各项指标，生成个性化健康建议，请耐心等待</p>
          </div>

          <!-- Risk Assessment: streaming + results -->
          <div
            v-if="assessingRisk || riskStreamText || riskResult"
            class="risk-assessment-section"
            style="margin-top: 16px"
          >
            <n-alert v-if="assessingRisk" type="info" :bordered="false" style="margin-bottom: 12px">
              AI 正在评估健康风险，实时输出如下...
            </n-alert>
            <div
              v-if="riskStreamText"
              style="
                white-space: pre-wrap;
                font-size: 14px;
                line-height: 1.8;
                margin-bottom: 12px;
                max-height: 200px;
                overflow: auto;
                background: var(--n-color-embedded);
                padding: 12px;
                border-radius: 8px;
              "
              >{{ riskStreamText }}</div
            >
            <template v-if="displayedRisk">
              <div v-if="riskResultTabs.length > 1" style="margin-bottom: 12px">
                <n-radio-group :value="selectedRiskTab" size="small" @update:value="selectedRiskTab = $event">
                  <n-radio-button v-for="tab in riskResultTabs" :key="tab.id" :value="tab.id" :label="tab.label" />
                </n-radio-group>
              </div>
              <RiskChart v-if="displayedRisk.assessments?.length" :assessments="displayedRisk.assessments" />
              <n-card
                v-for="(item, idx) in displayedRisk.assessments"
                :key="idx"
                :bordered="true"
                size="small"
                class="risk-card"
                style="margin-bottom: 12px"
              >
                <template #header>
                  <div class="flex items-center gap-[8px]">
                    <span class="risk-disease-name">{{ item.diseaseName }}</span>
                    <n-tag :type="riskLevelType(item.riskLevel)" size="small" round>{{ item.riskLevel }}</n-tag>
                  </div>
                </template>
                <div class="risk-body">
                  <p class="risk-intro">{{ item.introduce }}</p>
                  <p class="risk-analysis-text">{{ item.analysis }}</p>
                </div>
              </n-card>
            </template>
          </div>

          <n-divider v-if="interpretHistory.length > 0" style="margin: 20px 0 12px" />
          <div v-if="interpretHistory.length > 0" class="history-section">
            <h4 class="history-title">历史解读记录</h4>
            <n-collapse>
              <n-collapse-item v-for="group in groupedHistory" :key="group.type" :name="group.type">
                <template #header>
                  <div class="flex items-center gap-[8px]">
                    <span>{{ group.type }}</span>
                    <n-tag size="tiny" :bordered="false">{{ group.items.length }}条</n-tag>
                  </div>
                </template>
                <div v-for="item in group.items" :key="item.id" class="history-item" @click="selectedHistory = item">
                  <div class="history-meta">
                    <span class="history-time">{{ formatTime(item.interpretTime) }}</span>
                    <n-tag v-if="item.pushChannel" size="tiny" :bordered="false" type="info">{{
                      item.pushChannel
                    }}</n-tag>
                    <n-button
                      text
                      size="tiny"
                      type="error"
                      style="margin-left: auto"
                      @click.stop="handleDeleteInterpretRecord(item, $event)"
                      >删除</n-button
                    >
                  </div>
                  <p class="history-preview"
                    >{{ (item.interpretation || '').slice(0, 120)
                    }}{{ (item.interpretation || '').length > 120 ? '...' : '' }}</p
                  >
                </div>
              </n-collapse-item>
            </n-collapse>
          </div>

          <n-modal
            :show="selectedHistory !== null"
            preset="card"
            title="历史解读详情"
            style="max-width: 700px"
            @update:show="(v: boolean) => { if (!v) selectedHistory = null }"
          >
            <div
              v-if="selectedHistory"
              style="max-height: 500px; overflow: auto; white-space: pre-wrap; font-size: 14px; line-height: 1.8"
            >
              <div class="history-detail-meta">
                <span>时间：{{ formatTime(selectedHistory.interpretTime) }}</span>
                <n-tag
                  v-if="selectedHistory.pushChannel"
                  size="small"
                  :bordered="false"
                  type="info"
                  style="margin-left: 8px"
                  >{{ selectedHistory.pushChannel === 'SMS' ? '已短信推送' : selectedHistory.pushChannel }}</n-tag
                >
              </div>
              <n-divider style="margin: 12px 0" />
              {{ selectedHistory.interpretation }}
              <template v-if="selectedHistory.pushContent">
                <n-divider style="margin: 12px 0" />
                <div class="push-content-label">推送内容</div>
                {{ selectedHistory.pushContent }}
              </template>
            </div>
          </n-modal>
        </n-card>
      </n-gi>
    </n-grid>

    <n-modal
      :show="smsDialog.show"
      preset="card"
      title="短信推送 — 生成邀约文案"
      style="max-width: 640px"
      @update:show="(v: boolean) => { smsDialog.show = v }"
    >
      <div v-if="smsDialog.show" style="max-height: 70vh; overflow: auto">
        <n-descriptions label-placement="top" :column="2" size="small" style="margin-bottom: 12px">
          <n-descriptions-item label="客户">{{ smsDialog.customerName || '-' }}</n-descriptions-item>
          <n-descriptions-item label="电话">{{ smsDialog.phone || '-' }}</n-descriptions-item>
        </n-descriptions>
        <n-divider style="margin: 8px 0" />
        <div class="sms-section-label">选择AI解读记录（可多选）</div>
        <n-checkbox-group v-model:value="smsDialog.selectedRecordIds" style="margin-bottom: 8px">
          <n-space vertical :size="4">
            <n-checkbox
              v-for="r in smsRecords"
              :key="r.id"
              :value="r.id"
              :label="`[${r.suggestionType}] ${formatTime(r.interpretTime)}`"
            />
          </n-space>
        </n-checkbox-group>
        <div class="sms-section-label">健康知识（可选）</div>
        <n-input
          v-model:value="smsDialog.knowledge"
          type="textarea"
          :rows="3"
          placeholder="从健康知识页面搜索并粘贴相关内容..."
          style="margin-bottom: 12px"
        />
        <n-button
          type="primary"
          :loading="smsDialog.generating"
          :disabled="smsDialog.generating || smsDialog.selectedRecordIds.length === 0"
          block
          style="margin-bottom: 12px"
          @click="handleGenerateSmsStream"
        >
          {{ smsDialog.generating ? 'AI 正在生成中...' : '生成精简文案' }}
        </n-button>
        <div
          v-if="smsDialog.streamText"
          style="
            max-height: 160px;
            overflow: auto;
            white-space: pre-wrap;
            font-size: 13px;
            line-height: 1.7;
            background: var(--n-color-embedded);
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 12px;
            color: var(--text-color-2);
          "
          >{{ smsDialog.streamText }}</div
        >
        <template v-if="smsDialog.preview">
          <n-divider style="margin: 8px 0" />
          <div class="sms-section-label">预览短信</div>
          <n-card size="small" style="background: var(--n-color-embedded); margin-bottom: 8px">
            <div style="white-space: pre-wrap; font-size: 14px; line-height: 1.8">
              【云南昊邦健康产业】客户称呼:{{ smsDialog.greeting }}您好！根据您近期在我中心的体检结果，体检发现:{{
                smsDialog.finding
              }}。为保障您的身体健康，健康建议:{{
                smsDialog.action
              }}。如需预约复查或健康咨询，请联系我中心。祝您健康！拒收请回复R
            </div>
          </n-card>
          <n-space justify="end">
            <n-button @click="handleGenerateSmsStream">重新生成</n-button>
            <n-button type="primary" :loading="smsDialog.sending" :disabled="!smsValid" @click="handleSendSms"
              >确认推送</n-button
            >
          </n-space>
        </template>
        <template v-if="smsHistory.length > 0">
          <n-divider style="margin: 12px 0" />
          <n-collapse>
            <n-collapse-item title="推送历史" :name="'push-history'">
              <div
                v-for="h in smsHistory"
                :key="h.id"
                style="
                  font-size: 12px;
                  color: var(--text-color-3);
                  margin-bottom: 6px;
                  padding: 6px 8px;
                  background: var(--n-color-embedded);
                  border-radius: 6px;
                "
              >
                <span style="color: var(--text-color-2)">{{ formatTime(h.interpretTime) }}</span>
                {{ (h.pushContent || '').substring(0, 80) }}{{ (h.pushContent || '').length > 80 ? '...' : '' }}
              </div>
            </n-collapse-item>
          </n-collapse>
        </template>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import {
    NAlert,
    NButton,
    NCard,
    NCheckbox,
    NCheckboxGroup,
    NCollapse,
    NCollapseItem,
    NDescriptions,
    NDescriptionsItem,
    NDivider,
    NEmpty,
    NForm,
    NFormItem,
    NGi,
    NGrid,
    NInput,
    NModal,
    NRadioButton,
    NRadioGroup,
    NSelect,
    NSpace,
    NSpin,
    NTabPane,
    NTabs,
    NTag,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import RiskChart from './RiskChart.vue';

  import {
    assessRisk,
    batchInterpretationStatus,
    deleteInterpretRecord,
    getHealthAiEligibleArchives,
    getHealthAllergies,
    getHealthArchive,
    getHealthExaminations,
    getHealthHistories,
    getHealthVaccinations,
    getInterpretationHistory,
    healthAiInterpret,
    type HealthAiInterpretRecord,
    type HealthAiInterpretResult,
    type HealthAllergy,
    type HealthExamination,
    type HealthMedicalHistory,
    type HealthVaccination,
    pushHealthKnowledge,
    recordSmsPush,
    saveInterpretRecord,
    sendInviteSms,
    summarizeSms,
  } from '@/api/modules';

  import { useSseStream } from './useSseStream';

  // Module-level SSE state — survives component unmount during page navigation
  let sseReader: ReadableStreamDefaultReader<Uint8Array> | null = null;
  let sseAbortController: AbortController | null = null;
  const sseResult = reactive<Record<string, string>>({});
  let sseArchiveId: string | null = null;
  const sseActive = ref(false);
  const result = ref<HealthAiInterpretResult>({ results: {} });

  const Message = useMessage();
  const { t } = useI18n();
  const route = useRoute();

  const archiveOptions = ref<
    {
      label: string;
      value: string;
      source?: string;
      phone?: string;
      gender?: string;
      age?: number;
      customerName?: string;
      customerId?: string;
    }[]
  >([]);
  const archiveLoading = ref(false);
  const patientInfo = ref<{
    customerName?: string;
    gender?: string;
    age?: number;
    phone?: string;
    customerId?: string;
    source?: string;
  } | null>(null);

  const form = reactive({
    archiveId: null as string | null,
    suggestionTypes: ['检查建议'] as string[],
    reportData: {} as Record<string, any>,
  });
  const suggestionTypeOptions = [
    { label: '检查建议', value: '检查建议' },
    { label: '生活方式建议', value: '生活方式建议' },
    { label: '饮食建议', value: '饮食建议' },
    { label: '运动建议', value: '运动建议' },
  ];
  const allSuggestionTypes = suggestionTypeOptions.map((o) => o.value);
  const interpretedTypes = ref<Set<string>>(new Set());
  const activeResultTab = ref('检查建议');
  const formRef = ref<any>(null);
  const interpreting = computed(() => sseActive.value);
  const hasResult = computed(() => Object.keys(sseResult).length > 0);
  const patientPhone = ref<string | null>(null);

  const archiveDetail = ref<any>(null);
  const allergies = ref<HealthAllergy[]>([]);
  const histories = ref<HealthMedicalHistory[]>([]);
  const vaccinations = ref<HealthVaccination[]>([]);
  const examList = ref<HealthExamination[]>([]);
  const archiveDetailLoading = ref(false);
  const interpretHistory = ref<HealthAiInterpretRecord[]>([]);
  const historyLoading = ref(false);
  const selectedHistory = ref<HealthAiInterpretRecord | null>(null);

  const lifestyleLabel = (field: string, val: string | null) => {
    if (!val) return '-';
    const maps: Record<string, Record<string, string>> = {
      smoking: { NONE: '不吸烟', QUIT: '已戒烟', OCCASIONAL: '偶尔吸烟', REGULAR: '经常吸烟', HEAVY: '重度吸烟' },
      drinking: { NONE: '不饮酒', QUIT: '已戒酒', OCCASIONAL: '偶尔饮酒', REGULAR: '经常饮酒', HEAVY: '重度饮酒' },
      sleepQuality: {
        NEVER: '从不熬夜',
        RARELY: '很少熬夜',
        SOMETIMES: '有时熬夜',
        OFTEN: '经常熬夜',
        ALWAYS: '总是熬夜',
      },
      exercise: { 'NONE': '不运动', 'ONCE_WEEK': '每周1次', '2_3_WEEK': '每周2-3次', 'DAILY': '每天运动' },
      diet: { LIGHT: '清淡', BALANCED: '均衡', OILY: '油腻', IRREGULAR: '不规律' },
    };
    return maps[field]?.[val] || val;
  };

  const canInterpret = computed(() => !!form.archiveId && form.suggestionTypes.length > 0);

  const assessingRisk = ref(false);
  const riskStreamText = ref('');
  const riskResult = ref<any>(null);
  const selectedRiskTab = ref<string>('latest');

  const smsDialog = reactive({
    show: false,
    customerName: '',
    phone: '',
    selectedRecordIds: [] as string[],
    knowledge: '',
    generating: false,
    streamText: '',
    preview: false,
    greeting: '',
    finding: '',
    action: '',
    sending: false,
  });

  const smsRecords = computed(() => interpretHistory.value.filter((r) => r.pushChannel !== 'SMS'));

  const smsValid = computed(
    () =>
      smsDialog.greeting &&
      smsDialog.finding &&
      smsDialog.action &&
      !smsDialog.finding.includes('失败') &&
      !smsDialog.action.includes('失败')
  );

  const smsHistory = computed(() => interpretHistory.value.filter((r) => r.pushChannel === 'SMS' && r.pushContent));

  const riskResultTabs = computed(() => {
    const tabs: { id: string; label: string; data: any }[] = [];
    if (riskResult.value) tabs.push({ id: 'latest', label: '最新评估', data: riskResult.value });
    interpretHistory.value
      .filter((r) => r.suggestionType === '风险评估' && r.interpretation)
      .forEach((r) => {
        if (!tabs.some((x) => x.id === r.id)) {
          try {
            tabs.push({ id: r.id, label: formatTime(r.interpretTime), data: JSON.parse(r.interpretation!) });
          } catch {
            // skip malformed
          }
        }
      });
    return tabs;
  });

  const displayedRisk = computed(() => {
    const match = riskResultTabs.value.find((x) => x.id === selectedRiskTab.value);
    return match?.data ?? riskResult.value;
  });

  const groupedHistory = computed(() => {
    const groups = new Map<string, HealthAiInterpretRecord[]>();
    interpretHistory.value.forEach((item) => {
      const type = item.suggestionType || '其他';
      if (!groups.has(type)) groups.set(type, []);
      groups.get(type)!.push(item);
    });
    return Array.from(groups.entries()).map(([type, items]) => ({ type, items }));
  });

  const uninterpretedTypes = computed(() =>
    allSuggestionTypes.filter((type) => !Object.keys(result.value.results).includes(type))
  );

  const maskedPhone = computed(() => {
    const p = patientInfo.value?.phone;
    if (!p || p.length < 7) return '***';
    return `${p.substring(0, 3)}****${p.substring(p.length - 4)}`;
  });

  function sourceLabel(s: string) {
    const map: Record<string, string> = { abnormal_exam: '异常体检', follow_rule: '随访规则', both: '异常+规则' };
    return map[s] || s;
  }

  function stripThink(text: string): string {
    if (!text) return '';
    return text.replace(/<think>[\s\S]*?<\/think>/g, '').trim();
  }

  function stripJson(text: string): string {
    if (!text) return '';
    const cleaned = text
      .replace(/```json[\s\S]*?```/g, '')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/\{[\s\S]*"risk_level"[\s\S]*?\}/g, '')
      .replace(/\{[\s\S]*"风险评估"[\s\S]*?\}/g, '')
      .replace(/^\s*\{[\s\S]*\}\s*$/gm, '')
      .trim();
    return cleaned || text.trim();
  }

  const AI_STATE_KEY = 'health_ai_state';

  function saveAiState() {
    sessionStorage.setItem(
      AI_STATE_KEY,
      JSON.stringify({
        archiveId: form.archiveId,
        hasResult: hasResult.value,
        interpreting: interpreting.value,
        result: result.value,
      })
    );
  }

  function loadAiState() {
    try {
      const raw = sessionStorage.getItem(AI_STATE_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }

  function clearAiState() {
    sessionStorage.removeItem(AI_STATE_KEY);
  }

  onBeforeUnmount(() => {
    if (sseActive.value) {
      saveAiState();
      // DON'T abort SSE — it continues in module scope
    }
  });

  onMounted(async () => {
    // Reconnect to active SSE IMMEDIATELY — don't wait for anything
    if (sseActive.value && sseReader && sseArchiveId) {
      form.archiveId = sseArchiveId;
      result.value = { results: sseResult };
      loadArchiveOptions();
      handleArchiveChange(sseArchiveId).catch(() => {});
      return;
    }

    await loadArchiveOptions();
    const qArchiveId = route.query.archiveId as string;
    if (qArchiveId) {
      form.archiveId = qArchiveId;
      clearAiState();
      handleArchiveChange(qArchiveId);
      return;
    }
    const saved = loadAiState();
    if (saved?.archiveId) {
      form.archiveId = saved.archiveId;
      await handleArchiveChange(saved.archiveId);
      if (saved.hasResult && saved.result?.results) {
        Object.assign(sseResult, saved.result.results);
        result.value = { results: sseResult };
      }
    }
  });

  async function loadArchiveOptions() {
    archiveLoading.value = true;
    try {
      const res = await getHealthAiEligibleArchives({ page: 1, pageSize: 200 });
      const list = res?.list || [];
      archiveOptions.value = list.map((item: any) => ({
        label: `${item.archiveNo || ''} — ${item.customerName || '未知'}${
          item.source ? ` [${sourceLabel(item.source)}]` : ''
        }`,
        value: item.id,
        phone: item.phone,
        gender: item.gender,
        age: item.age,
        customerName: item.customerName,
        customerId: item.customerId,
        source: item.source,
      }));
    } catch {
      /* silent */
    } finally {
      archiveLoading.value = false;
    }
  }

  async function handleArchiveChange(archiveId: string) {
    if (!archiveId) {
      patientInfo.value = null;
      form.reportData = {};
      archiveDetail.value = null;
      allergies.value = [];
      histories.value = [];
      return;
    }
    const selected = archiveOptions.value.find((a) => a.value === archiveId);
    if (selected) {
      patientInfo.value = {
        customerName: selected.customerName,
        gender: selected.gender,
        age: selected.age,
        phone: selected.phone,
        customerId: selected.customerId,
        source: selected.source,
      };
    }
    archiveDetailLoading.value = true;
    try {
      const [detail, allergyList, historyList, vaccList, examData] = await Promise.all([
        getHealthArchive(archiveId).catch(() => null),
        getHealthAllergies(archiveId).catch(() => []),
        getHealthHistories(archiveId).catch(() => []),
        getHealthVaccinations(archiveId).catch(() => []),
        getHealthExaminations(archiveId).catch(() => []),
      ]);
      archiveDetail.value = detail;
      allergies.value = Array.isArray(allergyList) ? allergyList : [];
      histories.value = Array.isArray(historyList) ? historyList : [];
      vaccinations.value = Array.isArray(vaccList) ? vaccList : [];
      examList.value = Array.isArray(examData) ? examData : [];
      form.reportData = {};
      if (detail) {
        if (detail.height != null) form.reportData.height = String(detail.height);
        if (detail.weight != null) form.reportData.weight = String(detail.weight);
        if (detail.bloodType) form.reportData.bloodType = detail.bloodType;
        if (detail.bloodSugar != null) form.reportData.bloodGlucose = String(detail.bloodSugar);
        if (detail.bloodPressure) form.reportData.bloodPressure = detail.bloodPressure;
        if (detail.heartRate != null) form.reportData.heartRate = String(detail.heartRate);
        if (detail.allergies) form.reportData.allergies = detail.allergies;
        if (detail.pastMedicalHistory) form.reportData.pastMedicalHistory = detail.pastMedicalHistory;
        if (detail.familyHistory) form.reportData.familyHistory = detail.familyHistory;
      }
      if (allergies.value.length > 0) {
        form.reportData.allergies = allergies.value.map((a) => a.allergen).join('、');
      }
      if (histories.value.length > 0) {
        form.reportData.pastMedicalHistory = histories.value.map((h) => h.diseaseName).join('、');
      }
      if (vaccinations.value.length > 0) {
        form.reportData.vaccinations = vaccinations.value
          .map((v) => `${v.vaccineName}(${v.vaccinateDate || '-'})`)
          .join(', ');
      }
      if (examList.value.length > 0) {
        const items = examList.value
          .map((e) => {
            const parts = [e.examItem, e.resultValue].filter(Boolean);
            if (e.isAbnormal) parts.push('[异常]');
            return parts.join(': ');
          })
          .filter(Boolean);
        if (items.length > 0) form.reportData.examResults = items.join('\n');
      }
    } catch {
      /* silent */
    } finally {
      archiveDetailLoading.value = false;
    }
    saveAiState();
    loadInterpretHistory();
    loadInterpretedStatus();
  }

  async function loadInterpretHistory() {
    if (!form.archiveId) return;
    historyLoading.value = true;
    try {
      const res = await getInterpretationHistory({ page: 1, pageSize: 200 });
      interpretHistory.value = Array.isArray(res) ? res.filter((r) => r.archiveId === form.archiveId) : [];
      const latestRisk = interpretHistory.value.find((r) => r.suggestionType === '风险评估');
      if (latestRisk?.interpretation) {
        try {
          riskResult.value = JSON.parse(latestRisk.interpretation);
        } catch {
          riskResult.value = null;
        }
      }
    } catch {
      interpretHistory.value = [];
    } finally {
      historyLoading.value = false;
    }
  }

  async function handleDeleteInterpretRecord(item: HealthAiInterpretRecord, event: Event) {
    event.stopPropagation();
    if (!item.id) return;
    try {
      await deleteInterpretRecord(item.id);
      Message.success('删除成功');
      await loadInterpretHistory();
      await loadInterpretedStatus();
      if (selectedRiskTab.value === item.id) selectedRiskTab.value = 'latest';
      if (item.suggestionType === '风险评估' && !interpretHistory.value.some((r) => r.suggestionType === '风险评估')) {
        riskResult.value = null;
      }
    } catch {
      Message.error('删除失败');
    }
  }

  function formatTime(ts: number | undefined): string {
    if (!ts) return '-';
    return new Date(ts).toLocaleString('zh-CN');
  }

  async function handleInterpret(extraTypes?: string[]) {
    if (!form.archiveId) {
      Message.warning('请选择健康档案');
      return;
    }
    const rawTypes = extraTypes || form.suggestionTypes;
    let types: string[];
    if (Array.isArray(rawTypes)) types = rawTypes;
    else if (rawTypes) types = [rawTypes];
    else types = [];
    if (types.length === 0) {
      Message.warning('请选择至少一种建议类型');
      return;
    }

    // If SSE is already active for this archive, reconnect UI
    if (sseActive.value && sseReader && sseArchiveId === form.archiveId) {
      result.value.results = sseResult;
      return;
    }
    // Stale SSE state — reset
    if (sseActive.value && !sseReader) {
      sseActive.value = false;
    }

    if (!extraTypes) {
      Object.keys(sseResult).forEach((k) => delete sseResult[k]);
      result.value = { results: sseResult };
    } else {
      types.forEach((type) => {
        sseResult[type] = '';
      });
      result.value.results = sseResult;
    }
    const activeTab = types[0];
    activeResultTab.value = activeTab;

    sseArchiveId = form.archiveId;
    sseActive.value = true;
    sseAbortController = new AbortController();
    try {
      const resp = await fetch('/front/health/ai/interpret-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          archiveId: form.archiveId,
          reportType: 'GENERAL',
          reportData: form.reportData,
          select: types,
        }),
        signal: sseAbortController.signal,
      });
      sseReader = resp.body?.getReader();
      if (!sseReader) throw new Error('No response stream');
      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        // eslint-disable-next-line no-await-in-loop
        const { done, value } = await sseReader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        let eventName = '';
        let streamDone = false;
        for (let i = 0; i < lines.length; i++) {
          const line = lines[i];
          if (line.startsWith('event:')) {
            eventName = line.substring(6).trim();
          } else if (line.startsWith('data:') && eventName) {
            const payload = line.substring(5).trim();
            if (eventName === 'done') {
              streamDone = true;
            } else if (eventName.endsWith('_end')) {
              sseResult[eventName.replace('_end', '')] = payload;
            } else if (eventName !== 'error') {
              sseResult[eventName] = (sseResult[eventName] || '') + payload;
            }
            if (eventName === 'error') Message.error(payload);
            saveAiState();
            eventName = '';
          }
        }
        if (streamDone) {
          sseActive.value = false;
          saveAiState();
          loadInterpretHistory();
          loadInterpretedStatus();
        }
      }
    } catch (error: any) {
      if (error?.name !== 'AbortError') {
        Message.error(error?.message || 'AI解读失败，请稍后重试');
      }
    } finally {
      sseActive.value = false;
      sseReader = null;
      sseAbortController = null;
    }
  }

  async function addInterpretType(type: string) {
    await handleInterpret([type]);
  }

  async function loadInterpretedStatus() {
    if (!form.archiveId) return;
    try {
      const status = await batchInterpretationStatus([form.archiveId]);
      const types = status?.[form.archiveId] || [];
      interpretedTypes.value = new Set(types);
    } catch {
      interpretedTypes.value = new Set();
    }
  }

  async function handleRiskAssess() {
    if (!form.archiveId || assessingRisk.value) return;
    assessingRisk.value = true;
    riskStreamText.value = '';
    riskResult.value = null;
    try {
      await useSseStream({
        url: '/front/health/ai/assess-risk-stream',
        body: { archiveId: form.archiveId, reportType: 'GENERAL', reportData: form.reportData },
        onData: (_event, payload) => {
          riskStreamText.value += payload;
        },
        onDone: async (fullText) => {
          riskStreamText.value = '';
          const cleaned = fullText
            .replace(/<\/?think>/g, '')
            .replace(/<think>[\s\S]*?<\/think>/g, '')
            .replace(/```json\s*/g, '')
            .replace(/```\s*/g, '')
            .trim();
          try {
            riskResult.value = JSON.parse(cleaned);
            selectedRiskTab.value = 'latest';
            await saveInterpretRecord({
              archiveId: form.archiveId,
              customerId: patientInfo.value?.customerId || '',
              customerName: patientInfo.value?.customerName || '',
              suggestionType: '风险评估',
              interpretation: cleaned,
            }).catch(() => {});
            await loadInterpretHistory();
            await loadInterpretedStatus();
          } catch (e) {
            console.error('[RiskAssess] JSON parse failed', e, 'cleaned:', cleaned);
            Message.error('风险评估结果解析失败');
          }
        },
        onError: (err) => {
          Message.error(err);
        },
      });
    } catch (error: any) {
      if (error?.name !== 'AbortError') {
        Message.error(error?.message || '风险评估失败');
      }
    } finally {
      assessingRisk.value = false;
    }
  }

  function riskLevelType(level: string) {
    if (!level) return 'default';
    if (level.includes('极高') || level.includes('7')) return 'error';
    if (level.includes('高') || level.includes('6') || level.includes('5')) return 'warning';
    if (level.includes('中') || level.includes('4') || level.includes('3')) return 'info';
    return 'success';
  }

  function handleReset() {
    sseAbortController?.abort();
    sseActive.value = false;
    form.archiveId = null;
    form.suggestionTypes = ['检查建议'];
    form.reportData = {};
    patientInfo.value = null;
    archiveDetail.value = null;
    allergies.value = [];
    histories.value = [];
    vaccinations.value = [];
    examList.value = [];
    interpretedTypes.value = new Set();
    Object.keys(sseResult).forEach((k) => delete sseResult[k]);
    result.value = { results: {} };
    riskResult.value = null;
    riskStreamText.value = '';
    clearAiState();
  }

  async function handleCopy() {
    const active = activeResultTab.value;
    const text = result.value.results[active] || '';
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
      Message.success('复制成功');
    } catch {
      Message.error('复制失败');
    }
  }

  async function handlePushToPatient() {
    if (!patientInfo.value?.customerId) return;
    const active = activeResultTab.value;
    const pushContent = result.value.results[active];
    if (!pushContent) return;
    try {
      await pushHealthKnowledge({
        customerIds: patientInfo.value.customerId ? [patientInfo.value.customerId] : [],
        channel: 'SMS',
        title: `健康${active} — ${patientInfo.value.customerName || '患者'}`,
        content: pushContent,
      });
      saveInterpretRecord({
        archiveId: form.archiveId!,
        customerId: patientInfo.value?.customerId,
        customerName: patientInfo.value?.customerName,
        suggestionType: active,
        interpretation: pushContent,
        pushContent,
        pushChannel: 'SMS',
      }).catch(() => {});
      Message.success('健康报告已推送给患者');
      loadInterpretHistory();
      loadInterpretedStatus();
    } catch (error: any) {
      Message.error(error?.message || '推送失败');
    }
  }

  function openSmsDialog() {
    if (!patientInfo.value?.phone) {
      Message.warning('该客户没有手机号');
      return;
    }
    smsDialog.show = true;
    smsDialog.customerName = patientInfo.value?.customerName || '';
    smsDialog.phone = patientInfo.value?.phone || '';
    smsDialog.selectedRecordIds = [];
    smsDialog.knowledge = '';
    smsDialog.generating = false;
    smsDialog.streamText = '';
    smsDialog.preview = false;
    smsDialog.greeting = '';
    smsDialog.finding = '';
    smsDialog.action = '';
    smsDialog.sending = false;
    loadInterpretHistory();
  }

  async function handleGenerateSmsStream() {
    if (smsDialog.selectedRecordIds.length === 0) {
      Message.warning('请至少选择一条AI解读记录');
      return;
    }
    const records = interpretHistory.value
      .filter((r) => smsDialog.selectedRecordIds.includes(r.id!))
      .map((r) => ({ type: r.suggestionType || '健康解读', content: r.interpretation || '' }));
    smsDialog.generating = true;
    smsDialog.streamText = '';
    smsDialog.preview = false;
    try {
      const resp = await fetch('/front/health/ai/summarize-sms-stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          records,
          knowledge: smsDialog.knowledge || undefined,
          customerName: smsDialog.customerName || undefined,
          gender: patientInfo.value?.gender || undefined,
        }),
      });
      const reader = resp.body?.getReader();
      if (!reader) throw new Error('No response stream');
      const decoder = new TextDecoder();
      let buffer = '';
      while (true) {
        // eslint-disable-next-line no-await-in-loop
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';
        let eventName = '';
        // eslint-disable-next-line no-restricted-syntax
        for (const line of lines) {
          if (line.startsWith('event:')) {
            eventName = line.substring(6).trim();
          } else if (line.startsWith('data:') && eventName) {
            const payload = line.substring(5).trim();
            console.log('[SMS SSE]', eventName, payload.substring(0, 200));
            if (eventName === 'sms') {
              smsDialog.streamText += payload.replace(/<think>[\s\S]*?<\/think>/g, '').replace(/<\/?think>/g, '');
            } else if (eventName === 'sms_end') {
              try {
                const cleaned = payload
                  .replace(/<think>[\s\S]*?<\/think>/g, '')
                  .replace(/<\/?think>/g, '')
                  .replace(/```[a-zA-Z]*\s*/g, '')
                  .replace(/```/g, '');
                const jsonStart = cleaned.lastIndexOf('{');
                const jsonEnd = cleaned.lastIndexOf('}');
                const json =
                  jsonStart >= 0 && jsonEnd > jsonStart ? cleaned.substring(jsonStart, jsonEnd + 1) : cleaned;
                console.log('[SMS SSE] parsed json:', json);
                const parsed = JSON.parse(json);
                console.log('[SMS SSE] parsed result:', parsed);
                smsDialog.greeting = parsed.greeting || '';
                smsDialog.finding = parsed.finding || '';
                smsDialog.action = parsed.action || '';
                smsDialog.preview = true;
                console.log('[SMS SSE] preview set, greeting:', smsDialog.greeting);
              } catch (e) {
                console.error('[SMS SSE] parse error:', e);
              }
            } else if (eventName === 'done') {
              try {
                const parsed = JSON.parse(payload);
                smsDialog.greeting = parsed.greeting || '';
                smsDialog.finding = parsed.finding || '';
                smsDialog.action = parsed.action || '';
                smsDialog.preview = true;
              } catch {
                /* parse failed */
              }
            } else if (eventName === 'error') {
              Message.error(payload);
            }
            eventName = '';
          }
        }
      }
    } catch (error: any) {
      if (error?.name !== 'AbortError') {
        Message.error(error?.message || '生成失败');
      }
    } finally {
      smsDialog.generating = false;
    }
  }

  async function handleSendSms() {
    smsDialog.sending = true;
    try {
      const res = await sendInviteSms({
        phone: '18088237511', // 测试号码
        greeting: smsDialog.greeting,
        finding: smsDialog.finding,
        action: smsDialog.action,
        archiveId: form.archiveId || undefined,
        customerName: smsDialog.customerName || undefined,
      });
      if (res.success) {
        Message.success('短信推送成功');
        smsDialog.show = false;
        if (form.archiveId) {
          recordSmsPush(form.archiveId, 7).catch(() => {});
        }
        saveInterpretRecord({
          archiveId: form.archiveId!,
          customerId: patientInfo.value?.customerId,
          customerName: patientInfo.value?.customerName,
          suggestionType: '短信推送',
          interpretation: smsDialog.selectedRecordIds.join(','),
          pushContent: `【云南昊邦健康产业】客户称呼:${smsDialog.greeting}您好！根据您近期在我中心的体检结果，体检发现:${smsDialog.finding}。为保障您的身体健康，健康建议:${smsDialog.action}。如需预约复查或健康咨询，请联系我中心。祝您健康！拒收请回复R`,
          pushChannel: 'SMS',
        }).catch(() => {});
        loadInterpretHistory();
      } else {
        Message.error(res.message || '推送失败');
      }
    } catch (error: any) {
      Message.error(error?.message || '推送失败');
    } finally {
      smsDialog.sending = false;
    }
  }
</script>

<style lang="less" scoped>
  .health-ai {
    padding: 24px;
    height: 100%;
    overflow: auto;
  }
  .panel-card {
    height: 100%;
    border-radius: 12px;
    :deep(.n-card-header) {
      padding: 20px 24px 16px;
      font-size: 16px;
      font-weight: 600;
    }
    :deep(.n-card__content) {
      padding: 0 24px 24px;
    }
  }
  .input-panel {
    :deep(.n-card-header) {
      border-bottom: 1px solid var(--divider-color);
    }
  }
  .result-panel {
    :deep(.n-card-header) {
      border-bottom: 1px solid var(--divider-color);
    }
  }
  .uniform-select {
    :deep(.n-base-selection) {
      border-radius: 8px;
      min-height: 38px;
    }
    :deep(.n-base-selection-label) {
      font-size: 14px;
    }
  }
  .ai-form {
    :deep(.n-form-item-label) {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-color-2);
    }
  }
  .patient-summary {
    margin: 8px 0 4px;
    border-radius: 8px;
    .patient-name {
      font-size: 15px;
      font-weight: 600;
    }
    .patient-meta {
      margin-top: 6px;
      display: flex;
      flex-wrap: wrap;
      gap: 16px;
      font-size: 13px;
      color: var(--text-color-3);
    }
  }
  .archive-detail-collapse {
    margin-top: 8px;
    :deep(.n-collapse-item__header-main) {
      font-size: 14px;
      font-weight: 500;
    }
    .detail-grid {
      padding: 4px 0;
    }
    .detail-label {
      font-size: 12px;
      color: var(--text-color-3);
      display: block;
    }
    .detail-value {
      font-size: 13px;
      color: var(--text-color-1);
      font-weight: 500;
    }
    .detail-row {
      padding: 4px 0;
      .detail-label {
        font-size: 12px;
        color: var(--text-color-3);
        margin-bottom: 4px;
      }
      .detail-value {
        font-size: 13px;
        color: var(--text-color-1);
        line-height: 1.6;
      }
    }
  }
  .result-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 60px 20px;
    gap: 12px;
  }
  .interpreting-state {
    .state-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--primary-color);
      margin-top: 16px;
    }
    .state-desc {
      font-size: 13px;
      color: var(--text-color-3);
    }
  }
  .result-structured {
    padding-top: 4px;
  }
  .disease-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding: 16px 20px;
    background: linear-gradient(135deg, var(--primary-color-suppl), transparent);
    border-radius: 10px;
    .disease-name {
      margin: 0;
      font-size: 20px;
      font-weight: 700;
      color: var(--text-color-1);
    }
  }
  .result-collapse {
    :deep(.n-collapse-item__header) {
      font-size: 15px;
      font-weight: 600;
      padding: 14px 0;
    }
    :deep(.n-collapse-item__content-inner) {
      padding-top: 12px;
      padding-bottom: 16px;
    }
  }
  .risk-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 16px;
    margin-bottom: 8px;
  }
  .risk-card {
    padding: 16px;
    border-radius: 10px;
    background: var(--card-color);
    border: 1px solid var(--divider-color);
    .risk-label {
      font-size: 12px;
      color: var(--text-color-3);
      margin-bottom: 8px;
      text-transform: uppercase;
      letter-spacing: 1px;
    }
    .risk-value {
      font-size: 14px;
      line-height: 1.6;
      color: var(--text-color-2);
      margin-bottom: 10px;
    }
    &.highlight {
      border-color: var(--success-color);
      background: color-mix(in srgb, var(--success-color) 8%, transparent);
    }
  }
  .intervention-card {
    padding: 16px;
    border-radius: 10px;
    background: var(--card-color);
    border: 1px solid var(--divider-color);
    height: 100%;
    .intervention-icon {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 10px;
      &.lifestyle {
        background: color-mix(in srgb, #10b981 15%, transparent);
        color: #10b981;
      }
      &.exercise {
        background: color-mix(in srgb, #f59e0b 15%, transparent);
        color: #f59e0b;
      }
      &.diet {
        background: color-mix(in srgb, #ef4444 15%, transparent);
        color: #ef4444;
      }
      &.medical {
        background: color-mix(in srgb, #3b82f6 15%, transparent);
        color: #3b82f6;
      }
    }
    h4 {
      margin: 0 0 8px;
      font-size: 14px;
      font-weight: 600;
    }
    p {
      margin: 0;
      font-size: 13px;
      line-height: 1.7;
      color: var(--text-color-2);
    }
  }
  .intro-text {
    font-size: 14px;
    line-height: 1.8;
    color: var(--text-color-2);
    margin: 0;
  }
  .result-raw {
    .section-body {
      font-size: 14px;
      line-height: 1.8;
      color: var(--text-color-2);
      white-space: pre-wrap;
      padding: 16px;
      background: var(--card-color);
      border-radius: 8px;
    }
  }
  .history-section {
    .history-title {
      margin: 0 0 12px;
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color-1);
    }
    .history-item {
      padding: 10px 12px;
      border-radius: 8px;
      border: 1px solid var(--divider-color);
      margin-bottom: 8px;
      cursor: pointer;
      transition: background 0.2s;
      &:hover {
        background: var(--hover-color);
      }
      .history-meta {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;
      }
      .history-time {
        font-size: 12px;
        color: var(--text-color-3);
      }
      .history-preview {
        margin: 0;
        font-size: 13px;
        color: var(--text-color-2);
        line-height: 1.6;
      }
    }
  }
  .history-detail-meta {
    font-size: 13px;
    color: var(--text-color-2);
  }
  .push-content-label {
    font-size: 12px;
    color: var(--text-color-3);
    margin-bottom: 4px;
  }
</style>
