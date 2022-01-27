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

package org.roboquant.orders

import org.roboquant.TestData
import java.time.Instant
import org.junit.Test
import org.roboquant.brokers.sim.Execution
import kotlin.test.assertEquals

class CombinedOrderTest {

    class MyOrder(order: MarketOrder) : CombinedOrder(order) {
        override fun execute(price: Double, time: Instant): List<Execution> {
            return listOf()
        }

    }

    @Test
    fun test() {
        val child = TestData.euMarketOrder()
        val order = MyOrder(child)
        order.price = 100.0
        assertEquals(100.0, child.price)
        assertEquals(child.getValue(), order.getValue())

    }

}