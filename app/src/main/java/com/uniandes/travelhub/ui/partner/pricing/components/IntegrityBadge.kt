package com.uniandes.travelhub.ui.partner.pricing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.R
import com.uniandes.travelhub.viewmodels.IntegrityState

@Composable
fun IntegrityBadge(state: IntegrityState, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (state) {
        IntegrityState.Normal -> Triple(
            Color(0xFFE6F4EA),
            Color(0xFF1E7E34),
            stringResource(R.string.partner_pricing_integrity_normal),
        )
        IntegrityState.Locked -> Triple(
            Color(0xFFFFF3CD),
            Color(0xFF856404),
            stringResource(R.string.partner_pricing_integrity_locked),
        )
        IntegrityState.Compromised -> Triple(
            Color(0xFFF8D7DA),
            Color(0xFF721C24),
            stringResource(R.string.partner_pricing_integrity_compromised),
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        maxLines = 1,
        softWrap = false,
        modifier = modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
