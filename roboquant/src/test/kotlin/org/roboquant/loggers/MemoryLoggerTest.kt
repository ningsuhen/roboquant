/*
 * Copyright 2020-2023 Neural Layer
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

package org.roboquant.loggers

import kotlinx.coroutines.launch
import org.junit.jupiter.api.assertThrows
import org.roboquant.TestData
import org.roboquant.common.*
import org.roboquant.metrics.metricResultsOf
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class MemoryLoggerTest {

    @Test
    fun memoryLogger() {
        val logger = MemoryLogger(showProgress = false)
        assertTrue(logger.metricNames.isEmpty())

        val metrics = TestData.getMetrics()

        logger.start("test", Timeframe.INFINITE)
        logger.log(metrics, Instant.now(), "test")
        logger.end("test")
        assertFalse(logger.metricNames.isEmpty())
        assertEquals(metrics.size, logger.metricNames.size)

        val t = logger.getMetric(metrics.keys.first()).latestRun()
        assertEquals(1, t.size)

        val z = logger.getMetric(metrics.keys.first()).latestRun()
        assertEquals(1, z.size)


        assertEquals(1, logger.runs.size)

        assertTrue(z.min().value <= z.max().value)

        repeat(4) {
            logger.log(metrics, Instant.now(), "test")
        }
    }

    @Test
    fun concurrency() {
        val logger = MemoryLogger(false)
        val jobs = ParallelJobs()
        Timeframe.fromYears(2000, 2002).split(1.months).forEach {
            jobs.add {
                launch {
                    it.sample(1.days, 100).forEach { tf ->
                        val run = "run-$tf"
                        logger.start(run, tf)
                        repeat(1000) {
                            logger.log(mapOf("a" to 100.0 + it), tf.start + it.seconds, run)
                        }
                        logger.end(run)
                    }
                }
            }

        }
        jobs.joinAllBlocking()
        val data = logger.getMetric("a")
        for (ts in data.values) {
            assertEquals(1000, ts.size)
            assertEquals(100.0, ts.values[0])
        }

    }


    @Test
    fun testMetricsEntry() {
        val logger = MemoryLogger(showProgress = false)
        logger.start("test", Timeframe.INFINITE)
        repeat(12) {
            val metrics = metricResultsOf("key1" to it)
            logger.log(metrics, Instant.now(), "test")
        }
        val data = logger.getMetric("key1").latestRun()
        assertEquals(12, data.size)
        val dataDiff = data.diff()
        assertEquals(11, dataDiff.size)
        assertEquals(1.0, dataDiff.first().value)

        val dataPerc = data.returns()
        assertEquals(11, dataPerc.size)
        assertTrue((dataPerc.last().value - 0.1).absoluteValue < 0.0000001)

    }

    @Test
    fun groupBy() {
        val logger = MemoryLogger(showProgress = false)
        logger.start("test", Timeframe.INFINITE)

        var start = Instant.parse("2022-01-01T00:00:00Z")
        repeat(50) {
            val metrics = metricResultsOf("key1" to it)
            logger.log(metrics, start, "test")
            start += 2.days
        }

        val data = logger.getMetric("key1").latestRun()
        assertEquals(50, data.size)

        assertEquals(50, data.groupBy(ChronoUnit.MINUTES).size)
        assertEquals(50, data.groupBy(ChronoUnit.DAYS).size)
        assertEquals(15, data.groupBy(ChronoUnit.WEEKS).size)
        assertEquals(4, data.groupBy(ChronoUnit.MONTHS).size)
        assertEquals(1, data.groupBy(ChronoUnit.YEARS).size)
        assertEquals(50, data.groupBy(ChronoUnit.YEARS).values.first().size)

        assertThrows<IllegalArgumentException> {
            data.groupBy(ChronoUnit.MICROS)
        }
    }

    @Test
    fun groupAndFlatten() {
        val logger = MemoryLogger(showProgress = false)
        val now = Instant.now()

        repeat(50) {
            val run = "run-$it"
            logger.start(run, Timeframe.INFINITE)
            val metrics = metricResultsOf("key1" to it)
            logger.log(metrics, now + it.days, run)
            logger.log(metrics, now + it.days + 1.days, run)
        }

        val data = logger.getMetric("key1")
        assertEquals(100, data.values.flatten().size)
        assertEquals(50, data.size)

        val earliest = data.earliestRun().first().value
        assertEquals(0.0, earliest)

        val latest = data.latestRun().first().value
        assertEquals(49.0, latest)

    }


}