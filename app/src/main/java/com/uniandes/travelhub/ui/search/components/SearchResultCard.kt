package com.uniandes.travelhub.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.search.SearchResultItem
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.sanitizeDisplayText
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun SearchResultCard(
    item: SearchResultItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale: Locale = LocalConfiguration.current.locales[0]
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(),
    ) {
        Column {
            AsyncImage(
                model = item.mainImageUrl,
                contentDescription = sanitizeDisplayText(item.name),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                // Header: name (primary) + rating chip (right). Mirrors the web layout.
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = sanitizeDisplayText(item.name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = " %.1f".format(item.rating),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = " ${sanitizeDisplayText(item.city)}, ${sanitizeDisplayText(item.country)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatSearchPrice(item.priceFrom, item.currency, locale),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = " / " + stringResource(R.string.property_detail_price_per_night),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Mirrors the web's `formatCurrency(amount, currency, locale)` — uses the
 * platform NumberFormat in currency style with no decimal digits, so a
 * backend value like `"1240.0000000000000000"` (Decimal serialised verbatim)
 * renders as `COP 1.240` instead of the raw 17-digit string.
 */
private fun formatSearchPrice(rawPrice: String, currency: String, locale: Locale): String {
    val amount = rawPrice.toDoubleOrNull() ?: return "$currency $rawPrice"
    val nf = NumberFormat.getCurrencyInstance(locale).apply {
        runCatching { this.currency = Currency.getInstance(currency) }
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    return runCatching { nf.format(amount) }.getOrElse { "$currency ${amount.toLong()}" }
}
