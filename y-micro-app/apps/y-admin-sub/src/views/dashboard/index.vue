<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue';
import { Gantt } from '@dizzy_yakov/vue-gantt'
import '@dizzy_yakov/vue-gantt/styles'
import {Plus} from "@element-plus/icons-vue";
import { getDateOffset, formatDate } from '@/utils/utils.ts'
import { listScheduleTasks, getTaskStatistics } from '@/api/todo/task'
import { ElMessage } from 'element-plus'

const monthMap: Record<string, string> = {
  Jan: '1月', Feb: '2月', Mar: '3月', Apr: '4月',
  May: '5月', Jun: '6月', Jul: '7月', Aug: '8月',
  Sep: '9月', Oct: '10月', Nov: '11月', Dec: '12月',
}

interface GanttTask {
  id: string
  name: string
  start: string
  end: string
  progress?: number
  resourceIds?: string[]
  color?: string
}

interface GanttRow {
  id: string
  name: string
  tasks: GanttTask[]
}

const ganttRefs = ref()
const timerNum = ref<number>(30)
const loading = ref(false)
const statistics = reactive({
  total: 0,
  completed: 0,
  pending: 0,
  overdue: 0
})

const timers = [
  { label: '30天', value: 30 },
  { label: '60天', value: 60 },
  { label: '近一年', value: 365 }
]

const rows = ref<GanttRow[]>([])

const startTime = computed(() => {
  const difference = Math.floor(timerNum.value / 2)
  const before = getDateOffset(-difference)
  return formatDate(before)
})

const endTime = computed(() => {
  const difference = Math.floor(timerNum.value / 2)
  const after = getDateOffset(difference)
  return formatDate(after)
})

const urgencyColorMap: Record<number, string> = {
  1: '#94a3b8',
  2: '#f59e0b',
  3: '#3b82f6',
  4: '#ef4444'
}

const loadStatistics = async () => {
  try {
    const res = await getTaskStatistics()
    if (res.code === 200) {
      Object.assign(statistics, res.data)
    }
  } catch (e) {
    console.error('加载统计失败', e)
  }
}

const toGanttTask = (task: any): GanttTask => {
  const urgency = task.urgencyLevel || 1
  const startDate = task.startTime ? task.startTime.split(' ')[0] : formatDate(getDateOffset(0))
  const endDate = task.dueDate ? task.dueDate.split(' ')[0] : startDate
  const progress = task.completed === 1 ? 1 : 0
  return {
    id: String(task.taskId),
    name: task.taskTitle,
    start: startDate,
    end: endDate,
    progress,
    color: urgencyColorMap[urgency] || '#3b82f6'
  }
}

const buildTaskRows = (tasks: any[]): GanttRow[] => {
  if (!tasks.length) return []

  const taskMap: Record<string, any> = {}
  const childrenMap: Record<string, any[]> = {}

  tasks.forEach((task: any) => {
    taskMap[task.taskId] = task
    if (task.parentId) {
      if (!childrenMap[task.parentId]) childrenMap[task.parentId] = []
      childrenMap[task.parentId].push(task)
    }
  })

  const rows: GanttRow[] = []
  const processedChildren = new Set<string>()

  tasks.forEach((task: any) => {
    if (task.parentId) return

    const children = childrenMap[task.taskId] || []
    if (children.length > 0) {
      const completed = children.filter((t: any) => t.completed === 1).length
      rows.push({
        id: `parent-${task.taskId}`,
        name: `${task.taskTitle} (${completed}/${children.length})`,
        tasks: children.map(toGanttTask)
      })
      children.forEach((child: any) => processedChildren.add(String(child.taskId)))
    } else {
      rows.push({
        id: String(task.taskId),
        name: task.taskTitle,
        tasks: [toGanttTask(task)]
      })
    }
  })

  // 父任务不在查询结果中（但其子任务在）时，兜底把子任务作为独立行展示
  tasks.forEach((task: any) => {
    if (!task.parentId) return
    if (processedChildren.has(String(task.taskId))) return
    rows.push({
      id: String(task.taskId),
      name: task.taskTitle,
      tasks: [toGanttTask(task)]
    })
  })

  return rows
}

const loadScheduleTasks = async () => {
  loading.value = true
  try {
    const start = formatDate(getDateOffset(-Math.floor(timerNum.value / 2))) + ' 00:00:00'
    const end = formatDate(getDateOffset(Math.floor(timerNum.value / 2))) + ' 23:59:59'
    const res = await listScheduleTasks({ start, end })
    if (res.code === 200) {
      rows.value = buildTaskRows(res.data || [])
    } else {
      ElMessage.error(res.msg || '加载日程数据失败')
    }
  } catch (e) {
    console.error('加载日程数据失败', e)
  } finally {
    loading.value = false
  }
}

const handleScrollTimer = () => {
  loadScheduleTasks()
  setTimeout(() => {
    if (ganttRefs.value) {
      const day = getDateOffset(-12)
      ganttRefs.value.scrollToDate(formatDate(day))
    }
  }, 80)
}

onMounted(() => {
  loadStatistics()
  loadScheduleTasks()
})
</script>

<template>
  <div class="dashboard">
    <div class="dashboard-lt">
      <div class="statistics-bar">
        <div class="stat-card stat-total">
          <div class="stat-label">任务总数</div>
          <div class="stat-value">{{ statistics.total }}</div>
        </div>
        <div class="stat-card stat-completed">
          <div class="stat-label">已完成</div>
          <div class="stat-value">{{ statistics.completed }}</div>
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-label">待完成</div>
          <div class="stat-value">{{ statistics.pending }}</div>
        </div>
        <div class="stat-card stat-overdue">
          <div class="stat-label">已逾期</div>
          <div class="stat-value">{{ statistics.overdue }}</div>
        </div>
      </div>

      <div class="dashboard-lt-header">
        <div style="flex: 1">
          <el-button type="primary" plain :icon="Plus" @click="$router.push('/todo/task')">添加事项</el-button>
        </div>
        <el-segmented
            v-model="timerNum"
            :options="timers"
            @change="handleScrollTimer"
        ></el-segmented>
      </div>
      <div class="dashboard-lt-content" v-loading="loading">
        <div class="gantt-wrapper">
          <Gantt
              ref="ganttRefs"
              :rows="rows"
              :tiers="['month', 'week', 'day']"
              :start-date="startTime"
              :end-date="endTime"
          >
            <template #column="{ column, tier } :any">
              <span v-if="tier === 'month'">
                {{ monthMap[column.label] ?? column.label }}
              </span>
              <span v-else-if="tier === 'week' && column.label.startsWith('W')">
                第{{ column.label.slice(1) }}周
              </span>
              <span v-else>{{ column.label }}</span>
            </template>
            <template #bar="{ task , progress } :any">
              <div class="custom-bar" :style="{'--color': task.color || '#722ed1'}">
                <span class="bar-name">{{ task ? (task.name || '') : '' }}</span>
                <span class="bar-progress">{{ Math.round(progress * 100) }}%</span>
              </div>
            </template>
            <template #tooltip="{ task } :any">
              <div>任务：{{ task.name }}</div>
              <div>时间：{{ task.start }} 至 {{ task.end }}</div>
            </template>
          </Gantt>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  // 主题色变量：跟随全局主题自动切换
  --db-bg: var(--app-card-bg, #ffffff);
  --db-text: var(--app-text-color, #303133);
  --db-text-secondary: var(--app-text-secondary, #606266);
  --db-border: var(--app-border-color, #e4e7ed);

  width: 100%;
  height: 100%;
  display: flex;
  overflow: hidden;
}

.statistics-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 13px;
  margin-bottom: 13px;

  .stat-card {
    background: var(--db-bg);
    border: 1px solid var(--db-border);
    border-radius: 13px;
    padding: 16px;
    transition: background-color 0.4s ease, border-color 0.4s ease;

    .stat-label {
      font-size: 14px;
      color: var(--db-text-secondary);
      margin-bottom: 8px;
      transition: color 0.4s ease;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 600;
      transition: color 0.4s ease;
    }
  }

  .stat-total .stat-value { color: #3b82f6; }
  .stat-completed .stat-value { color: #10b981; }
  .stat-pending .stat-value { color: #f59e0b; }
  .stat-overdue .stat-value { color: #ef4444; }
}

.dashboard-lt {
  flex: 1;
  background: var(--db-bg);
  border: 1px solid var(--db-border);
  border-radius: 13px;
  box-sizing: border-box;
  padding: 13px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: background-color 0.4s ease, border-color 0.4s ease;

  .dashboard-lt-header {
    display: flex;
  }

  .dashboard-lt-content {
    flex: 1;
    overflow: hidden;
  }
}

.dashboard-lt-header {
  margin-bottom: 13px;
}

.gantt-wrapper {
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  border: 1px solid var(--db-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--db-bg);
  color: var(--db-text);
  transition: background-color 0.4s ease, border-color 0.4s ease, color 0.4s ease;

  // 甘特图组件主题变量（浅色默认值）
  --gantt-surface: var(--db-bg);
  --gantt-grid-color: var(--db-border);
  --gantt-group-header-bg: #f8fafc;
  --gantt-bar-bg: #3b82f6;
  --gantt-bar-height: 24px;
  --gantt-bar-radius: 4px;
  --gantt-bar-color: #ffffff;
  --gantt-bar-font-size: 12px;
  --gantt-progress-color: rgba(255, 255, 255, 0.3);
  --gantt-progress-handle-color: #ffffff;
  --gantt-ghost-opacity: 0.55;
  --gantt-drag-label-bg: #1e293b;
  --gantt-drag-label-color: #ffffff;
  --gantt-drag-label-radius: 4px;
  --gantt-tooltip-bg: #1e293b;
  --gantt-tooltip-color: #ffffff;
  --gantt-tooltip-radius: 6px;
  --gantt-tooltip-shadow: 0 4px 12px rgb(0 0 0 / 15%);
  --gantt-critical-color: #dc2626;
  --gantt-slack-color: #94a3b8;
  --gantt-today-color: #ef4444;
  --gantt-marker-color: #6b7280;
  --gantt-nonworking-bg: #f3f4f6;
  --gantt-summary-bar-progress-bg: #94a3b8;
  --gantt-summary-bar-height: 40%;
  --gantt-summary-bar-radius: 3px;
  --gantt-period-band-bg: rgb(99 102 241 / 4%);
  --gantt-period-border: 1px dashed var(--db-border);
  --gantt-conflict-color: #ef4444;
}

// 深色主题下覆盖甘特图变量
:global(html.dark .gantt-wrapper) {
  --gantt-surface: #1f1f1f;
  --gantt-grid-color: #434343;
  --gantt-group-header-bg: #2c2c2c;
  --gantt-bar-bg: #4b5563;
  --gantt-bar-color: #e0e0e0;
  --gantt-progress-color: rgba(0, 0, 0, 0.25);
  --gantt-progress-handle-color: #e0e0e0;
  --gantt-drag-label-bg: #2c2c2c;
  --gantt-drag-label-color: #e0e0e0;
  --gantt-tooltip-bg: #2c2c2c;
  --gantt-tooltip-color: #e0e0e0;
  --gantt-marker-color: #a0a0a0;
  --gantt-nonworking-bg: #2c2c2c;
  --gantt-period-band-bg: rgb(255 255 255 / 4%);
  --gantt-period-border: 1px dashed #434343;
}

:deep(.gantt-root) {
  width: 100% !important;
}

:deep(.gantt__head-main) {
  flex: 1;
}

:deep(.gantt__body) {
  flex: 1;
}
:deep(.gantt-timeline__cell) {
  display: flex;
  justify-content: center !important;
}

:deep(.gantt-bar) {
  background: none !important;
  border-radius: 0 !important;
}

.my-bar {
  width: 100%;
  height: 100%;
  border-radius: 4px;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  padding: 0 8px;
  box-sizing: border-box;
}

.custom-bar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
  border-radius: 0 !important;
  color: #fff;
  font-size: 12px;
  overflow: hidden;
  white-space: nowrap;
  background: none !important;
  box-sizing: border-box;
  &:after {
    content: '';
    width: 100%;
    height: 100%;
    background: var(--color);
    opacity: .6;
    position: absolute;
    top: 0;
    left: 0;
  }
  &:before {
    content: '';
    width: 4px;
    height: 100%;
    background: var(--color);
    position: absolute;
    left: 0;
    top: 0;
  }
}

.bar-name {
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
}

.bar-progress {
  flex-shrink: 0;
  margin-left: 4px;
  font-weight: 500;
}

@media (max-width: 768px) {
  .statistics-bar {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
