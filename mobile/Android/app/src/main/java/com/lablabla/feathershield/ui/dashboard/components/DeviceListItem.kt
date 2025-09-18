package com.lablabla.feathershield.ui.dashboard.components

import android.content.res.Configuration
import android.graphics.drawable.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery5Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lablabla.feathershield.data.model.Device
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListItem(device: Device, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon Column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Battery5Bar,
                    contentDescription = "Battery Level",
                    tint = if (device.batteryLevel > 20) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text("${device.batteryLevel}%", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Device ID: ${device.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Firmware: ${device.fwVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Update Indicator
            if (device.isUpdateAvailable) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

// --- PREVIEW SECTION ---

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
fun DeviceListItemPreview_Light() {
    FeatherShieldTheme {
        Surface {
            val device = Device(batteryLevel = 80)
            DeviceListItem(
                device,
                onClick = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DeviceListItemPreview_Dark() {
    FeatherShieldTheme {
        Surface {
            val device = Device(batteryLevel = 80)
            DeviceListItem(
                device,
                onClick = {}
            )
        }
    }
}

@Preview(name = "Loading State", showBackground = true, widthDp = 360)
@Composable
fun DeviceListItemPreview_Loading() {
    FeatherShieldTheme {
        Surface {
            val device = Device()
            DeviceListItem(
                device,
                onClick = {}
            )
        }
    }
}