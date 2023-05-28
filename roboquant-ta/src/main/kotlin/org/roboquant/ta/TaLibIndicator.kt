/*
 * Copyright 2020-2023 Neural Layer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.roboquant.ta

import org.roboquant.feeds.Action
import org.roboquant.metrics.Indicator
import java.time.Instant

/**
 * This class enables the creation of an Indicator based on TaLib.
 *
 * Example:
 * ```
 * val indicator = TaLibIndicator(barCount) {
 *      mapOf("ema" to ema(it, barCount))
 * }
 * ```
 */
class TaLibIndicator  (
    barCount: Int = 20,
    private val block: TaLib.(series: PriceBarSerie) -> Map<String, Double>
) : Indicator {

    private val taLib = TaLib()
    private val series = PriceBarSerie(barCount)

    override fun calculate(action: Action, time: Instant): Map<String, Double> {
        return if (series.add(action)) {
            block.invoke(taLib, series)
        } else {
            emptyMap()
        }
    }

    /**
     * @see Indicator.clear
     */
    override fun clear() {
        series.clear()
    }

    /**
     * Commonly used indicators using the TaLib library
     */
    companion object {

        /**
         * Return a Relative Strength Indicator for the provided [barCount]
         */
        fun rsi(barCount: Int = 10) : TaLibIndicator {
            return TaLibIndicator(barCount+1) {
                mapOf("rsi$barCount" to rsi(it, barCount))
            }
        }

        /**
         * Return a Bollinger Band Indicator for the provided [barCount]
         */
        fun bbands(barCount: Int = 10) : TaLibIndicator {
            return TaLibIndicator(barCount) {
                val (high, mid, low) = bbands(it, barCount)
                val prefix = "bb$barCount"
                mapOf("$prefix.low" to low, "$prefix.high" to high, "$prefix.mid" to mid)
            }
        }

        /**
         * Return an Exponential Moving Average Indicator for the provided [barCount]
         */
        fun ema(barCount: Int = 10) : TaLibIndicator {
            return TaLibIndicator(barCount) {
                mapOf("ema$barCount" to ema(it, barCount))
            }
        }

        /**
         * Return an Simple Moving Average Indicator for the provided [barCount]
         */
        fun sma(barCount: Int = 10) : TaLibIndicator {
            return TaLibIndicator(barCount) {
                mapOf("sma$barCount" to sma(it, barCount))
            }
        }
        
        
    }

}
