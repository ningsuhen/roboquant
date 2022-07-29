/*
 * Copyright 2021 Neural Layer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.roboquant.jupyter

import org.apache.commons.math3.stat.descriptive.rank.Percentile
import org.icepear.echarts.Boxplot
import org.icepear.echarts.charts.boxplot.BoxplotSeries
import org.icepear.echarts.components.coord.cartesian.CategoryAxis
import org.icepear.echarts.components.coord.cartesian.ValueAxis
import org.icepear.echarts.components.dataZoom.DataZoom
import org.roboquant.common.clean
import org.roboquant.common.max
import org.roboquant.common.min
import org.roboquant.logging.*
import java.math.MathContext
import java.math.RoundingMode
import java.time.temporal.ChronoUnit

/**
 * A box chart is a standardized way of displaying data based on: the minimum, the maximum, and the
 * low-, mid- and high percentiles. It provides a good indication how a certain metric is distributed over a certain
 * period of time.
 */
class MetricBoxChart(
    private val metricData: Collection<MetricsEntry>,
    private val period: ChronoUnit = ChronoUnit.MONTHS,
    private val lowPercentile: Double = 25.0,
    private val midPercentile: Double = 50.0,
    private val highPercentile: Double = 75.0,
    private val precision : Int = 8
) : Chart() {

    private fun toSeriesData(): List<Pair<String, Any>> {
        val result = mutableListOf<Pair<String, Any>>()
        val ctx = MathContext(precision, RoundingMode.HALF_DOWN)
        for (d in metricData.groupBy(period)) {
            val arr = d.value.toDoubleArray().clean()
            if (arr.isNotEmpty()) {
                val p = Percentile()
                p.data = arr
                val entry = listOf(
                    arr.min().toBigDecimal(ctx),
                    p.evaluate(lowPercentile).toBigDecimal(ctx),
                    p.evaluate(midPercentile).toBigDecimal(ctx),
                    p.evaluate(highPercentile).toBigDecimal(ctx),
                    arr.max().toBigDecimal(ctx)
                )
                result.add(Pair(d.key, entry))
            }
        }
        result.sortBy { it.first }
        return result
    }

    /** @suppress */
    override fun renderOption(): String {
        val data = toSeriesData()
        val xData = data.map { it.first }.toTypedArray()
        val yData = data.map { it.second }

        val chart = Boxplot()
            .setTitle("Metric: ${metricData.getName()}")
            .addSeries(BoxplotSeries().setName("boxplot").setData(yData))
            .addYAxis(ValueAxis())
            .addXAxis(CategoryAxis().setData(xData))
            .setTooltip("axis")

        val option = chart.option
        option.setToolbox(getToolbox(false))
        option.setDataZoom(DataZoom())

        return renderJson(option)
    }
}