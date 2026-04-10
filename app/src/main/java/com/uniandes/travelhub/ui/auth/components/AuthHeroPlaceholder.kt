package com.uniandes.travelhub.ui.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uniandes.travelhub.ui.theme.TravelhubBlue300
import com.uniandes.travelhub.ui.theme.TravelhubBlue600
import com.uniandes.travelhub.ui.theme.TravelhubTheme

@Composable
fun AuthHeroPlaceholder(
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TravelhubBlue300, TravelhubBlue600)
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Flight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AuthHeroPlaceholderPreview() {
    TravelhubTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AuthHeroPlaceholder()
        }
    }
}
