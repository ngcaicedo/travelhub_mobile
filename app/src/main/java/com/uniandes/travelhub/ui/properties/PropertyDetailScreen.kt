package com.uniandes.travelhub.ui.properties

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.ui.auth.components.TravelHubPrimaryButton
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.viewmodels.PropertyDetailUiState
import com.uniandes.travelhub.viewmodels.PropertyDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyDetailScreen(
    viewModel: PropertyDetailViewModel,
    onBackClick: () -> Unit
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
                is PropertyDetailUiState.Idle -> { /* nothing */ }
                is PropertyDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PropertyDetailUiState.Success -> {
                    PropertyDetailContent(property = state.property)
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
private fun PropertyDetailContent(property: Property) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Image Carousel
        if (property.images.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.md)
            ) {
                items(property.images) { image ->
                    AsyncImage(
                        model = image.url,
                        contentDescription = image.altText ?: property.name,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .clip(RoundedCornerShape(MaterialTheme.spacing.sm)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .padding(MaterialTheme.spacing.md)
                    .clip(RoundedCornerShape(MaterialTheme.spacing.sm)),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.property_detail_no_images))
            }
        }

        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Text(
                text = property.name,
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
                    text = " · ${property.location}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = MaterialTheme.spacing.xs)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.lg))

            // Details info
            Text(
                text = stringResource(
                    R.string.property_detail_capacity,
                    property.maxGuests,
                    property.bedrooms,
                    property.bathrooms
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.lg))

            // Description
            Text(
                text = stringResource(R.string.property_detail_description_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Text(
                text = property.description,
                style = MaterialTheme.typography.bodyLarge
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.spacing.lg))

            // Amenities
            Text(
                text = stringResource(R.string.property_detail_amenities_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            property.amenities.forEach { amenity ->
                Text(
                    text = "• $amenity",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))

            // Footer Price and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
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
                }

                TravelHubPrimaryButton(
                    text = stringResource(R.string.property_detail_reserve),
                    onClick = { /* Handle Reservation */ },
                    modifier = Modifier.weight(0.5f).padding(start = MaterialTheme.spacing.md)
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
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
