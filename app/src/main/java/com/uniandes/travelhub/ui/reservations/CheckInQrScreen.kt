package com.uniandes.travelhub.ui.reservations

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.reservations.CachedCheckInQr
import com.uniandes.travelhub.ui.auth.components.asString
import com.uniandes.travelhub.ui.reservations.components.formatReservationDate
import com.uniandes.travelhub.ui.theme.spacing
import com.uniandes.travelhub.utils.CheckInQrCodec
import com.uniandes.travelhub.utils.sanitizeDisplayText
import com.uniandes.travelhub.viewmodels.CheckInQrUiState
import com.uniandes.travelhub.viewmodels.CheckInQrViewModel

@Composable
fun CheckInQrScreen(
    viewModel: CheckInQrViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    CheckInQrScreenContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = viewModel::load,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInQrScreenContent(
    uiState: CheckInQrUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.checkin_qr_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.property_detail_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState) {
                CheckInQrUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CheckInQrUiState.Error -> MessageState(
                    message = uiState.message.asString(),
                    action = stringResource(R.string.property_retry),
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
                is CheckInQrUiState.Invalidated -> MessageState(
                    message = uiState.message.asString(),
                    action = stringResource(R.string.property_retry),
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center),
                )
                is CheckInQrUiState.Available -> CheckInQrBody(
                    data = uiState.payload,
                    isOffline = uiState.isOffline,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(MaterialTheme.spacing.lg)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun CheckInQrBody(
    data: CachedCheckInQr,
    isOffline: Boolean,
    modifier: Modifier = Modifier,
) {
    val qrBitmap = remember(data.encryptedPayload) {
        CheckInQrCodec.createQrBitmap(data.encryptedPayload, sizePx = 720)
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        data.propertyCoverImageUrl?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = sanitizeDisplayText(data.propertyName.orEmpty()),
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Text(
            text = sanitizeDisplayText(data.propertyName ?: stringResource(R.string.checkin_qr_property_fallback)),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.checkin_qr_reservation_id, shortReservationCode(data.reservationId)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.checkin_qr_image_content_description),
                            modifier = Modifier.size(220.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(
                        if (isOffline) R.string.checkin_qr_offline_ready
                        else R.string.checkin_qr_scan_hint
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionTitle(text = stringResource(R.string.checkin_qr_arrival_instructions))
        InstructionCard(
            step = "1",
            title = stringResource(R.string.checkin_qr_step_lobby_title),
            description = stringResource(R.string.checkin_qr_step_lobby_body),
        )
        InstructionCard(
            step = "2",
            title = stringResource(R.string.checkin_qr_step_scan_title),
            description = stringResource(R.string.checkin_qr_step_scan_body),
        )
        InstructionCard(
            step = "3",
            title = stringResource(R.string.checkin_qr_step_verify_title),
            description = stringResource(R.string.checkin_qr_step_verify_body, formatReservationDate(data.checkInDate)),
        )
        Text(
            text = stringResource(
                R.string.checkin_qr_stay_summary,
                formatReservationDate(data.checkInDate),
                formatReservationDate(data.checkOutDate),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Box(modifier = Modifier.fillMaxWidth()) {
        RowLikeTitle(text = text)
    }
}

@Composable
private fun RowLikeTitle(text: String) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InstructionCard(
    step: String,
    title: String,
    description: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = step,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MessageState(
    message: String,
    action: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRetry) { Text(action) }
    }
}

private fun shortReservationCode(reservationId: String): String =
    "#BK-${reservationId.takeLast(6).uppercase()}"
