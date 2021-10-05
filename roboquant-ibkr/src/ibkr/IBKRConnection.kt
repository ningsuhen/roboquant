package org.roboquant.ibkr

import com.ib.client.*
import org.roboquant.common.Asset
import org.roboquant.common.AssetType
import java.util.logging.Logger

/**
 * Common utilities for both IBKR Broker and IBKR Feed implementations
 *
 */
internal class IBKRConnection(wrapper: EWrapper, private val logger: Logger) {

    private val signal = EJavaSignal()
    val client = EClientSocket(wrapper, signal)

    /**
     * Connect to a IBKR TWS or Gateway
     *
     * @param host
     * @param port
     * @param clientId
     */
    fun connect(host: String, port: Int, clientId: Int) {
        client.isAsyncEConnect = false
        client.eConnect(host, port, clientId)
        logger.info { "Connected to IBKR on $host and port $port with clientId $clientId" }

        val reader = EReader(client, signal)
        reader.start()
        Thread {
            while (client.isConnected) {
                signal.waitForSignal()
                try {
                    reader.processMsgs()
                } catch (e: Exception) {
                    logger.warning("Exception: " + e.message)
                }
            }
        }.start()
    }

    fun disconnect() {
        client.eDisconnect()
    }

    /**
     * Convert a roboquant asset to an IBKR contract.
     *
     * TODO support more asset classes
     *
     * @param asset
     * @return
     */
    fun getContract(asset: Asset): Contract {
        val contract = Contract()
        contract.symbol(asset.symbol)
        contract.currency(asset.currencyCode)
        if (asset.multiplier != 1.0) contract.multiplier(asset.multiplier.toString())

        when(asset.type) {
            AssetType.STOCK -> contract.secType(Types.SecType.STK)
            AssetType.FOREX -> contract.secType(Types.SecType.CASH)
            AssetType.BOND -> contract.secType(Types.SecType.BOND)
            else -> throw Exception("${asset.type} is not supported")
        }

        val exchange = when (asset.exchangeCode) {
            "NASDAQ" -> "ISLAND"
            "" -> "SMART"
            else -> asset.exchangeCode
        }
        contract.exchange(exchange)

        if (asset.id.isNotEmpty()) contract.conid(asset.id.toInt())
        return contract
    }


}
