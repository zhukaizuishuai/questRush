<script setup lang="ts">
defineProps<{
  /** 0 免费 1 VIP 专属 */
  isVip?: number
}>()
</script>

<template>
  <span v-if="isVip === 1" class="vip-badge" title="VIP 专属题目，开通会员可查看">VIP</span>
</template>

<style scoped>
.vip-badge {
  display: inline-block;
  padding: 0 6px;
  height: 20px;
  line-height: 20px;
  font-size: 12px;
  font-weight: 600;
  color: #8a5a00;
  background: linear-gradient(135deg, #ffe9b8, #ffd77a);
  border-radius: 4px;
  cursor: default;
  flex-shrink: 0;
}
</style>
