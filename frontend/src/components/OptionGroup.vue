<script setup lang="ts">
import { computed } from 'vue'
import type { QuestionOptionVO } from '@/types'
import MarkdownRender from './MarkdownRender.vue'

const props = defineProps<{
  /** 1 单选 2 多选 3 判断 4 简答 */
  type: number
  options: QuestionOptionVO[] | null
  /** 单选/判断/多选：归一化字母串；简答：文本 */
  modelValue: string
  /** 是否禁用（判分后回显用） */
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const selectedCodes = computed(() => (props.type === 2 ? props.modelValue.split('') : [props.modelValue]))

function toggle(code: string) {
  if (props.disabled) return
  if (props.type === 2) {
    const set = new Set(selectedCodes.value)
    if (set.has(code)) {
      set.delete(code)
    } else {
      set.add(code)
    }
    // 多选归一化为排序后拼接的字母串再提交（BD 与 DB 等价）
    emit('update:modelValue', [...set].sort().join(''))
  } else {
    emit('update:modelValue', code)
  }
}
</script>

<template>
  <!-- 简答题：文本作答（后端 SubmitDTO @Size(max=500) 与列 VARCHAR(500) 一致，前端同步限制） -->
  <textarea
    v-if="type === 4"
    class="short-answer"
    :value="modelValue"
    :disabled="disabled"
    maxlength="500"
    rows="6"
    placeholder="请输入你的作答内容（最多 500 字；简答题不自动判分，提交后可查看参考答案）"
    @input="emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
  ></textarea>

  <!-- 客观题：单选/判断 = radio 样式，多选 = checkbox 样式 -->
  <div v-else class="option-group">
    <div
      v-for="opt in options ?? []"
      :key="opt.optionCode"
      class="option-item"
      :class="{ selected: selectedCodes.includes(opt.optionCode), disabled }"
      @click="toggle(opt.optionCode)"
    >
      <span class="marker" :class="type === 2 ? 'square' : 'circle'"></span>
      <span class="code">{{ opt.optionCode }}</span>
      <div class="option-content">
        <MarkdownRender :content="opt.optionContent" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  background: #fff;
}

.option-item:hover:not(.disabled) {
  border-color: #18a058;
}

.option-item.selected {
  border-color: #18a058;
  background: #f0faf4;
}

.option-item.disabled {
  cursor: default;
}

.marker {
  flex-shrink: 0;
  margin-top: 5px;
  width: 14px;
  height: 14px;
  border: 1.5px solid #c2c4cc;
  position: relative;
}

.marker.circle {
  border-radius: 50%;
}

.marker.square {
  border-radius: 3px;
}

.option-item.selected .marker {
  border-color: #18a058;
  background: #18a058;
}

.option-item.selected .marker::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 1px;
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.code {
  font-weight: 600;
  color: #18a058;
  margin-top: 1px;
}

.option-content {
  flex: 1;
  min-width: 0;
}

.short-answer {
  width: 100%;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.short-answer:focus {
  border-color: #18a058;
}
</style>
