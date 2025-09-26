package com.lablabla.feathershield.ui.dashboard.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.NestCamWiredStand
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.lablabla.feathershield.ui.dashboard.DashboardAction
import com.lablabla.feathershield.ui.theme.FeatherShieldTheme

@Composable
fun DeviceCard(
    device: Device,
    onAction: (DashboardAction) -> Unit
) {
    Card(
        onClick = { onAction(DashboardAction.OnDeviceClick(device.id)) },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NestCamWiredStand,
                    contentDescription = "Device Icon",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val batteryColor = when {
                        device.batteryLevel > 60 -> MaterialTheme.colorScheme.primary
                        device.batteryLevel > 20 -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.error
                    }
                    val batteryIcon = when {
                        device.batteryLevel > 90 -> Icons.Default.BatteryFull
                        device.batteryLevel > 60 -> Icons.Default.Battery6Bar
                        device.batteryLevel > 40 -> Icons.Default.Battery4Bar
                        device.batteryLevel > 20 -> Icons.Default.Battery2Bar
                        else -> Icons.Default.BatteryAlert
                    }
                    Icon(
                        imageVector = batteryIcon,
                        contentDescription = "Battery Level",
                        modifier = Modifier.size(16.dp),
                        tint = batteryColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${device.batteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = batteryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (device.isUpdateAvailable) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Update",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// --- PREVIEW SECTION ---

@Preview(name = "Light Mode", showBackground = true, widthDp = 360)
@Composable
fun DeviceCardPreview_Light() {
    FeatherShieldTheme {
        Surface {
            val device = Device(name = "Woof", batteryLevel = 80)
            DeviceCard(
                device,
                onAction = {}
            )
        }
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360)
@Composable
fun DeviceCardPreview_Dark() {
    FeatherShieldTheme {
        Surface {
            val device = Device(name = "Meow", batteryLevel = 50)
            DeviceCard(
                device,
                onAction = {}
            )
        }
    }
}
