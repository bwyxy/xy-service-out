import type { EChartsOption } from 'echarts'

export interface ReliefStyle {
  name: string
  value: string
  bgColor: string
  textColor: string
  axisColor: string
  palette: string[]
}

export const whiteReliefStyle: ReliefStyle = {
  name: '白色浮雕',
  value: 'white',
  bgColor: '#ffffff',
  textColor: '#303133',
  axisColor: '#dcdfe6',
  palette: ['#ffffff', '#f2f4f7', '#e4e7ec', '#d1d5db', '#9ca3af']
}

export const darkReliefStyle: ReliefStyle = {
  name: '暗色浮雕',
  value: 'dark',
  bgColor: '#1f1f1f',
  textColor: '#e0e0e0',
  axisColor: '#434343',
  palette: ['#4b5563', '#374151', '#2c2c2c', '#1f1f1f', '#111827']
}

function linearGradient(colorStart: string, colorEnd: string, x = 0, y = 0, x2 = 0, y2 = 1) {
  return {
    type: 'linear' as const,
    x,
    y,
    x2,
    y2,
    colorStops: [
      { offset: 0, color: colorStart },
      { offset: 1, color: colorEnd }
    ]
  }
}

function reliefShadow() {
  return {
    shadowBlur: 10,
    shadowColor: 'rgba(0, 0, 0, 0.2)',
    shadowOffsetX: 3,
    shadowOffsetY: 4
  }
}

export function getBaseOption(style: ReliefStyle): EChartsOption {
  return {
    backgroundColor: style.bgColor,
    textStyle: { color: style.textColor },
    title: { textStyle: { color: style.textColor } },
    legend: { textStyle: { color: style.textColor } },
    tooltip: {
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: style.axisColor,
      textStyle: { color: style.textColor },
      extraCssText: 'box-shadow: 0 4px 12px rgba(0,0,0,0.12);'
    }
  }
}

export function applyLineSeries(series: any[], style: ReliefStyle) {
  return series.map((s, index) => {
    const light = style.palette[index % style.palette.length]
    const dark = style.palette[Math.min(index + 2, style.palette.length - 1)]
    return {
      ...s,
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: {
        width: 4,
        color: linearGradient(light, dark, 0, 0, 1, 0),
        shadowBlur: 6,
        shadowColor: 'rgba(0,0,0,0.15)',
        shadowOffsetY: 3
      },
      itemStyle: {
        color: linearGradient(light, dark),
        borderColor: '#fff',
        borderWidth: 2,
        ...reliefShadow()
      },
      areaStyle: {
        color: linearGradient(light, style.bgColor, 0, 0, 0, 1),
        opacity: 0.35
      }
    }
  })
}

export function applyBarSeries(series: any[], style: ReliefStyle) {
  return series.map(s => {
    const light = style.palette[0]
    const dark = style.palette[style.palette.length - 1]
    return {
      ...s,
      itemStyle: {
        color: linearGradient(light, dark, 0, 0, 0, 1),
        borderRadius: [0, 6, 6, 0],
        borderColor: 'rgba(255,255,255,0.5)',
        borderWidth: 1,
        ...reliefShadow()
      },
      emphasis: {
        itemStyle: {
          color: linearGradient(dark, light, 0, 0, 0, 1),
          shadowBlur: 16,
          shadowColor: 'rgba(0,0,0,0.18)'
        }
      }
    }
  })
}

export function applyPieSeries(series: any[], style: ReliefStyle) {
  return series.map(s => {
    const data = (s.data || []).map((item: any, index: number) => {
      const light = style.palette[index % style.palette.length]
      const dark = style.palette[Math.min(index + 1, style.palette.length - 1)]
      return {
        ...item,
        itemStyle: {
          color: linearGradient(light, dark),
          borderRadius: 6,
          borderColor: 'rgba(255,255,255,0.5)',
          borderWidth: 2,
          ...reliefShadow()
        }
      }
    })
    return { ...s, data }
  })
}

export function applyAxisStyle(option: any, style: ReliefStyle) {
  const axisCommon = {
    axisLine: { lineStyle: { color: style.axisColor, width: 2 } },
    axisTick: { lineStyle: { color: style.axisColor } },
    axisLabel: { color: style.textColor },
    splitLine: { lineStyle: { color: style.axisColor, type: 'dashed' as const, opacity: 0.4 } }
  }
  if (option.xAxis) {
    option.xAxis = Array.isArray(option.xAxis)
      ? option.xAxis.map((a: any) => ({ ...a, ...axisCommon }))
      : { ...option.xAxis, ...axisCommon }
  }
  if (option.yAxis) {
    option.yAxis = Array.isArray(option.yAxis)
      ? option.yAxis.map((a: any) => ({ ...a, ...axisCommon }))
      : { ...option.yAxis, ...axisCommon }
  }
  return option
}
