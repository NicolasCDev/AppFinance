package com.example.appfinancetest.services

import com.example.appfinancetest.classes.TransactionDB
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class StockPricePoint(
    val timestampMilli: Long,
    val closePrice: Double
)

data class StockTickerData(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val previousClose: Double,
    val currency: String,
    val history: List<StockPricePoint>
)

object StockMarketService {

    private val ISIN_REGEX = Regex("[A-Z]{2}[A-Z0-9]{9}[0-9]")

    val KNOWN_ISIN_MAP = mapOf(
        "FR0010315770" to "CW8.PA",
        "LU1681043599" to "CW8.PA",
        "FR0011550186" to "ESE.PA",
        "FR0000121014" to "MC.PA",
        "FR0000120271" to "TTE.PA",
        "NL0000235190" to "AIR.PA",
        "FR0000120073" to "OR.PA",
        "FR0000120628" to "CS.PA",
        "FR0000131104" to "BNP.PA",
        "FR0000121667" to "EL.PA",
        "FR0000125486" to "DG.PA",
        "FR0000120578" to "SAN.PA",
        "FR0000121972" to "RI.PA",
        "FR0000073272" to "SAF.PA",
        "FR0000121329" to "SU.PA",
        "FR0000120321" to "GLE.PA",
        "US0378331005" to "AAPL",
        "US67066G1040" to "NVDA",
        "US5949181045" to "MSFT",
        "US0231351067" to "AMZN",
        "US88160R1014" to "TSLA",
        "IE00B4L5Y983" to "IWDA.AS",
        "IE00B5B83C77" to "SXXP.PA",
        "FR0013412020" to "WPEA.PA",
        "FR0013412285" to "PE500.PA"
    )

    fun extractIsin(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return ISIN_REGEX.find(text.uppercase(Locale.ROOT))?.value
    }

    fun inferCryptoTicker(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val upper = text.uppercase(Locale.ROOT)
        return when {
            upper.contains("BITCOIN") || upper.contains("BTC") -> "BTC-EUR"
            upper.contains("ETHEREUM") || upper.contains("ETH") -> "ETH-EUR"
            upper.contains("SOLANA") || upper.contains("SOL") -> "SOL-EUR"
            upper.contains("CARDANO") || upper.contains("ADA") -> "ADA-EUR"
            upper.contains("RIPPLE") || upper.contains("XRP") -> "XRP-EUR"
            else -> null
        }
    }

    suspend fun resolveIsinToTicker(isin: String): String {
        val cleanIsin = isin.trim().uppercase(Locale.ROOT)
        KNOWN_ISIN_MAP[cleanIsin]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val urlString = "https://query1.finance.yahoo.com/v1/finance/search?q=$cleanIsin&quotesCount=1"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val responseBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        responseBuilder.append(line)
                    }
                    reader.close()

                    val jsonObj = JSONObject(responseBuilder.toString())
                    val quotes = jsonObj.optJSONArray("quotes")
                    if (quotes != null && quotes.length() > 0) {
                        val symbol = quotes.getJSONObject(0).optString("symbol")
                        if (symbol.isNotBlank()) {
                            return@withContext symbol
                        }
                    }
                }
            } catch (_: Exception) {
            }
            cleanIsin
        }
    }

    suspend fun fetchTickerData(symbol: String, range: String = "max", interval: String = "1d"): Result<StockTickerData> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanSymbol = symbol.trim().uppercase(Locale.ROOT)
                val urlString = "https://query1.finance.yahoo.com/v8/finance/chart/$cleanSymbol?range=$range&interval=$interval"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                )

                if (connection.responseCode != 200) {
                    return@withContext Result.failure(Exception("Erreur serveur (${connection.responseCode})"))
                }

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    responseBuilder.append(line)
                }
                reader.close()

                val jsonStr = responseBuilder.toString()
                val rootObj = JSONObject(jsonStr)
                val chartObj = rootObj.getJSONObject("chart")
                val resultArray = chartObj.optJSONArray("result")

                if (resultArray == null || resultArray.length() == 0) {
                    return@withContext Result.failure(Exception("Ticker non trouvé ($cleanSymbol)"))
                }

                val resultObj = resultArray.getJSONObject(0)
                val metaObj = resultObj.getJSONObject("meta")

                val symbolMeta = metaObj.optString("symbol", cleanSymbol)
                val currency = metaObj.optString("currency", "EUR")
                val regularMarketPrice = metaObj.optDouble("regularMarketPrice", 0.0)
                val previousClose = metaObj.optDouble("chartPreviousClose", metaObj.optDouble("previousClose", regularMarketPrice))
                val longName = metaObj.optString("longName", metaObj.optString("shortName", symbolMeta))

                val timestamps = resultObj.optJSONArray("timestamp")
                val indicatorsObj = resultObj.optJSONObject("indicators")
                val quoteArray = indicatorsObj?.optJSONArray("quote")
                val quoteObj = quoteArray?.optJSONObject(0)
                val closeArray = quoteObj?.optJSONArray("close")

                val historyList = mutableListOf<StockPricePoint>()
                if (timestamps != null && closeArray != null) {
                    val length = timestamps.length().coerceAtMost(closeArray.length())
                    for (i in 0 until length) {
                        if (!timestamps.isNull(i) && !closeArray.isNull(i)) {
                            val tsSec = timestamps.getLong(i)
                            val closeVal = closeArray.getDouble(i)
                            if (closeVal > 0.0) {
                                historyList.add(StockPricePoint(tsSec * 1000L, closeVal))
                            }
                        }
                    }
                }

                val currentPrice = if (regularMarketPrice > 0.0) regularMarketPrice else historyList.lastOrNull()?.closePrice ?: 0.0

                Result.success(
                    StockTickerData(
                        symbol = symbolMeta,
                        name = longName,
                        currentPrice = currentPrice,
                        previousClose = previousClose,
                        currency = currency,
                        history = historyList
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
