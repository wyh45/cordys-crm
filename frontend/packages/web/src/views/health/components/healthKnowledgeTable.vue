<template>
  <div class="health-knowledge-table">
    <div class="toolbar">
      <n-button type="primary" @click="handleAdd">
        {{ t('common.add') }}
      </n-button>
      <div class="toolbar-right">
        <n-select
          v-model:value="filterCategory"
          :placeholder="t('health.category')"
          :options="categoryOptions"
          clearable
          style="width: 160px"
          @update:value="searchData"
        />
        <n-input
          v-model:value="filterKeyword"
          :placeholder="t('health.semanticSearchPlaceholder')"
          clearable
          style="width: 280px"
          @keyup.enter="searchData"
          @clear="searchData"
        />
      </div>
    </div>

    <n-data-table
      :columns="columns"
      :data="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-key="(row: HealthKnowledgeListItem) => row.id"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />

    <n-modal v-model:show="modalVisible" :title="modalTitle" preset="card" style="width: 720px" :mask-closable="false">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="80">
        <n-form-item :label="t('health.knowledgeTitle')" path="title">
          <n-input v-model:value="formData.title" :placeholder="t('health.knowledgeTitlePlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.category')" path="category">
          <n-select
            v-model:value="formData.category"
            :placeholder="t('health.knowledgeCategoryPlaceholder')"
            :options="categoryOptions"
            filterable
            tag
          />
        </n-form-item>
        <n-form-item :label="t('health.tags')" path="tags">
          <n-input v-model:value="formData.tags" :placeholder="t('health.knowledgeTagsPlaceholder')" />
        </n-form-item>
        <n-form-item :label="t('health.content')" path="content">
          <n-input
            v-model:value="formData.content"
            type="textarea"
            :placeholder="t('health.knowledgeContentPlaceholder')"
            :autosize="{ minRows: 6, maxRows: 16 }"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="modalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="primary" :loading="saving" @click="handleSave">{{ t('common.confirm') }}</n-button>
        </div>
      </template>
    </n-modal>

    <n-modal
      v-model:show="deleteModalVisible"
      :title="t('common.deleteConfirmTitle', { name: deleteTarget?.title || '' })"
      preset="card"
      style="width: 420px"
      :mask-closable="false"
    >
      <p>{{ t('health.deleteKnowledgeConfirm') }}</p>
      <template #footer>
        <div class="modal-footer">
          <n-button @click="deleteModalVisible = false">{{ t('common.cancel') }}</n-button>
          <n-button type="error" :loading="!!deletingId" @click="confirmDelete">{{
            t('common.confirmDelete')
          }}</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
  import { computed, h, onMounted, ref } from 'vue';
  import { NButton, NDataTable, NForm, NFormItem, NInput, NModal, NSelect, NTag, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import type { HealthKnowledgeListItem } from '@/api/modules';
  import {
    deleteHealthKnowledge,
    getHealthKnowledgeCategories,
    getHealthKnowledgeList,
    saveHealthKnowledge,
  } from '@/api/modules';

  import type { DataTableColumns, FormInst, FormRules } from 'naive-ui';

  const { t } = useI18n();
  const Message = useMessage();

  const loading = ref(false);
  const saving = ref(false);
  const tableData = ref<HealthKnowledgeListItem[]>([]);
  const total = ref(0);
  const page = ref(1);
  const pageSize = ref(10);
  const filterKeyword = ref('');
  const filterCategory = ref<string | null>(null);
  const categories = ref<string[]>([]);

  const modalVisible = ref(false);
  const editingId = ref('');
  const formRef = ref<FormInst | null>(null);
  const formData = ref({
    title: '',
    category: '',
    tags: '',
    content: '',
  });

  const formRules: FormRules = {
    title: [{ required: true, message: () => t('health.knowledgeTitlePlaceholder'), trigger: 'blur' }],
    category: [{ required: true, message: () => t('health.knowledgeCategoryPlaceholder'), trigger: 'blur' }],
    content: [{ required: true, message: () => t('health.knowledgeContentPlaceholder'), trigger: 'blur' }],
  };

  const categoryOptions = computed(() => categories.value.map((c) => ({ label: c, value: c })));

  const pagination = computed(() => ({
    page: page.value,
    pageSize: pageSize.value,
    itemCount: total.value,
    showSizePicker: true,
    pageSizes: [10, 20, 50],
  }));

  const modalTitle = computed(() => (editingId.value ? t('health.editKnowledge') : t('health.addKnowledge')));

  const columns: DataTableColumns<HealthKnowledgeListItem> = [
    { title: t('health.knowledgeTitle'), key: 'title', width: 220, ellipsis: { tooltip: true } },
    { title: t('health.category'), key: 'category', width: 100 },
    {
      title: t('health.tags'),
      key: 'tags',
      width: 180,
      render(row) {
        if (!row.tags) return null;
        const tagList = typeof row.tags === 'string' ? row.tags.split(',') : row.tags;
        return h(
          'div',
          { style: { display: 'flex', gap: '4px', flexWrap: 'wrap' } },
          tagList
            .slice(0, 4)
            .map((tag: string) => h(NTag, { size: 'tiny', bordered: false, type: 'warning' }, () => tag.trim()))
        );
      },
    },
    {
      title: t('common.createTime'),
      key: 'createTime',
      width: 160,
      render(row) {
        if (!row.createTime) return null;
        return new Date(Number(row.createTime)).toLocaleString('zh-CN');
      },
    },
    {
      title: t('common.action'),
      key: 'actions',
      width: 140,
      render(row) {
        return h('div', { style: { display: 'flex', gap: '8px' } }, [
          h(NButton, { size: 'small', onClick: () => handleEdit(row) }, () => t('common.edit')),
          h(NButton, { size: 'small', type: 'error', ghost: true, onClick: () => handleDelete(row) }, () =>
            t('common.delete')
          ),
        ]);
      },
    },
  ];

  onMounted(() => {
    loadCategories();
    searchData();
  });

  async function loadCategories() {
    try {
      const res: any = await getHealthKnowledgeCategories();
      categories.value = res || [];
    } catch {
      // ignore
    }
  }

  async function searchData() {
    loading.value = true;
    try {
      const res: any = await getHealthKnowledgeList({
        page: page.value,
        pageSize: pageSize.value,
        keyword: filterKeyword.value || undefined,
        category: filterCategory.value || undefined,
      });
      tableData.value = (res?.list || res?.records || []) as HealthKnowledgeListItem[];
      total.value = (res?.total || 0) as number;
    } catch {
      // ignore
    } finally {
      loading.value = false;
    }
  }

  function handlePageChange(p: number) {
    page.value = p;
    searchData();
  }

  function handlePageSizeChange(ps: number) {
    pageSize.value = ps;
    page.value = 1;
    searchData();
  }

  function handleAdd() {
    editingId.value = '';
    formData.value = { title: '', category: filterCategory.value || '', tags: '', content: '' };
    modalVisible.value = true;
  }

  function handleEdit(row: HealthKnowledgeListItem) {
    editingId.value = row.id;
    formData.value = {
      title: row.title || '',
      category: row.category || '',
      tags: typeof row.tags === 'string' ? row.tags : (row.tags || []).join(','),
      content: row.content || '',
    };
    modalVisible.value = true;
  }

  async function handleSave() {
    try {
      await formRef.value?.validate();
    } catch {
      return;
    }
    saving.value = true;
    try {
      await saveHealthKnowledge({
        id: editingId.value || undefined,
        ...formData.value,
      });
      Message.success(t('common.saveSuccess'));
      modalVisible.value = false;
      loadCategories();
      searchData();
    } catch {
      Message.error(t('common.saveFailed'));
    } finally {
      saving.value = false;
    }
  }

  const deletingId = ref('');
  const deleteModalVisible = ref(false);
  const deleteTarget = ref<HealthKnowledgeListItem | null>(null);

  function handleDelete(row: HealthKnowledgeListItem) {
    deleteTarget.value = row;
    deleteModalVisible.value = true;
  }

  async function confirmDelete() {
    if (!deleteTarget.value) return;
    deletingId.value = deleteTarget.value.id;
    try {
      await deleteHealthKnowledge(deleteTarget.value.id);
      Message.success(t('common.deleteSuccess'));
      deleteModalVisible.value = false;
      searchData();
    } catch {
      Message.error(t('common.deleteFailed'));
    } finally {
      deletingId.value = '';
    }
  }
</script>

<style lang="less" scoped>
  .health-knowledge-table {
    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  }
</style>
