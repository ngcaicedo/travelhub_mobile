package com.uniandes.travelhub.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.network.NotificationItemDto
import com.uniandes.travelhub.viewmodels.NotificationsFilter
import com.uniandes.travelhub.viewmodels.NotificationsUiState
import com.uniandes.travelhub.viewmodels.NotificationsViewModel
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    viewModel: NotificationsViewModel,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onItemClick: (NotificationItemDto) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.notifications_back))
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.notifications_settings_action))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterTabs(filter, viewModel::selectFilter)
            HorizontalDivider()

            when (val s = state) {
                is NotificationsUiState.Loading -> CenteredLoader()
                is NotificationsUiState.Error -> CenteredText(stringResource(R.string.notifications_error, s.message))
                is NotificationsUiState.Success -> {
                    if (s.items.isEmpty()) {
                        CenteredText(stringResource(R.string.notifications_empty))
                    } else {
                        NotificationsList(s.items) { item ->
                            viewModel.markOpened(item.id)
                            onItemClick(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(
    selected: NotificationsFilter,
    onSelect: (NotificationsFilter) -> Unit,
) {
    TabRow(selectedTabIndex = if (selected == NotificationsFilter.ALL) 0 else 1) {
        Tab(
            selected = selected == NotificationsFilter.ALL,
            onClick = { onSelect(NotificationsFilter.ALL) },
            text = { Text(stringResource(R.string.notifications_filter_all)) },
        )
        Tab(
            selected = selected == NotificationsFilter.UNREAD,
            onClick = { onSelect(NotificationsFilter.UNREAD) },
            text = { Text(stringResource(R.string.notifications_filter_unread)) },
        )
    }
}

@Composable
private fun NotificationsList(
    items: List<NotificationItemDto>,
    onClick: (NotificationItemDto) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
        items(items, key = { it.id }) { item ->
            NotificationRow(item = item, onClick = { onClick(item) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItemDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconBubble()
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    relativeTime(LocalContext.current, item.created_at),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!item.is_read) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun IconBubble() {
    val tint = MaterialTheme.colorScheme.secondary
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Notifications, contentDescription = null, tint = tint)
    }
}

@Composable
private fun CenteredLoader() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}

private fun relativeTime(context: android.content.Context, isoTimestamp: String): String {
    val parsed = try {
        OffsetDateTime.parse(isoTimestamp)
    } catch (_: DateTimeParseException) {
        return ""
    }
    val now = OffsetDateTime.now(parsed.offset)
    val diff = Duration.between(parsed, now)
    return when {
        diff.toMinutes() < 1 -> context.getString(R.string.notifications_relative_now)
        diff.toMinutes() < 60 -> context.getString(R.string.notifications_relative_minutes, diff.toMinutes().toInt())
        diff.toHours() < 24 -> context.getString(R.string.notifications_relative_hours, diff.toHours().toInt())
        diff.toDays() < 7 -> context.getString(R.string.notifications_relative_days, diff.toDays().toInt())
        else -> parsed.toLocalDate().toString()
    }
}
