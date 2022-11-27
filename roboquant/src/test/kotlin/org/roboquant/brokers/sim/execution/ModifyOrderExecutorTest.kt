/*
 * Copyright 2020-2022 Neural Layer
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

package org.roboquant.brokers.sim.execution

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.roboquant.TestData
import org.roboquant.common.Size
import org.roboquant.orders.*
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ModifyOrderExecutorTest {

    private val asset = TestData.usStock()


    @Test
    fun testLimitOrderHandler() {
        val order1 = LimitOrder(asset, Size(100), 10.0)
        val handler = LimitOrderExecutor(order1)

        val order2 = LimitOrder(asset, Size(100), 11.0)
        assertTrue(handler.update(order2, Instant.now()))

        assertEquals(order2.limit, handler.order.limit)
    }



    @Test
    fun testFailingMarketOrderHandler() {
        val order1 = MarketOrder(asset, 100)
        val order2 = LimitOrder(asset, Size(100), 10.0)
        assertThrows<IllegalArgumentException> {
            UpdateOrder(order1, order2)
        }

    }

    @Test
    fun testCancelOrderHandler() {
        val order1 = MarketOrder(asset, 100)
        val exec1 = MarketOrderExecutor(order1)

        val order = CancelOrder(order1)

        val cmd = CancelOrderExecutor(order)
        cmd.execute(exec1, Instant.now())
        assertEquals(OrderStatus.COMPLETED, cmd.status)
        assertEquals(OrderStatus.CANCELLED, exec1.status)
    }




}