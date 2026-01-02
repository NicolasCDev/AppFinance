package com.example.appfinancetest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar (
    onValidateClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onImportExportClick: () -> Unit,
    name: String
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        },
        navigationIcon = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                IconButton(
                    onClick = onValidateClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_validate_investment),
                        contentDescription = "Validate Investment",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = onImportExportClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_import_export),
                        contentDescription = "Import / Export",
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp).padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White
        )
    )
}
