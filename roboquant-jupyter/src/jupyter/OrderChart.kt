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

import org.icepear.echarts.Scatter
import org.icepear.echarts.charts.scatter.ScatterSeries
import org.icepear.echarts.components.coord.cartesian.TimeAxis
import org.icepear.echarts.components.coord.cartesian.ValueAxis
import org.icepear.echarts.components.dataZoom.DataZoom
import org.icepear.echarts.components.tooltip.Tooltip
import org.roboquant.common.UnsupportedException
import org.roboquant.orders.OrderState
import org.roboquant.orders.OrderStatus
import org.roboquant.orders.SingleOrder
import java.math.BigDecimal
import java.time.Instant

/**
 * Order chart plots [orders] over time. By default, the quantity will be plotted, but you can change the [aspect]
 * to plot other properties of an order ("remaining", "direction", "quantity", "fill").
 *
 * Please not this chart only display orders of the type [SingleOrder] and will ignore other order types. Often trades
 * provide more insights, since these also cover advanced order types. You can use the [TradeChart] for that.
 */
class OrderChart(
    private val orderStates: List<OrderState>,
    private val aspect: String = "quantity",
) : Chart() {

    init {
        require(aspect in listOf("direction", "quantity"))
    }

    @Suppress("MaxLineLength")
    private fun getTooltip(order: SingleOrder, openedAt: Instant): String {
        return with(order) {
            "asset: ${asset.symbol}<br> currency: ${asset.currency}<br> placed: $openedAt<br> qty: ${order.size}<br> id: $id<br> type: ${order::class.simpleName}<br> tif: ${order.tif}"
        }
    }

    private fun toSeriesData(): List<Triple<Instant, BigDecimal, String>> {
        val states = orderStates.filter { it.status != OrderStatus.INITIAL }
        val d = mutableListOf<Triple<Instant, BigDecimal, String>>()
        for (state in states.sortedBy { it.openedAt }) {
            val order = state.order
            if (order is SingleOrder) {
                val value = when (aspect) {
                    "direction" -> order.direction.toBigDecimal()
                    "quantity" -> order.size.toBigDecimal()
                    else -> throw UnsupportedException("Unsupported aspect $aspect")
                }

                val tooltip = getTooltip(order, state.openedAt)
                d.add(Triple(state.openedAt, value, tooltip))
            }
        }

        return d
    }

    /** @suppress */
    override fun renderOption(): String {

        val data = toSeriesData()
        val max = data.maxOfOrNull { it.second.abs() } ?: BigDecimal.ONE

        val series = ScatterSeries()
            .setData(data)
            .setSymbolSize(10)

        val vm = getVisualMap(-max, max).setDimension(1)

        val tooltip = Tooltip()
            .setFormatter(javasciptFunction("return p.value[2];"))

        val chart = Scatter()
            .setTitle("Order Chart $aspect")
            .addXAxis(TimeAxis())
            .addYAxis(ValueAxis().setScale(true))
            .addSeries(series)
            .setVisualMap(vm)
            .setTooltip(tooltip)

        val option = chart.option
        option.setToolbox(getToolbox(includeMagicType = false))
        option.setDataZoom(DataZoom())

        return renderJson(option)
    }
}