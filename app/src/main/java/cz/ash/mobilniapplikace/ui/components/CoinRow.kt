package cz.ash.mobilniapplikace.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import cz.ash.mobilniapplikace.ui.settings.LocalVsCurrency

@Composable
fun CoinRow(
    name: String,
    symbol: String,
    imageUrl: String?,
    price: Double?,
    change24hPct: Double?,
    isFavorite: Boolean,
    onToggleFavorite: (() -> Unit)?,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vsCurrency = LocalVsCurrency.current
    val currencyFormatter = remember(vsCurrency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            runCatching { currency = Currency.getInstance(vsCurrency.uppercase()) }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinAvatar(imageUrl = imageUrl)

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = symbol.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            val changeColor = when {
                change24hPct == null -> MaterialTheme.colorScheme.onSurfaceVariant
                change24hPct >= 0 -> Color(0xFF16A34A) // green
                else -> Color(0xFFEF4444) // red
            }

            Text(
                text = change24hPct?.let { String.format(Locale.US, "%.2f%%", it) } ?: "—",
                style = MaterialTheme.typography.titleSmall,
                color = changeColor
            )
            Text(
                text = price?.let {
                    runCatching { currencyFormatter.format(it) }
                        .getOrElse { String.format(Locale.getDefault(), "%.2f %s", it, vsCurrency.uppercase()) }
                } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.padding(start = 4.dp)) {
                if (isFavorite) {
                    Icon(Icons.Filled.Star, contentDescription = "Remove from watchlist")
                } else {
                    Icon(Icons.Outlined.StarBorder, contentDescription = "Add to watchlist")
                }
            }
        }
    }
}

@Composable
private fun CoinAvatar(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNullOrBlank()) {
            // placeholder dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun CoinRowDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    )
}

