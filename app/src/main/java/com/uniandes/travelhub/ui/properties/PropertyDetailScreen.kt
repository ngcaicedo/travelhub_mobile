package com.uniandes.travelhub.ui.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.models.properties.PropertyImage
import com.uniandes.travelhub.models.properties.PropertyReview
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.properties.components.AmenityPill
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.resolvePropertyImageUrl
import com.uniandes.travelhub.utils.sanitizeDisplayText
import com.uniandes.travelhub.utils.sortPropertyImages
import com.uniandes.travelhub.viewmodels.PropertyDetailUiState
import com.uniandes.travelhub.viewmodels.PropertyDetailViewModel

@Composable
fun PropertyDetailScreen(
    viewModel: PropertyDetailViewModel,
    onBackClick: () -> Unit,
    onReserveClick: (Property) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is PropertyDetailUiState.Idle -> Unit
            is PropertyDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            is PropertyDetailUiState.Success -> {
                PropertyDetailContent(
                    property = state.property,
                    isRefreshing = state.isRefreshing,
                    onBackClick = onBackClick,
                    onReserveClick = { onReserveClick(state.property) },
                )
            }
            is PropertyDetailUiState.Error -> {
                ErrorState(
                    message = state.message.asString(),
                    onRetry = { viewModel.loadPropertyDetail() },
                    modifier = Modifier.align(Alignment.Center)
                )
                BackOverlayButton(
                    onBackClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(MaterialTheme.spacing.md)
                )
            }
        }
    }
}

@Composable
private fun PropertyDetailContent(
    property: Property,
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onReserveClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    HeroSection(
                        property = property,
                        onBackClick = onBackClick,
                    )
                }
                item {
                    HeaderInfo(property = property)
                }
                if (property.amenities.isNotEmpty()) {
                    item {
                        AmenitiesSection(amenities = property.amenities)
                    }
                }
                item {
                    DetailSection(title = stringResource(R.string.property_detail_about_title)) {
                        Text(
                            text = sanitizeDisplayText(property.description),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (property.cancellationPolicy.isNotBlank()) {
                    item {
                        DetailSection(title = stringResource(R.string.property_detail_policy_title)) {
                            Text(
                                text = sanitizeDisplayText(property.cancellationPolicy),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                item {
                    DetailSection(title = stringResource(R.string.property_detail_reviews_title)) {
                        if (property.reviews.isEmpty()) {
                            Text(
                                text = stringResource(R.string.property_detail_no_reviews),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)) {
                                property.reviews.forEach { review ->
                                    ReviewCard(review)
                                }
                            }
                        }
                    }
                }
            }
        }

        StickyReserveBar(
            property = property,
            onReserveClick = onReserveClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun HeroSection(
    property: Property,
    onBackClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val widthDp = configuration.screenWidthDp.dp
    val widthPx = remember(widthDp, density) { with(density) { widthDp.roundToPx() } }
    val sortedImages = remember(property.images) { sortPropertyImages(property.images) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        if (sortedImages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.property_detail_no_images),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(modifier = Modifier.fillMaxSize()) {
                items(sortedImages) { image ->
                    HeroImage(
                        image = image,
                        propertyName = sanitizeDisplayText(property.name),
                        widthDp = widthDp,
                        widthPx = widthPx,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.4f),
                        0.5f to Color.Transparent
                    )
                )
        )

        BackOverlayButton(
            onBackClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(MaterialTheme.spacing.md)
        )
    }
}

@Composable
private fun HeroImage(
    image: PropertyImage,
    propertyName: String,
    widthDp: Dp,
    widthPx: Int,
) {
    val imageUrl = remember(image, widthPx) {
        resolvePropertyImageUrl(image, targetWidthPx = widthPx)
    }
    AsyncImage(
        model = imageUrl,
        contentDescription = sanitizeDisplayText(image.altText ?: propertyName),
        modifier = Modifier
            .width(widthDp)
            .fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun BackOverlayButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.35f),
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.property_detail_back),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun HeaderInfo(property: Property) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-24).dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.md,
                end = MaterialTheme.spacing.md,
                top = MaterialTheme.spacing.lg,
                bottom = MaterialTheme.spacing.sm
            )
        ) {
            Text(
                text = sanitizeDisplayText(property.name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = sanitizeDisplayText(property.location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", property.rating),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(
                            R.string.property_detail_reviews_count_short,
                            property.reviewCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(
                    R.string.property_detail_capacity,
                    property.maxGuests,
                    property.bedrooms,
                    property.bathrooms
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AmenitiesSection(amenities: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.md,
                vertical = MaterialTheme.spacing.sm
            )
    ) {
        Text(
            text = stringResource(R.string.property_detail_amenities_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        ) {
            items(amenities) { amenity ->
                AmenityPill(label = sanitizeDisplayText(amenity))
            }
        }
    }
}

@Composable
private fun StickyReserveBar(
    property: Property,
    onReserveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${property.currency} ${property.pricePerNight}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " " + stringResource(R.string.property_detail_price_per_night),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Button(
                    onClick = onReserveClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.property_detail_reserve),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = MaterialTheme.spacing.md,
            vertical = MaterialTheme.spacing.sm
        )
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = MaterialTheme.spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        content()
    }
}

@Composable
private fun ReviewCard(review: PropertyReview) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sanitizeDisplayText(review.author),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${review.rating}/5",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = review.reviewDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (review.verifiedStay) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                Text(
                    text = stringResource(R.string.property_detail_verified_stay),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = sanitizeDisplayText(review.comment),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        TravelHubPrimaryButton(
            text = stringResource(R.string.property_retry),
            onClick = onRetry
        )
    }
}
