<template>
  <div class="health-knowledge">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pb-[16px] pt-[20px]">
        <div class="semantic-search-bar">
          <n-input-group>
            <n-input
              v-model:value="semanticQuery"
              :placeholder="t('health.semanticSearchPlaceholder')"
              clearable
              @keyup.enter="handleSemanticSearch"
            />
            <n-button type="primary" :loading="semanticSearching" @click="handleSemanticSearch">
              {{ t('health.semanticSearch') }}
            </n-button>
          </n-input-group>
        </div>

        <n-collapse v-if="semanticResults.length > 0" :default-expanded-names="['results']" class="search-results">
          <n-collapse-item name="results">
            <template #header>
              <div class="flex items-center gap-[8px]">
                <span>{{ t('health.searchResults') }}</span>
                <n-tag type="info" size="small" :bordered="false">{{ semanticResults.length }}</n-tag>
              </div>
            </template>
            <div class="result-list">
              <div v-for="item in semanticResults" :key="item.id" class="result-card">
                <div class="result-header">
                  <h4 class="result-title">{{ item.title }}</h4>
                  <n-tag v-if="item.category" size="small" :bordered="false" type="success">
                    {{ item.category }}
                  </n-tag>
                </div>
                <p v-if="item.content" class="result-content">
                  {{ item.content.length > 200 ? item.content.slice(0, 200) + '...' : item.content }}
                </p>
                <div v-if="item.tags" class="result-tags">
                  <n-tag
                    v-for="tag in item.tags.split(',').slice(0, 5)"
                    :key="tag"
                    size="tiny"
                    :bordered="false"
                    type="warning"
                  >
                    {{ tag.trim() }}
                  </n-tag>
                </div>
              </div>
            </div>
          </n-collapse-item>
        </n-collapse>

        <HealthKnowledgeTable />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { NButton, NCollapse, NCollapseItem, NInput, NInputGroup, NTag, useMessage } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import HealthKnowledgeTable from '../components/healthKnowledgeTable.vue';

  import type { HealthKnowledgeListItem } from '@/api/modules';
  import { searchHealthKnowledge } from '@/api/modules';

  const { t } = useI18n();
  const Message = useMessage();

  const semanticQuery = ref('');
  const semanticSearching = ref(false);
  const semanticResults = ref<HealthKnowledgeListItem[]>([]);

  async function handleSemanticSearch() {
    const q = semanticQuery.value.trim();
    if (!q) {
      Message.warning(t('health.semanticSearchPlaceholder'));
      return;
    }
    semanticSearching.value = true;
    try {
      const res: any = await searchHealthKnowledge(q, 10);
      semanticResults.value = res || [];
      if ((res || []).length === 0) {
        Message.info(t('health.noSearchResults'));
      }
    } catch {
      Message.error(t('common.searchFailed'));
    } finally {
      semanticSearching.value = false;
    }
  }
</script>

<style lang="less" scoped>
  .health-knowledge {
    height: 100%;
  }

  .semantic-search-bar {
    margin-bottom: 16px;
    max-width: 640px;
  }

  .search-results {
    margin-bottom: 16px;
  }

  .result-list {
    max-height: 480px;
    overflow-y: auto;
  }

  .result-card {
    padding: 12px 16px;
    border-bottom: 1px solid var(--divider-color);
    &:last-child {
      border-bottom: none;
    }
  }

  .result-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }

  .result-title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: var(--text-color-1);
  }

  .result-content {
    margin: 0 0 8px 0;
    font-size: 13px;
    color: var(--text-color-2);
    line-height: 1.6;
  }

  .result-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
</style>
