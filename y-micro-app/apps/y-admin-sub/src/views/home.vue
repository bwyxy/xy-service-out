<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'
import useThemeStore from '@/store/modules/theme'
import { getStatsOverview, getStatsDaily, getStatsTopPages, getStatsDevices } from '@/api/blog/stats'
import {
  whiteReliefStyle,
  darkReliefStyle,
  getBaseOption,
  applyLineSeries,
  applyBarSeries,
  applyPieSeries,
  applyAxisStyle
} from '@/utils/relief-chart'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const themeStore = useThemeStore()
// 根据当前主题自动切换浮雕配色
const style = computed(() => (themeStore.isDark ? darkReliefStyle : whiteReliefStyle))
watch(style, () => renderCharts())

// 页面加载状态
const loading = ref(false)
// 访问统计概览数据
const overview = ref<any>({})

// 图表容器引用
const dailyRef = ref<HTMLDivElement>()
const pageRef = ref<HTMLDivElement>()
const deviceRef = ref<HTMLDivElement>()

// ECharts 实例
let dailyChart: echarts.ECharts | null = null
let pageChart: echarts.ECharts | null = null
let deviceChart: echarts.ECharts | null = null

// 原始统计数据
const dailyData = ref<any[]>([])
const pageData = ref<any[]>([])
const deviceData = ref<any[]>([])

/**
 * 根据当前小时生成问候语
 */
const greeting = computed(() => {
  const hour = new Date().getHours()
  const name = userStore.userInfo?.nickname || userStore.userInfo?.username || ''
  let text = ''
  if (hour < 12) {
    text = '早上好'
  } else if (hour < 18) {
    text = '下午好'
  } else {
    text = '晚上好'
  }
  return name ? `${text}，${name}` : text
})

// 获取访问概览
const fetchOverview = async () => {
  const res = await getStatsOverview()
  if (res.code === 200) {
    overview.value = res.data || {}
  }
}

// 获取近7日访问数据
const fetchDaily = async () => {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  const fmt = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  const res = await getStatsDaily({ startDate: fmt(start), endDate: fmt(end) })
  if (res.code === 200) {
    dailyData.value = res.data || []
  }
}

// 获取热门页面排行
const fetchTopPages = async () => {
  const res = await getStatsTopPages({ topN: 10 })
  if (res.code === 200) {
    pageData.value = res.data || []
  }
}

// 获取设备分布
const fetchDevices = async () => {
  const res = await getStatsDevices({})
  if (res.code === 200) {
    deviceData.value = res.data || []
  }
}

/**
 * 根据当前主题使用对应浮雕风格渲染所有图表
 */
const renderCharts = () => {
  const currentStyle = style.value

  // ---------- 近7日访问趋势折线图 ----------
  const dates = dailyData.value.map(item => item.statDate)
  const pv = dailyData.value.map(item => item.pv || 0)
  const uv = dailyData.value.map(item => item.uv || 0)
  const dailyOption = {
    ...getBaseOption(currentStyle),
    tooltip: { trigger: 'axis' },
    legend: { data: ['PV', 'UV'], bottom: 0, textStyle: { color: currentStyle.textColor } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: { type: 'value' },
    series: applyLineSeries([
      { name: 'PV', type: 'line', data: pv },
      { name: 'UV', type: 'line', data: uv }
    ], currentStyle)
  }
  dailyChart?.setOption(applyAxisStyle(dailyOption, currentStyle), true)

  // ---------- 热门页面 TOP10 横向柱状图 ----------
  // 按 PV 升序排列，让最大值显示在柱状图顶部
  const sortedPages = [...pageData.value].sort((a, b) => (a.pv || 0) - (b.pv || 0))
  const paths = sortedPages.map(item => item.path || '-')
  const values = sortedPages.map(item => item.pv || 0)
  const pageOption = {
    ...getBaseOption(currentStyle),
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: paths },
    series: applyBarSeries([{ type: 'bar', data: values }], currentStyle)
  }
  pageChart?.setOption(applyAxisStyle(pageOption, currentStyle), true)

  // ---------- 设备分布环形饼图 ----------
  const deviceOption = {
    ...getBaseOption(currentStyle),
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, textStyle: { color: currentStyle.textColor } },
    series: applyPieSeries([{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      label: { show: true, formatter: '{b}: {c} ({d}%)', color: currentStyle.textColor },
      data: deviceData.value.map(item => ({ value: item.count || 0, name: item.device || '未知' }))
    }], currentStyle)
  }
  deviceChart?.setOption(deviceOption, true)
}

// 初始化 ECharts 实例
const initCharts = () => {
  if (dailyRef.value) dailyChart = echarts.init(dailyRef.value)
  if (pageRef.value) pageChart = echarts.init(pageRef.value)
  if (deviceRef.value) deviceChart = echarts.init(deviceRef.value)
}

// 销毁图表实例，防止内存泄漏
const disposeCharts = () => {
  dailyChart?.dispose()
  pageChart?.dispose()
  deviceChart?.dispose()
  dailyChart = null
  pageChart = null
  deviceChart = null
}

// 加载所有统计数据并渲染图表
const refresh = async () => {
  loading.value = true
  try {
    await Promise.all([fetchOverview(), fetchDaily(), fetchTopPages(), fetchDevices()])
    renderCharts()
  } catch (e) {
    ElMessage.error('统计数据加载失败')
  } finally {
    loading.value = false
  }
}

// 窗口大小变化时重绘图表
const onResize = () => {
  dailyChart?.resize()
  pageChart?.resize()
  deviceChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    initCharts()
    refresh()
    window.addEventListener('resize', onResize)
  })
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  disposeCharts()
})
</script>

<template>
  <div v-loading="loading" class="home-wrapper">
    <div class="home-content">
      <div class="greeting">
        <h1>{{ greeting }}</h1>
        <p class="greeting-sub">
          键盘敲落三更月，图表铺开万里春。
          数据无声皆战事，后台一盏守灯人。
        </p>
      </div>

      <!-- 概览数据卡片 -->
      <el-row :gutter="16" class="stat-cards">
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="hover">
            <div class="stat-title">总访问量 (PV)</div>
            <div class="stat-value">{{ overview.totalPv || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="hover">
            <div class="stat-title">总独立访客 (UV)</div>
            <div class="stat-value">{{ overview.totalUv || 0 }}</div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="hover">
            <div class="stat-title">今日 PV / UV</div>
            <div class="stat-value">
              {{ overview.todayPv || 0 }} / {{ overview.todayUv || 0 }}
            </div>
          </el-card>
        </el-col>
        <el-col :xs="12" :sm="12" :md="6">
          <el-card shadow="hover">
            <div class="stat-title">平均停留时长</div>
            <div class="stat-value">{{ overview.todayAvgDurationSec || 0 }}s</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 近7日访问趋势 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <span>近7日访问趋势</span>
            </template>
            <div ref="dailyRef" class="chart daily-chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 热门页面 + 设备分布 -->
      <el-row :gutter="16" class="chart-row">
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <span>热门页面 TOP10</span>
            </template>
            <div ref="pageRef" class="chart"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card shadow="hover" class="chart-card">
            <template #header>
              <span>设备分布</span>
            </template>
            <div ref="deviceRef" class="chart"></div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<style scoped lang="scss">
.home-wrapper {
  width: 100%;
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
  // 跟随系统主题切换背景色
  background-color: var(--app-bg-color);
  transition: background-color 0.4s ease;
}

.home-content {
  h1 {
    margin: 0;
    font-size: 24px;
    color: var(--app-text-color);
  }
}

// 问候语区域
.greeting {
  margin-bottom: 20px;

  h1 {
    font-size: 24px;
    color: var(--app-text-color);
    transition: color 0.4s ease;
  }

  .greeting-sub {
    margin: 6px 0 0;
    font-size: 14px;
    color: var(--app-text-secondary);
    transition: color 0.4s ease;
  }
}

.stat-cards {
  margin-bottom: 16px;
}

.stat-title {
  font-size: 13px;
  color: var(--app-text-secondary);
  margin-bottom: 8px;
  transition: color 0.4s ease;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--app-text-color);
  transition: color 0.4s ease;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  .chart {
    width: 100%;
    height: 320px;
  }
  .daily-chart {
    height: 360px;
  }
}
</style>
