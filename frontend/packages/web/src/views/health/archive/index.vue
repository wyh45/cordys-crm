<template>
  <div class="health-archive absolute bottom-0 left-0 right-0 top-0">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pb-[24px] pt-[24px]">
        <HealthArchiveTable />
      </div>
    </CrmCard>
  </div>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import HealthArchiveTable from '../components/healthArchiveTable.vue';
</script>
