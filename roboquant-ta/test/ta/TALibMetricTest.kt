package org.roboquant.ta

import org.junit.Test
import kotlin.test.assertTrue

class TALibMetricTest {

    @Test
    fun test() {
        val metric = TALibMetric("ema50",50) { series ->
            ema(series.close, 50)
        }
        assertTrue(metric.getMetrics().isEmpty())
    }
}