import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

function ActivityXAxisTick({ x, y, payload }) {
  const [day, date] = String(payload.value).split('|')

  return (
    <g transform={`translate(${x},${y})`}>
      <text fill="#657572" fontSize="11" textAnchor="middle">
        <tspan x="0" dy="14">{day}</tspan>
        <tspan x="0" dy="15">{date}</tspan>
      </text>
    </g>
  )
}

function ActivityTooltip({ active, payload }) {
  if (!active || !payload?.length) return null

  const day = payload[0].payload
  return (
    <div className="activity-tooltip">
      <strong>{day.day}, {day.date}</strong>
      {day.workers.length === 0 ? (
        <span>No submissions</span>
      ) : (
        <ul>
          {day.workers.map((worker) => (
            <li key={worker.id}>
              <span>{worker.name}</span>
              <b>{worker.count}</b>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

function SubmissionActivityChart({ activity }) {
  const activityAxisMaximum = Math.max(
    4,
    ...activity.map((day) => day.count),
  )
  const activityTicks = Array.from(
    { length: activityAxisMaximum + 1 },
    (_, index) => index,
  )
  const chartData = activity.map((day) => ({
    ...day,
    label: `${day.day}|${day.date}`,
  }))

  return (
    <div
      className="activity-chart"
      role="img"
      aria-label="Bar chart showing submission totals for the last seven days"
    >
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={chartData}
          margin={{ top: 8, right: 4, bottom: 0, left: 0 }}
          accessibilityLayer
        >
          <CartesianGrid vertical={false} stroke="#d8e0dd" />
          <XAxis
            dataKey="label"
            axisLine={{ stroke: '#b9c7c3' }}
            height={44}
            interval={0}
            tick={<ActivityXAxisTick />}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            axisLine={false}
            domain={[0, activityAxisMaximum]}
            label={{
              value: 'Submissions',
              angle: -90,
              position: 'insideLeft',
              fill: '#657572',
              fontSize: 12,
            }}
            tick={{ fill: '#657572', fontSize: 11 }}
            tickLine={false}
            ticks={activityTicks}
            width={44}
          />
          <Tooltip
            content={<ActivityTooltip />}
            cursor={false}
            shared={false}
          />
          <Bar
            dataKey="count"
            fill="#025539"
            isAnimationActive={false}
            maxBarSize={56}
            radius={[2, 2, 0, 0]}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  )
}

export default SubmissionActivityChart
