package com.example.appfinancetest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R

@Composable
fun TopBar(
    title: String,
    onImportExportClick: () -> Unit,
    onVisibilityClick: () -> Unit,
    isVisibilityOff: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardBg: Color = MaterialTheme.colorScheme.surface,
    cardBorder: Color = MaterialTheme.colorScheme.outlineVariant,
    textPrimary: Color = MaterialTheme.colorScheme.onSurface
) {
    TopBar(
        titleContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = textPrimary
                )
            )
        },
        onImportExportClick = onImportExportClick,
        onVisibilityClick = onVisibilityClick,
        isVisibilityOff = isVisibilityOff,
        onSettingsClick = onSettingsClick,
        modifier = modifier,
        cardBg = cardBg,
        cardBorder = cardBorder,
        textPrimary = textPrimary
    )
}

@Composable
fun TopBar(
    titleContent: @Composable () -> Unit,
    onImportExportClick: () -> Unit,
    onVisibilityClick: () -> Unit,
    isVisibilityOff: Boolean,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardBg: Color = MaterialTheme.colorScheme.surface,
    cardBorder: Color = MaterialTheme.colorScheme.outlineVariant,
    textPrimary: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        titleContent()

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Import / Export Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, cardBorder, CircleShape)
                    .clickable { onImportExportClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_import_export),
                    contentDescription = "Import / Export",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Visibility Toggle Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, cardBorder, CircleShape)
                    .clickable { onVisibilityClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVisibilityOff) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Visibility",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Settings Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(cardBg)
                    .border(1.dp, cardBorder, CircleShape)
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
