@file:Suppress("unused", "unused", "unused", "unused")

package org.roboquant.binance

import com.binance.api.client.BinanceApiRestClient
import com.binance.api.client.domain.TimeInForce
import com.binance.api.client.domain.account.NewOrder.*
import com.binance.api.client.domain.account.NewOrderResponse
import com.binance.api.client.domain.account.request.CancelOrderRequest
import com.binance.api.client.domain.account.request.OrderRequest
import org.roboquant.brokers.Account
import org.roboquant.brokers.Broker
import org.roboquant.common.Asset
import org.roboquant.common.AssetType
import org.roboquant.common.Currency
import org.roboquant.common.Logging
import org.roboquant.feeds.Event
import org.roboquant.orders.*
import java.math.BigDecimal
import kotlin.math.absoluteValue

/**
 * Implementation of the broker interface for Binance exchange. This enables live trading of cryptocurrencies
 * on the Binance exchange. This broker only supports assets of the type Crypto.
 *
 * @constructor
 *
 */
class BinanceBroker(
    apiKey: String? = null, secret:String? = null,
    baseCurrencyCode: String = "USD",
    private val useMachineTime: Boolean = true
) : Broker {

    private val client: BinanceApiRestClient
    override val account: Account = Account(Currency.getInstance(baseCurrencyCode))
    private val logger = Logging.getLogger("BinanceBroker")
    private val placedOrders = mutableMapOf<Long, SingleOrder>()
    private var orderId = 0

    val assets = retrieveAssets()

    init {
        val factory = BinanceConnection.getFactory(apiKey, secret)
        client = factory.newRestClient()
        logger.info("Created BinanceBroker with client $client")
        updateAccount()
    }

    private fun retrieveAssets(): List<Asset> {
        return client.exchangeInfo.symbols.map {
            Asset(it.symbol, AssetType.CRYPTO, it.quoteAsset, "BINANCE")
        }
    }


    private fun updateAccount() {
        val balances = client.account.balances
        for (balance in balances) {
            logger.info { "${balance.asset} ${balance.free}" }
        }

        for (order in client.getOpenOrders(OrderRequest(""))) {
            val o = placedOrders[order.orderId]
            if (o !== null) {
                o.fill = order.executedQty.toDouble()
            } else {
                logger.info("Received unknown order $order")
            }
        }
    }


    /**
     * Place orders on a XChange supported exchange using the trade service.
     * @TODO test with a real account on BinanceBroker
     *
     * @param orders
     * @return
     */
    override fun place(orders: List<Order>, event: Event): Account {

        for (order in orders) {
            val asset = order.asset
            if (asset.type == AssetType.CRYPTO) {
                val symbol = binanceSymbol(asset)

                when (order) {
                    is CancellationOrder -> cancelOrder(order)

                    is LimitOrder -> {
                        val newLimitOrder = trade(symbol, order)
                        placedOrders[newLimitOrder.orderId] = order
                    }
                    is MarketOrder -> {
                        val newMarketOrder = trade(symbol, order)
                        placedOrders[newMarketOrder.orderId] = order
                    }
                    else -> {
                        logger.warning { "BinanceBroker supports only cancellation, market and limit orders, received ${order::class} instead" }
                    }
                }

            } else {
                logger.warning { "BinanceBroker supports only CRYPTO assets, received ${asset.type} instead" }
            }
        }

        return account.clone()
    }

    private fun binanceSymbol(asset: Asset): String {
        return asset.symbol.uppercase()
    }

    /**
     * Cancel an order
     *
     * @param cancellation
     */
    private fun cancelOrder(cancellation: CancellationOrder) {
        val c = cancellation.order
        require(c.id.isNotEmpty()) {  "Require non empty id when cancelling and order $c"}
        require(c.asset.type == AssetType.CRYPTO) {  "BinanceBroker only support CRYPTO orders ${c.asset}"}
        val symbol = binanceSymbol(c.asset)
        val r = CancelOrderRequest(symbol, c.id)
        client.cancelOrder(r)
    }

    /**
     * Place a limit order for a currency pair
     *
     * @param symbol
     * @param order
     */
    private fun trade(symbol: String, order: LimitOrder): NewOrderResponse {
        val amount = BigDecimal(order.quantity.absoluteValue).toBigInteger().toString()
        val price = order.limit.toString()
        val newOrder = if (order.buy)
            client.newOrder(limitBuy(symbol, TimeInForce.GTC, amount, price))
        else
            client.newOrder(limitSell(symbol, TimeInForce.GTC, amount, price))
        logger.info { "$newOrder" }
        return newOrder
    }

    /**
     * place a market order for a currency pair
     *
     * @param symbol
     * @param order
     */
    private fun trade(symbol: String, order: MarketOrder): NewOrderResponse {
        val amount = BigDecimal(order.quantity.absoluteValue).toBigInteger().toString()
        val newOrder = if (order.buy)
            client.newOrder(marketBuy(symbol, amount))
        else
            client.newOrder(marketSell(symbol, amount))
        logger.info { "$newOrder" }
        return newOrder
    }

}