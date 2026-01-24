<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="400px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    center
  >
    <div class="flex flex-col items-center justify-center py-4">
      <el-progress
        type="circle"
        :percentage="percentage"
        :status="percentage === 100 ? 'success' : ''"
      />
      <p class="mt-4 text-gray-600 font-medium">{{ statusText }}</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  modelValue: boolean
  title?: string
  percentage: number
  statusText?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})
</script>

<style scoped>
.flex { display: flex; }
.flex-col { flex-direction: column; }
.items-center { align-items: center; }
.justify-center { justify-content: center; }
.py-4 { padding-top: 1rem; padding-bottom: 1rem; }
.mt-4 { margin-top: 1rem; }
.text-gray-600 { color: #4B5563; }
.font-medium { font-weight: 500; }
</style>
