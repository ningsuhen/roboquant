package org.roboquant.binance

import com.binance.api.client.BinanceApiRestClient
import org.roboquant.common.Asset
import org.roboquant.common.AssetType
import org.roboquant.common.Logging
import org.roboquant.common.TimeFrame
import org.roboquant.feeds.*
import java.time.Instant
import java.util.*


/**
 * Create a new feed based on price actions coming from the Binance exchange.
 *
 * @property useMachineTime
 * @constructor
 *
 */
class BinanceHistoricFeed(apiKey: String? = null, secret:String? = null, private val useMachineTime: Boolean = true) : HistoricFeed {

    private var client: BinanceApiRestClient
    private val logger = Logging.getLogger(this)

    private val events = TreeMap<Instant, MutableList<PriceAction>>()
    override val timeline: List<Instant>
        get() = events.keys.toList()

    override val assets
        get() = events.values.map { priceBars -> priceBars.map { it.asset }.distinct() }.flatten().distinct().toSortedSet()

    init {
        val factory = BinanceConnection.getFactory(apiKey, secret)
        client = factory.newRestClient()
    }

    /**
     * (Re)play the events of the feed using the provided [EventChannel]
     *
     * @param channel
     * @return
     */
    override suspend fun play(channel: EventChannel) {
        events.forEach {
            val event = Event(it.value, it.key)
            channel.send(event)
        }
    }


    fun retrieve( vararg names:String, timeFrame: TimeFrame, interval: Interval = Interval.DAILY, limit: Int = 500) {
        require(names.isNotEmpty()) { "You need to provide at least 1 name"}
        val startTime = timeFrame.start.toEpochMilli()
        val endTime = timeFrame.end.toEpochMilli() - 1 // Binance uses inclusive end-times, so we subtract 1 millis
        for (name in names) {
            val nameSplit = name.split('-').map { it.uppercase() }
            require(nameSplit.size == 2) { "Name needs to be of format XXX-YYY, for example BTC-BUSD"}
            val (currency1, currency2) = nameSplit
            val symbol = currency1 + currency2
            val bars = client.getCandlestickBars(symbol, interval, limit, startTime, endTime)
            val asset = getAsset(symbol, currency2)
            for (bar in bars) {
                val action = PriceBar(asset, bar.open.toDouble(), bar.high.toDouble(), bar.low.toDouble(), bar.close.toDouble(), bar.volume.toDouble())
                val now = Instant.ofEpochMilli(bar.closeTime)
                val list = events.getOrPut(now) { mutableListOf() }
                list.add(action)
            }
            logger.fine { "Retrieved $asset for $timeFrame"}
        }
    }



    /**
     * Create an asset based on a currency pair.
     *
     * @param symbol
     * @return
     */
    private fun getAsset(symbol: String, currencyCode: String): Asset {
        return Asset(
            symbol = symbol,
            currencyCode = currencyCode,
            exchangeCode = "Binance",
            type = AssetType.CRYPTO
        )
    }


}
