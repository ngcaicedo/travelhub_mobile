package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.utils.PasswordStrength
import com.uniandes.travelhub.utils.PasswordTier
import com.uniandes.travelhub.ui.theme.Slate200
import com.uniandes.travelhub.ui.theme.TravelhubTheme
import com.uniandes.travelhub.ui.theme.extendedColors
import com.uniandes.travelhub.ui.theme.spacing

@Composable
fun PasswordStrengthMeter(
    password: String,
    modifier: Modifier = Modifier,
) {
    val tier = PasswordStrength.tier(password)
    val activeBars = when (tier) {
        PasswordTier.NONE -> 0
        PasswordTier.WEAK -> 1
        PasswordTier.MEDIUM -> 2
        PasswordTier.STRONG -> 3
        PasswordTier.VERY_STRONG -> 4
    }
    val activeColor = when (tier) {
        PasswordTier.NONE -> Slate200
        PasswordTier.WEAK -> MaterialTheme.colorScheme.error
        PasswordTier.MEDIUM -> MaterialTheme.extendedColors.warning
        PasswordTier.STRONG, PasswordTier.VERY_STRONG -> MaterialTheme.extendedColors.success
    }
    val label = when (tier) {
        PasswordTier.NONE -> ""
        PasswordTier.WEAK -> "Débil"
        PasswordTier.MEDIUM -> "Media"
        PasswordTier.STRONG -> "Fuerte"
        PasswordTier.VERY_STRONG -> "Muy fuerte"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            repeat(4) { index ->
                val color: Color = if (index < activeBars) activeColor else Slate200
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(color = color, shape = RoundedCornerShape(3.dp))
                )
            }
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = activeColor,
                modifier = Modifier.padding(top = MaterialTheme.spacing.xs)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun PasswordStrengthMeterPreviews() {
    TravelhubTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PasswordStrengthMeter(password = "")
            PasswordStrengthMeter(password = "abcdefgh")
            PasswordStrengthMeter(password = "Abcdefgh")
            PasswordStrengthMeter(password = "Abcdefg1")
            PasswordStrengthMeter(password = "Abcdefg1!")
        }
    }
}
