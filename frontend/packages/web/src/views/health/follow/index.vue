<template>
  <div class="health-follow">
    <CrmCard no-content-padding hide-footer>
      <div class="px-[16px] pb-[16px] pt-[24px]">
        <n-collapse class="rule-collapse">
          <n-collapse-item name="rules" :title="t('health.followRuleConfig')">
            <HealthFollowRuleTable />
          </n-collapse-item>
        </n-collapse>

        <n-collapse v-if="matchedArchives.length > 0" class="match-collapse" :default-expanded-names="['matched']">
          <n-collapse-item name="matched">
            <template #header>
              <div class="flex items-center gap-[8px]">
                <span>{{ t('health.matchedArchives') }}</span>
                <n-tag type="info" size="small" :bordered="false">{{ filteredArchives.length }}</n-tag>
              </div>
            </template>
            <n-space :size="12" wrap class="filter-row">
              <n-checkbox-group v-model:value="selectedRuleNames">
                <n-checkbox v-for="name in allRuleNames" :key="name" :value="name">{{ name }}</n-checkbox>
              </n-checkbox-group>
            </n-space>
            <n-data-table
              :columns="matchedColumns"
              :data="pagedArchives"
              :bordered="false"
              :single-line="false"
              size="small"
              :row-props="() => ({ style: 'cursor: pointer' })"
              @row-click="handleRowClick"
            />
            <div
              v-if="filteredArchives.length > pageSize"
              style="display: flex; justify-content: flex-end; margin-top: 12px"
            >
              <n-pagination
                v-model:page="matchPage"
                v-model:page-size="pageSize"
                :item-count="filteredArchives.length"
                :page-sizes="[10, 20, 50]"
                show-size-picker
              />
            </div>
          </n-collapse-item>
        </n-collapse>

        <HealthFollowTable class="follow-table" />
      </div>
    </CrmCard>

    <n-modal
      v-model:show="smsDialogVisible"
      preset="card"
      :title="t('health.smsRepushDialogTitle')"
      style="max-width: 600px"
    >
      <n-spin :show="smsDialogLoading">
        <div style="max-height: 400px; overflow: auto; white-space: pre-wrap; font-size: 14px; line-height: 1.8">
          {{ stripContent(smsDialogData?.lastInterpretation) || '暂无解读记录' }}
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="smsDialogVisible = false">取消</n-button>
          <n-button type="warning" ghost @click="goToAiInterpret">AI解读其他建议</n-button>
          <n-button type="primary" :loading="smsDialogLoading" @click="confirmSmsRepush">{{
            t('health.confirmRepush')
          }}</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="followInterpretModalVisible" preset="card" title="AI解读详情" style="max-width: 700px">
      <n-spin :show="followInterpretModalLoading">
        <div
          v-if="followInterpretModalData"
          style="max-height: 500px; overflow: auto; white-space: pre-wrap; font-size: 14px; line-height: 1.8"
        >
          <div style="margin-bottom: 12px; font-size: 13px; color: var(--text-color-2)">
            客户：{{ followInterpretModalData.customerName }} | 建议类型：{{ followInterpretModalData.type }}
          </div>
          <n-divider style="margin: 8px 0" />
          {{ followInterpretModalData.content }}
        </div>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="followInterpretModalVisible = false">关闭</n-button>
          <n-button type="primary" @click="copyFollowInterpretContent">复制</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onMounted, onUnmounted, provide, ref, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import {
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NCollapse,
    NCollapseItem,
    NDataTable,
    NDivider,
    NModal,
    NPagination,
    NSpace,
    NSpin,
    NTag,
    useDialog,
    useMessage,
  } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import HealthFollowRuleTable from '../components/healthFollowRuleTable.vue';
  import HealthFollowTable from '../components/healthFollowTable.vue';

  import {
    batchCheckActionStatus,
    batchInterpretationStatus,
    evaluateFollowRules,
    getInterpretationHistory,
    getLastInterpretation,
    pushHealthKnowledge,
    recordPhoneContact,
    recordSmsPush,
    type RuleMatchedArchive,
  } from '@/api/modules';

  import { HealthRouteEnum } from '@/enums/routeEnum';

  const { t } = useI18n();
  const router = useRouter();
  const Message = useMessage();
  const dialog = useDialog();

  const matchedArchives = ref<RuleMatchedArchive[]>([]);
  const smsLoadingSet = ref<Set<string>>(new Set());
  const actionStatusMap = ref<Record<string, any>>({});
  const interpretedTypesMap = ref<Record<string, string[]>>({});
  const now = ref(Date.now());
  const matchPage = ref(1);
  const pageSize = ref(20);
  const selectedRuleNames = ref<string[]>([]);

  const allRuleNames = computed(() => [...new Set(matchedArchives.value.map((a) => a.ruleName || '').filter(Boolean))]);

  interface MergedArchive {
    archiveId: string;
    archiveNo?: string;
    customerName?: string;
    phone?: string;
    abnormalCount: number;
    synced?: boolean;
    ruleNames: string[];
    followMethods: string[];
    followInterval: number;
    matchedExamItems: string[];
  }

  const mergedArchives = computed(() => {
    const map = new Map<string, MergedArchive>();
    matchedArchives.value.forEach((a) => {
      const key = a.archiveId || '';
      if (!key) return;
      const existing = map.get(key);
      if (existing) {
        if (!existing.ruleNames.includes(a.ruleName)) existing.ruleNames.push(a.ruleName);
        (a.followMethod || '').split(',').forEach((s) => {
          const code = followMethodCode[s.trim()] || s.trim();
          if (code && !existing.followMethods.includes(code)) existing.followMethods.push(code);
        });
        if (a.followInterval < existing.followInterval) existing.followInterval = a.followInterval;
        (a.matchedExamItems || []).forEach((item) => {
          if (!existing.matchedExamItems.includes(item)) existing.matchedExamItems.push(item);
        });
      } else {
        const methods: string[] = [];
        (a.followMethod || '').split(',').forEach((s) => {
          const code = followMethodCode[s.trim()] || s.trim();
          if (code) methods.push(code);
        });
        map.set(key, {
          archiveId: a.archiveId,
          archiveNo: a.archiveNo,
          customerName: a.customerName,
          phone: a.phone,
          abnormalCount: a.abnormalCount,
          synced: a.synced,
          ruleNames: [a.ruleName],
          followMethods: methods,
          followInterval: a.followInterval,
          matchedExamItems: [...(a.matchedExamItems || [])],
        });
      }
    });
    return Array.from(map.values());
  });

  const filteredArchives = computed(() => {
    if (selectedRuleNames.value.length === 0) return mergedArchives.value;
    const selected = new Set(selectedRuleNames.value);
    return mergedArchives.value.filter((a) => a.ruleNames.some((n) => selected.has(n)));
  });

  const pagedArchives = computed(() => {
    const start = (matchPage.value - 1) * pageSize.value;
    return filteredArchives.value.slice(start, start + pageSize.value);
  });

  watch(selectedRuleNames, () => {
    matchPage.value = 1;
  });

  const followMethodCode: Record<string, string> = {
    短信: 'SMS',
    电话: 'PHONE',
  };
  let timerInterval: ReturnType<typeof setInterval> | null = null;
  const smsDialogVisible = ref(false);
  const smsDialogData = ref<{
    archiveId: string;
    customerName: string;
    lastInterpretation: string;
    followInterval: number;
  } | null>(null);
  const smsDialogLoading = ref(false);
  const followInterpretModalVisible = ref(false);
  const followInterpretModalLoading = ref(false);
  const followInterpretModalData = ref<{ customerName: string; type: string; content: string } | null>(null);

  const followMethodLabel: Record<string, string> = {
    PHONE: '电话',
    SMS: '短信',
  };

  function formatCountdown(cooldownUntil: number): string {
    const remaining = cooldownUntil - now.value;
    if (remaining <= 0) return '';
    const days = Math.floor(remaining / 86400000);
    const hours = Math.floor((remaining % 86400000) / 3600000);
    if (days > 0) return `${days}天${hours}时`;
    const mins = Math.floor((remaining % 3600000) / 60000);
    return `${hours}时${mins}分`;
  }

  function getActionStatus(archiveId: string | undefined) {
    if (!archiveId) return {};
    return actionStatusMap.value[archiveId] || {};
  }

  async function checkHistoryThenAct(row: MergedArchive) {
    if (!row.archiveId) return;
    try {
      const record = await getLastInterpretation(row.archiveId);
      if (record?.interpretation) {
        openSmsDialog(row);
      } else {
        router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: row.archiveId } });
      }
    } catch (e) {
      console.error(e);
      router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: row.archiveId } });
    }
  }

  function stripContent(text: string | undefined): string {
    if (!text) return '';
    return text
      .replace(/<think>[\s\S]*?<\/think>/g, '')
      .replace(/```json[\s\S]*?```/g, '')
      .replace(/```[\s\S]*?```/g, '')
      .replace(/\{[\s\S]*"risk_level"[\s\S]*?\}/g, '')
      .replace(/^\s*\{[\s\S]*\}\s*$/gm, '')
      .trim();
  }

  function goToAiInterpret() {
    smsDialogVisible.value = false;
    const data = smsDialogData.value;
    if (data?.archiveId) {
      router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: data.archiveId } });
    }
  }

  async function openSmsDialog(row: MergedArchive) {
    if (!row.archiveId) return;
    smsDialogLoading.value = true;
    smsDialogVisible.value = true;
    try {
      const record = await getLastInterpretation(row.archiveId);
      smsDialogData.value = {
        archiveId: row.archiveId,
        customerName: row.customerName || '',
        lastInterpretation: record?.interpretation || '暂无历史解读记录',
        followInterval: row.followInterval,
      };
    } catch (e) {
      console.error(e);
      smsDialogData.value = {
        archiveId: row.archiveId,
        customerName: row.customerName || '',
        lastInterpretation: '暂无历史解读记录',
        followInterval: row.followInterval,
      };
    } finally {
      smsDialogLoading.value = false;
    }
  }

  async function confirmSmsRepush() {
    const data = smsDialogData.value;
    if (!data) return;
    smsDialogLoading.value = true;
    try {
      const pushContent = `【健康随访】${data.customerName}，以下是您的健康建议。${data.lastInterpretation}`;
      await pushHealthKnowledge({
        customerIds: [data.archiveId],
        channel: 'SMS',
        title: '健康随访建议',
        content: pushContent,
      });
      const interval = data.followInterval || 7;
      await recordSmsPush(data.archiveId, interval);
      actionStatusMap.value[data.archiveId] = {
        ...actionStatusMap.value[data.archiveId],
        smsPushed: true,
        smsFrozen: true,
        smsCooldownUntil: Date.now() + interval * 86400000,
      };
      Message.success(t('health.smsPushSuccess'));
      smsDialogVisible.value = false;
    } catch (err: any) {
      Message.error(err?.message || '短信推送失败');
    } finally {
      smsDialogLoading.value = false;
    }
  }

  async function openFollowInterpretModal(row: MergedArchive, type: string) {
    followInterpretModalVisible.value = true;
    followInterpretModalLoading.value = true;
    followInterpretModalData.value = null;
    try {
      const all = await getInterpretationHistory({ page: 1, pageSize: 200 });
      const records = Array.isArray(all)
        ? all.filter((r: any) => r.archiveId === row.archiveId && r.suggestionType === type)
        : [];
      const match = records.length > 0 ? records[0] : null;
      followInterpretModalData.value = {
        customerName: row.customerName || '-',
        type,
        content: match?.interpretation || '暂无解读内容',
      };
    } catch (e) {
      console.error(e);
      followInterpretModalData.value = { customerName: row.customerName || '-', type, content: '加载失败' };
    } finally {
      followInterpretModalLoading.value = false;
    }
  }

  async function copyFollowInterpretContent() {
    if (!followInterpretModalData.value?.content) return;
    try {
      await navigator.clipboard.writeText(followInterpretModalData.value.content);
      Message.success('复制成功');
    } catch (e) {
      console.error(e);
      Message.error('复制失败');
    }
  }

  const matchedColumns = [
    {
      title: t('health.ruleName'),
      key: 'ruleName',
      width: 180,
      render: (row: MergedArchive) =>
        h(
          'div',
          { class: 'flex flex-wrap gap-[4px]' },
          row.ruleNames.map((name: string) => h(NTag, { type: 'info', size: 'small', bordered: false }, () => name))
        ),
    },
    {
      title: t('health.customerName'),
      key: 'customerName',
      width: 100,
    },
    {
      title: t('health.phone'),
      key: 'phone',
      width: 130,
    },
    {
      title: t('health.abnormalItems'),
      key: 'abnormalCount',
      width: 80,
      render: (row: MergedArchive) =>
        h(NTag, { type: 'error', size: 'small', bordered: false }, () => String(row.abnormalCount)),
    },
    {
      title: '同步状态',
      key: 'synced',
      width: 90,
      render: (row: MergedArchive) =>
        row.synced ? h(NTag, { type: 'success', size: 'small' }, { default: () => t('health.synced') }) : '-',
    },
    {
      title: 'AI已解读',
      key: 'interpreted',
      width: 160,
      render: (row: MergedArchive) => {
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
                      openFollowInterpretModal(row, type);
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
      title: t('health.matchedExamItems'),
      key: 'matchedExamItems',
      width: 200,
      render: (row: MergedArchive) =>
        h(
          'div',
          { class: 'flex flex-wrap gap-[4px]' },
          (row.matchedExamItems || []).map((item: string) =>
            h(NTag, { type: 'warning', size: 'small', bordered: false }, () => item)
          )
        ),
    },
    {
      title: t('health.followMethod'),
      key: 'followMethod',
      width: 90,
      render: (row: MergedArchive) => row.followMethods.map((m: string) => followMethodLabel[m] || m).join('、') || '-',
    },
    {
      title: t('health.followInterval'),
      key: 'followInterval',
      width: 80,
      render: (row: MergedArchive) => `${row.followInterval}天`,
    },
    {
      title: '操作',
      key: 'action',
      width: 260,
      render: (row: MergedArchive) => {
        const status = getActionStatus(row.archiveId);
        const buttons: any[] = [];

        if (row.followMethods.includes('SMS')) {
          const pushed = status.smsPushed;
          const frozen = status.smsFrozen;
          const cooldown = status.smsCooldownUntil || 0;
          if (pushed && frozen) {
            const label = `${t('health.smsNotified')} ${formatCountdown(cooldown)}`;
            buttons.push(h(NButton, { size: 'small', type: 'default', disabled: true }, { default: () => label }));
          } else {
            buttons.push(
              h(
                NButton,
                {
                  size: 'small',
                  type: 'primary',
                  ghost: !!pushed,
                  disabled: !row.archiveId,
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    checkHistoryThenAct(row);
                  },
                },
                { default: () => t('health.smsFollow') }
              )
            );
          }
        }

        if (row.followMethods.includes('PHONE')) {
          const contacted = status.phoneContacted;
          const frozen = status.phoneFrozen;
          const cooldown = status.phoneCooldownUntil || 0;
          if (contacted && frozen) {
            const label = `${t('health.contacted')} ${formatCountdown(cooldown)}`;
            buttons.push(h(NButton, { size: 'small', type: 'default', disabled: true }, { default: () => label }));
          } else {
            buttons.push(
              h(
                NButton,
                {
                  size: 'small',
                  type: 'primary',
                  ghost: !!contacted,
                  disabled: !row.archiveId,
                  onClick: (e: Event) => handlePhoneContact(row, e),
                },
                { default: () => t('health.phoneFollow') }
              )
            );
          }
        }

        buttons.push(
          h(
            NButton,
            {
              size: 'small',
              type: 'primary',
              ghost: true,
              disabled: !row.archiveId,
              onClick: (e: Event) => {
                e.stopPropagation();
                router.push({ name: HealthRouteEnum.HEALTH_AI, query: { archiveId: row.archiveId } });
              },
            },
            { default: () => t('health.aiInterpretBtn') }
          )
        );

        return h(NSpace, { size: 'small' }, { default: () => buttons });
      },
    },
  ];

  function handleRowClick(row: MergedArchive) {
    if (!row.archiveId) return;
    router.push({
      name: HealthRouteEnum.HEALTH_EXAMINATION_DETAIL,
      params: { archiveId: row.archiveId },
    });
  }

  async function checkActionStatuses(archives: { archiveId: string }[]) {
    const ids = archives.filter((a) => a.archiveId).map((a) => a.archiveId);
    if (ids.length === 0) return;
    try {
      const res = await batchCheckActionStatus(ids);
      actionStatusMap.value = res || {};
    } catch (e) {
      console.error('[follow] batch-action-status failed:', e);
    }
  }

  function handlePhoneContact(row: MergedArchive, e: Event) {
    e.stopPropagation();
    const aid = row.archiveId;
    if (!aid) return;
    dialog.warning({
      title: t('health.phoneFollowDialogTitle'),
      content: t('health.phoneFollowDialogContent', { phone: row.phone || '未提供' }),
      positiveText: t('health.contacted'),
      negativeText: t('common.cancel'),
      onPositiveClick: async () => {
        try {
          await recordPhoneContact(aid, row.followInterval || 7);
          actionStatusMap.value[aid] = {
            ...actionStatusMap.value[aid],
            phoneContacted: true,
            phoneFrozen: true,
            phoneCooldownUntil: Date.now() + (row.followInterval || 7) * 86400000,
          };
          Message.success(t('health.phoneContactRecorded'));
        } catch (err: any) {
          console.error('[follow] phone contact failed:', err);
          Message.error(err?.message || '记录失败');
        }
      },
    });
  }

  async function loadMatchedArchives() {
    try {
      const res: any = await evaluateFollowRules();
      matchedArchives.value = res || [];
      if (matchedArchives.value.length > 0) {
        await checkActionStatuses(matchedArchives.value);
        const ids = matchedArchives.value.map((a: RuleMatchedArchive) => a.archiveId).filter(Boolean) as string[];
        if (ids.length > 0) {
          const status = await batchInterpretationStatus(ids);
          interpretedTypesMap.value = status || {};
        }
      }
    } catch (error: any) {
      console.error('[follow] evaluateFollowRules failed:', error);
      matchedArchives.value = [];
    }
  }

  provide('refreshMatchedArchives', loadMatchedArchives);

  onMounted(async () => {
    timerInterval = setInterval(() => {
      now.value = Date.now();
    }, 1000);
    await loadMatchedArchives();
  });

  onUnmounted(() => {
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
  });
</script>

<style lang="less" scoped>
  .health-follow {
    height: 100%;
    overflow: auto;
  }

  .rule-collapse {
    margin-bottom: 16px;
  }

  .match-collapse {
    margin-bottom: 16px;
  }

  .match-table {
    max-height: 360px;
  }

  .follow-table {
    margin-top: 8px;
  }

  .filter-row {
    margin-bottom: 12px;
  }
</style>
