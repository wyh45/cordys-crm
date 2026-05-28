<template>
  <div class="health-follow-rule">
    <div class="flex-between mb-4">
      <n-button type="primary" @click="handleAdd">
        <template #icon
          ><n-icon><AddOutline /></n-icon
        ></template>
        添加规则
      </n-button>
      <n-space>
        <n-input v-model:value="searchKeyword" placeholder="搜索规则名称" clearable style="width: 200px" />
        <n-button @click="fetchData">搜索</n-button>
      </n-space>
    </div>

    <n-data-table :columns="columns" :data="displayData" :loading="loading" :pagination="pagination" />

    <n-modal v-model:show="showModal" :title="isEdit ? '编辑规则' : '添加规则'" preset="card" style="width: 520px">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="top">
        <n-form-item label="规则名称" path="name">
          <n-input v-model:value="formData.name" placeholder="如: 血糖异常随访" />
        </n-form-item>
        <n-form-item label="关注检查项（逗号分隔）" path="watchExamItems">
          <n-input v-model:value="formData.watchExamItems" placeholder="如: 血糖,总胆固醇,甘油三酯" />
          <span class="form-tip">填写检查项关键词，匹配体检记录中包含该关键词的异常项</span>
        </n-form-item>
        <n-form-item label="最小异常项数" path="minAbnormalCount">
          <n-input-number v-model:value="formData.minAbnormalCount" :min="1" :max="50" style="width: 100%" />
          <span class="form-tip">客户异常检查项达到此数量时触发随访</span>
        </n-form-item>
        <n-form-item label="随访方式" path="followMethods">
          <n-checkbox-group v-model:value="formData.followMethods">
            <n-checkbox value="SMS">短信通知</n-checkbox>
            <n-checkbox value="PHONE">电话随访</n-checkbox>
          </n-checkbox-group>
        </n-form-item>
        <n-form-item label="随访间隔(天)" path="followInterval">
          <n-input-number v-model:value="formData.followInterval" :min="1" :max="365" style="width: 100%" />
        </n-form-item>
        <n-form-item label="是否启用">
          <n-switch v-model:value="formData.enabled" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, inject, ref } from 'vue';
  import {
    NButton,
    NCheckbox,
    NCheckboxGroup,
    NDataTable,
    NForm,
    NFormItem,
    NIcon,
    NInput,
    NInputNumber,
    NModal,
    NSpace,
    NSwitch,
    NTag,
    useDialog,
    useMessage,
  } from 'naive-ui';
  import { AddOutline } from '@vicons/ionicons5';

  import {
    deleteFollowRule,
    disableFollowRule,
    enableFollowRule,
    getFollowRulePage,
    saveFollowRule,
  } from '@/api/modules';

  const message = useMessage();
  const dialog = useDialog();
  const refreshMatchedArchives = inject<() => Promise<void>>('refreshMatchedArchives', async () => {});

  const loading = ref(false);
  const saving = ref(false);
  const showModal = ref(false);
  const isEdit = ref(false);
  const searchKeyword = ref('');
  const formRef = ref<any>(null);
  const tableData = ref<any[]>([]);

  const formData = ref({
    id: null as string | null,
    name: '',
    watchExamItems: '',
    minAbnormalCount: 1,
    followMethods: ['SMS'] as string[],
    followInterval: 7,
    enabled: true,
  });

  const rules: any = {
    name: { required: true, message: '请输入规则名称', trigger: 'blur' },
    followMethods: {
      type: 'array',
      required: true,
      message: '请选择随访方式',
      trigger: 'change',
      validator: (_rule: any, value: string[]) => value && value.length > 0,
    },
  };

  const pagination = { pageSize: 10 };

  const displayData = computed(() => {
    if (!searchKeyword.value) return tableData.value;
    return tableData.value.filter((item: any) => item.name?.includes(searchKeyword.value));
  });

  const followMethodLabel = (val: string) => {
    const map: Record<string, string> = { PHONE: '电话随访', SMS: '短信通知' };
    return map[val] || val;
  };

  const columns = [
    { title: '规则名称', key: 'name', width: 160 },
    {
      title: '关注检查项',
      key: 'watchExamItems',
      width: 220,
      render: (row: any) => {
        if (!row.watchExamItems) return '-';
        return h(
          NSpace,
          { size: 4, wrap: true },
          {
            default: () =>
              row.watchExamItems
                .split(',')
                .filter(Boolean)
                .map((item: string) => h(NTag, { type: 'info', size: 'small' }, { default: () => item.trim() })),
          }
        );
      },
    },
    { title: '最小异常数', key: 'minAbnormalCount', width: 100 },
    {
      title: '随访方式',
      key: 'followMethod',
      width: 140,
      render: (row: any) => {
        const methods = (row.followMethod || row.followType || '')
          .split(',')
          .map((s: string) => s.trim())
          .filter(Boolean);
        return h(
          NSpace,
          { size: 4, wrap: true },
          {
            default: () =>
              methods.map((m: string) =>
                h(
                  NTag,
                  { size: 'small', type: m === 'SMS' ? 'info' : 'warning' },
                  { default: () => followMethodLabel(m) }
                )
              ),
          }
        );
      },
    },
    {
      title: '间隔(天)',
      key: 'followInterval',
      width: 80,
      render: (row: any) => row.followInterval || row.followIntervalDays || 7,
    },
    {
      title: '状态',
      key: 'enabled',
      width: 80,
      render: (row: any) => h(NSwitch, { value: row.enabled, onUpdateValue: () => handleToggleEnabled(row) }),
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (row: any) =>
        h(
          NSpace,
          { size: 'small' },
          {
            default: () => [
              h(NButton, { size: 'small', type: 'info', onClick: () => handleEdit(row) }, { default: () => '编辑' }),
              h(NButton, { size: 'small', type: 'error', onClick: () => handleDelete(row) }, { default: () => '删除' }),
            ],
          }
        ),
    },
  ];

  const fetchData = async () => {
    loading.value = true;
    try {
      const res: any = await getFollowRulePage({ keyword: searchKeyword.value, page: 1, pageSize: 100 });
      tableData.value = Array.isArray(res) ? res : res?.list || res?.records || [];
    } catch {
      message.error('获取数据失败');
    } finally {
      loading.value = false;
    }
  };

  const handleAdd = () => {
    isEdit.value = false;
    formData.value = {
      id: null,
      name: '',
      watchExamItems: '',
      minAbnormalCount: 1,
      followMethods: ['SMS'],
      followInterval: 7,
      enabled: true,
    };
    showModal.value = true;
  };

  const handleEdit = (row: any) => {
    isEdit.value = true;
    const rawMethod = row.followMethod || row.followType || 'SMS';
    formData.value = {
      ...row,
      followMethods:
        typeof rawMethod === 'string'
          ? rawMethod
              .split(',')
              .map((s: string) => s.trim())
              .filter(Boolean)
          : rawMethod,
    };
    showModal.value = true;
  };

  const handleSave = async () => {
    try {
      await formRef.value?.validate();
    } catch {
      return;
    }
    saving.value = true;
    try {
      const payload = {
        ...formData.value,
        followMethod: formData.value.followMethods.join(','),
      };
      await saveFollowRule(payload);
      message.success('保存成功');
      showModal.value = false;
      fetchData();
      refreshMatchedArchives();
    } catch {
      message.error('保存失败');
    } finally {
      saving.value = false;
    }
  };

  const handleDelete = (row: any) => {
    dialog.warning({
      title: '确认删除',
      content: `确定要删除规则"${row.name}"吗？`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await deleteFollowRule(row.id);
          message.success('删除成功');
          fetchData();
          refreshMatchedArchives();
        } catch {
          message.error('删除失败');
        }
      },
    });
  };

  const handleToggleEnabled = async (row: any) => {
    try {
      if (row.enabled) {
        await disableFollowRule(row.id);
      } else {
        await enableFollowRule(row.id);
      }
      const item = tableData.value.find((i: any) => i.id === row.id);
      if (item) item.enabled = !row.enabled;
      message.success(item?.enabled ? '已启用' : '已禁用');
      refreshMatchedArchives();
    } catch {
      message.error('操作失败');
    }
  };

  fetchData();
</script>

<style lang="less" scoped>
  .health-follow-rule {
    padding: 16px;
  }
  .mb-4 {
    margin-bottom: 16px;
  }
  .flex-between {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .form-tip {
    font-size: 12px;
    color: var(--text-color-3);
    margin-top: 4px;
    display: block;
  }
</style>
