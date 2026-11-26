/**
 * ECharts 按需引入
 *
 * 之前 `import * as echarts from 'echarts'` 会把整个 ECharts（~1MB）打进包里，
 * 这里只注册项目实际用到的图表与组件，显著减小管理后台仪表盘的体积。
 * 新增图表类型时在下方 use([...]) 中补充即可。
 */
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { ComposeOption } from 'echarts/core'
import type { LineSeriesOption, BarSeriesOption } from 'echarts/charts'
import type {
  GridComponentOption,
  TooltipComponentOption,
  LegendComponentOption,
  TitleComponentOption
} from 'echarts/components'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

export type EChartsOption = ComposeOption<
  | LineSeriesOption
  | BarSeriesOption
  | GridComponentOption
  | TooltipComponentOption
  | LegendComponentOption
  | TitleComponentOption
>

export type ECharts = echarts.ECharts

export default echarts
