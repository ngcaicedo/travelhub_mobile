package com.uniandes.travelhub.ui.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.resolvePropertyImageUrl
import com.uniandes.travelhub.utils.sanitizeDisplayText
import com.uniandes.travelhub.utils.sortPropertyImages
import com.uniandes.travelhub.viewmodels.PropertyDetailUiState
import com.uniandes.travelhub.viewmodels.PropertyDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    viewModel: PropertyDetailViewModel,
    onBackClick: () -> Unit,
    onReserveClick: (Property) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.property_detail_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is PropertyDetailUiState.Idle -> Unit
                is PropertyDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PropertyDetailUiState.Success -> {
                    PropertyDetailContent(
                        property = state.property,
                        isRefreshing = state.isRefreshing,
                        onReserveClick = { onReserveClick(state.property) },
                    )
                }
                is PropertyDetailUiState.Error -> {
                    ErrorState(
                        message = state.message.asString(),
                        onRetry = { viewModel.loadPropertyDetail() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyDetailContent(
    property: Property,
    isRefreshing: Boolean,
    onReserveClick: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val galleryHorizontalPadding = MaterialTheme.spacing.xl
    val cardWidthDp = remember(configuration, galleryHorizontalPadding) { configuration.screenWidthDp.dp - galleryHorizontalPadding }
    val cardWidthPx = remember(configuration, density, cardWidthDp) {
        with(density) { cardWidthDp.roundToPx() }
    }
    val sortedImages = remember(property.images) { sortPropertyImages(property.images) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.xl)
    ) {
        if (isRefreshing) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            if (sortedImages.isEmpty()) {
                Text(
                    text = stringResource(R.string.property_detail_no_images),
                    modifier = Modifier.padding(MaterialTheme.spacing.md),
                )
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.md),
                ) {
                    items(sortedImages) { image ->
                        PropertyImageCard(
                            image = image,
                            propertyName = sanitizeDisplayText(property.name),
                            targetWidthPx = cardWidthPx,
                            cardWidthDp = cardWidthDp,
                        )
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                Text(
                    text = sanitizeDisplayText(property.name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " " + stringResource(
                            R.string.property_detail_reviews_count,
                            property.rating,
                            property.reviewCount
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " · ${sanitizeDisplayText(property.location)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = MaterialTheme.spacing.xs)
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${property.currency} ${property.pricePerNight}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " " + stringResource(R.string.property_detail_price_per_night),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
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

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))

                TravelHubPrimaryButton(
                    text = stringResource(R.string.property_detail_reserve),
                    onClick = onReserveClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            DetailSection(
                title = stringResource(R.string.property_detail_amenities_title)
            ) {
                property.amenities.forEach { amenity ->
                    Text(
                        text = "• ${sanitizeDisplayText(amenity)}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
                    )
                }
            }
        }

        item {
            DetailSection(
                title = stringResource(R.string.property_detail_description_title)
            ) {
                Text(
                    text = sanitizeDisplayText(property.description),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            DetailSection(
                title = stringResource(R.string.property_detail_policy_title)
            ) {
                Text(
                    text = sanitizeDisplayText(property.cancellationPolicy),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        item {
            DetailSection(
                title = stringResource(R.string.property_detail_reviews_title)
            ) {
                if (property.reviews.isEmpty()) {
                    Text(stringResource(R.string.property_detail_no_reviews))
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

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md, vertical = MaterialTheme.spacing.sm)) {
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
private fun PropertyImageCard(
    image: PropertyImage,
    propertyName: String,
    targetWidthPx: Int,
    cardWidthDp: Dp
) {
    val imageUrl = remember(image, targetWidthPx) {
        resolvePropertyImageUrl(image, targetWidthPx = targetWidthPx)
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = sanitizeDisplayText(image.altText ?: propertyName),
        modifier = Modifier
            .width(cardWidthDp)
            .clip(RoundedCornerShape(MaterialTheme.spacing.sm)),
        contentScale = ContentScale.Crop
    )
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


