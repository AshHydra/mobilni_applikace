package cz.ash.mobilniapplikace.data

import cz.ash.mobilniapplikace.data.local.FavoriteCoinEntity
import cz.ash.mobilniapplikace.data.local.FavoritesDao
import cz.ash.mobilniapplikace.data.local.MarketCacheDao
import cz.ash.mobilniapplikace.data.local.MarketCoinEntity
import cz.ash.mobilniapplikace.data.local.CurrencySyncEntity
import cz.ash.mobilniapplikace.data.remote.CoinGeckoApi
import cz.ash.mobilniapplikace.domain.Coin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class CoinsRepository(
    private val api: CoinGeckoApi,
    private val favoritesDao: FavoritesDao,
    private val marketCacheDao: MarketCacheDao
) {
    private data class CacheEntry<T>(val atMs: Long, val value: T)

    private val topTtlMs = 5 * 60_000L
    private val minIntervalMs = 5_000L

    private val topCacheByCurrency: MutableMap<String, CacheEntry<List<Coin>>> = mutableMapOf()
    private val coinCache: MutableMap<String, CacheEntry<Coin>> = mutableMapOf()
    private val coinsByIdsCache: MutableMap<String, CacheEntry<List<Coin>>> = mutableMapOf()

    fun observeFavoriteIds(): Flow<Set<String>> =
        favoritesDao.observeAll().map { list -> list.map { it.id }.toSet() }

    fun observeFavorites(): Flow<List<Coin>> =
        favoritesDao.observeAll().map { list ->
            list.map {
                Coin(
                    id = it.id,
                    symbol = it.symbol,
                    name = it.name,
                    imageUrl = it.imageUrl,
                    priceUsd = it.lastPriceUsd,
                    change24hPct = it.lastChange24hPct,
                    marketCapUsd = it.lastMarketCapUsd
                )
            }
        }

    fun observeIsFavorite(id: String): Flow<Boolean> = favoritesDao.observeIsFavorite(id)

    suspend fun fetchTopCoins(vsCurrency: String, force: Boolean = false): List<Coin> {
        val now = System.currentTimeMillis()
        val currency = vsCurrency.lowercase()
        val cached = topCacheByCurrency[currency]

        // Normal case: reuse cache during tab switching / recomposition.
        if (!force && cached != null && now - cached.atMs <= topTtlMs) return cached.value

        // Persistent cache fallback
        val sync = marketCacheDao.getSync(currency) ?: CurrencySyncEntity(currency, 0L, 0L)
        val cacheFromDb: List<Coin> = marketCacheDao.getAllForCurrency(currency).map { it.toDomain() }

        // If API told us to wait, only return cached data.
        if (now < sync.nextAllowedAtMs) {
            if (cacheFromDb.isNotEmpty()) return cacheFromDb
        }

        // Avoid hammering API even if refresh is triggered multiple times quickly.
        if (!force && sync.lastFetchedAtMs > 0 && now - sync.lastFetchedAtMs <= topTtlMs && cacheFromDb.isNotEmpty()) {
            topCacheByCurrency[currency] = CacheEntry(now, cacheFromDb)
            return cacheFromDb
        }
        if (cached != null && now - cached.atMs <= minIntervalMs) return cached.value

        try {
            val fresh = api.getMarkets(vsCurrency = currency).map { dto ->
                Coin(
                    id = dto.id,
                    symbol = dto.symbol,
                    name = dto.name,
                    imageUrl = dto.image,
                    priceUsd = dto.currentPrice,
                    change24hPct = dto.priceChangePercentage24h,
                    marketCapUsd = dto.marketCap
                )
            }
            // Save to persistent cache
            marketCacheDao.deleteForCurrency(currency)
            marketCacheDao.upsertAll(fresh.map { it.toMarketEntity(currency, now) })
            marketCacheDao.upsertSync(CurrencySyncEntity(currency, lastFetchedAtMs = now, nextAllowedAtMs = 0L))

            topCacheByCurrency[currency] = CacheEntry(now, fresh)
            return fresh
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 429) {
                val retryAfterSec = t.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                val nextAllowed = if (retryAfterSec != null) now + retryAfterSec * 1000 else now + 60_000L
                marketCacheDao.upsertSync(CurrencySyncEntity(currency, lastFetchedAtMs = sync.lastFetchedAtMs, nextAllowedAtMs = nextAllowed))
                if (cacheFromDb.isNotEmpty()) {
                    topCacheByCurrency[currency] = CacheEntry(now, cacheFromDb)
                    return cacheFromDb
                }
            }
            throw t
        }
    }

    suspend fun fetchCoin(id: String, vsCurrency: String, force: Boolean = false): Coin? {
        val now = System.currentTimeMillis()
        val currency = vsCurrency.lowercase()
        val cacheKey = "$currency:$id"
        val cached = coinCache[cacheKey]
        if (!force && cached != null && now - cached.atMs <= topTtlMs) return cached.value

        val sync = marketCacheDao.getSync(currency) ?: CurrencySyncEntity(currency, 0L, 0L)
        if (now < sync.nextAllowedAtMs) {
            val fromDb = marketCacheDao.getByIds(currency, listOf(id)).firstOrNull()?.toDomain()
            if (fromDb != null) return fromDb
        }

        val list = api.getMarkets(vsCurrency = currency, ids = id, perPage = 1, page = 1)
        val dto = list.firstOrNull() ?: return null
        val fresh = Coin(
            id = dto.id,
            symbol = dto.symbol,
            name = dto.name,
            imageUrl = dto.image,
            priceUsd = dto.currentPrice,
            change24hPct = dto.priceChangePercentage24h,
            marketCapUsd = dto.marketCap
        )
        coinCache[cacheKey] = CacheEntry(now, fresh)
        // Update persistent cache for that coin (best-effort)
        marketCacheDao.upsertAll(listOf(fresh.toMarketEntity(currency, now)))
        return fresh
    }

    suspend fun fetchCoinsByIds(ids: List<String>, vsCurrency: String, force: Boolean = false): List<Coin> {
        if (ids.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val currency = vsCurrency.lowercase()
        val idsKey = ids.sorted().joinToString(",")
        val cacheKey = "$currency:$idsKey"
        val cached = coinsByIdsCache[cacheKey]

        if (!force && cached != null && now - cached.atMs <= topTtlMs) return cached.value
        if (cached != null && now - cached.atMs <= minIntervalMs) return cached.value

        val sync = marketCacheDao.getSync(currency) ?: CurrencySyncEntity(currency, 0L, 0L)

        // If we're rate-limited, return cached subset from DB.
        if (now < sync.nextAllowedAtMs) {
            val fromDb = marketCacheDao.getByIds(currency, ids).map { it.toDomain() }
            if (fromDb.isNotEmpty()) return fromDb
        }

        try {
            val fresh = api.getMarkets(vsCurrency = currency, ids = idsKey, perPage = ids.size.coerceAtMost(250), page = 1)
                .map { dto ->
                    Coin(
                        id = dto.id,
                        symbol = dto.symbol,
                        name = dto.name,
                        imageUrl = dto.image,
                        priceUsd = dto.currentPrice,
                        change24hPct = dto.priceChangePercentage24h,
                        marketCapUsd = dto.marketCap
                    )
                }
            marketCacheDao.upsertAll(fresh.map { it.toMarketEntity(currency, now) })
            marketCacheDao.upsertSync(CurrencySyncEntity(currency, lastFetchedAtMs = now, nextAllowedAtMs = 0L))
            coinsByIdsCache[cacheKey] = CacheEntry(now, fresh)
            return fresh
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 429) {
                val retryAfterSec = t.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                val nextAllowed = if (retryAfterSec != null) now + retryAfterSec * 1000 else now + 60_000L
                marketCacheDao.upsertSync(CurrencySyncEntity(currency, lastFetchedAtMs = sync.lastFetchedAtMs, nextAllowedAtMs = nextAllowed))
                val fromDb = marketCacheDao.getByIds(currency, ids).map { it.toDomain() }
                if (fromDb.isNotEmpty()) return fromDb
            }
            throw t
        }
    }

    suspend fun setFavorite(coin: Coin, favorite: Boolean) {
        if (favorite) {
            favoritesDao.upsert(
                FavoriteCoinEntity(
                    id = coin.id,
                    symbol = coin.symbol,
                    name = coin.name,
                    imageUrl = coin.imageUrl,
                    lastPriceUsd = coin.priceUsd,
                    lastChange24hPct = coin.change24hPct,
                    lastMarketCapUsd = coin.marketCapUsd
                )
            )
        } else {
            favoritesDao.deleteById(coin.id)
        }
    }

    suspend fun clearFavorites() {
        favoritesDao.clearAll()
    }

    private fun MarketCoinEntity.toDomain(): Coin =
        Coin(
            id = id,
            symbol = symbol,
            name = name,
            imageUrl = imageUrl,
            priceUsd = price,
            change24hPct = change24hPct,
            marketCapUsd = marketCap
        )

    private fun Coin.toMarketEntity(vsCurrency: String, nowMs: Long): MarketCoinEntity =
        MarketCoinEntity(
            vsCurrency = vsCurrency,
            id = id,
            symbol = symbol,
            name = name,
            imageUrl = imageUrl,
            price = priceUsd,
            change24hPct = change24hPct,
            marketCap = marketCapUsd,
            updatedAtMs = nowMs
        )
}

