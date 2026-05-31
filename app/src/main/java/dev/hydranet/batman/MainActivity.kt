package dev.hydranet.batman

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.hydranet.batman.ui.theme.BatmanTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        BatteryWarningStore.ensureInitialized(this)
        BatteryWarningNotifier.createNotificationChannel(this)
        BatteryMonitorJobService.schedule(this)
        BatteryWarningNotifier.checkAndNotify(this)

        setContent {
            BatmanTheme {
                val context = LocalContext.current
                var thresholds by remember { mutableStateOf(BatteryWarningStore.getThresholds(context)) }
                var selectedThreshold by remember { mutableIntStateOf(suggestedThreshold(thresholds)) }
                var currentBatteryPercent by remember {
                    mutableStateOf(BatteryWarningNotifier.currentBatteryPercent(context))
                }
                var notificationsAllowed by remember {
                    mutableStateOf(BatteryWarningNotifier.canPostNotifications(context))
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    notificationsAllowed = granted
                    if (granted) BatteryWarningNotifier.checkAndNotify(context)
                }

                DisposableEffect(context) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context, intent: Intent) {
                            currentBatteryPercent = BatteryWarningNotifier.currentBatteryPercent(context)
                            BatteryWarningNotifier.checkAndNotify(context)
                        }
                    }
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )

                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }

                BatteryWarningsScreen(
                    thresholds = thresholds,
                    selectedThreshold = selectedThreshold,
                    currentBatteryPercent = currentBatteryPercent,
                    notificationsAllowed = notificationsAllowed,
                    onSelectedThresholdChange = { selectedThreshold = it },
                    onAddThreshold = {
                        thresholds = BatteryWarningStore.addThreshold(context, selectedThreshold)
                        selectedThreshold = suggestedThreshold(thresholds)
                        BatteryWarningNotifier.checkAndNotify(context)
                    },
                    onRemoveThreshold = { threshold ->
                        thresholds = BatteryWarningStore.removeThreshold(context, threshold)
                    },
                    onRequestNotifications = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryWarningsScreen(
    thresholds: List<Int>,
    selectedThreshold: Int,
    currentBatteryPercent: Int?,
    notificationsAllowed: Boolean,
    onSelectedThresholdChange: (Int) -> Unit,
    onAddThreshold: () -> Unit,
    onRemoveThreshold: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Battery warnings",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CurrentBatteryPanel(currentBatteryPercent)
            }

            if (!notificationsAllowed) {
                item {
                    NotificationPermissionPanel(onRequestNotifications)
                }
            }

            item {
                AddThresholdPanel(
                    threshold = selectedThreshold,
                    existingThresholds = thresholds,
                    onThresholdChange = onSelectedThresholdChange,
                    onAddThreshold = onAddThreshold,
                )
            }

            item {
                Text(
                    text = "Configured levels",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (thresholds.isEmpty()) {
                item {
                    EmptyThresholdsPanel()
                }
            } else {
                items(thresholds, key = { threshold -> threshold }) { threshold ->
                    ThresholdRow(
                        threshold = threshold,
                        onRemove = { onRemoveThreshold(threshold) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentBatteryPanel(currentBatteryPercent: Int?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.BatteryFull,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Current battery",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = currentBatteryPercent?.let { "$it%" } ?: "Unknown",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun NotificationPermissionPanel(onRequestNotifications: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notifications are off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Enable alerts",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(onClick = onRequestNotifications) {
                Text("Allow")
            }
        }
    }
}

@Composable
private fun AddThresholdPanel(
    threshold: Int,
    existingThresholds: List<Int>,
    onThresholdChange: (Int) -> Unit,
    onAddThreshold: () -> Unit,
) {
    val isDuplicate = threshold in existingThresholds

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Add level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$threshold%",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onAddThreshold,
                    enabled = !isDuplicate,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add")
                }
            }
            Slider(
                value = threshold.toFloat(),
                onValueChange = { value ->
                    onThresholdChange(value.roundToInt().coerceIn(1, 100))
                },
                valueRange = 1f..100f,
                steps = 98,
            )
        }
    }
}

@Composable
private fun ThresholdRow(
    threshold: Int,
    onRemove: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = "$threshold%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            leadingContent = {
                Icon(
                    imageVector = Icons.Outlined.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remove $threshold% warning",
                    )
                }
            },
        )
    }
}

@Composable
private fun EmptyThresholdsPanel() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No levels configured",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun suggestedThreshold(thresholds: List<Int>): Int {
    return sequenceOf(20, 15, 10, 5, 30, 50)
        .firstOrNull { it !in thresholds }
        ?: 25
}

@Preview(showBackground = true)
@Composable
private fun BatteryWarningsScreenPreview() {
    BatmanTheme {
        BatteryWarningsScreen(
            thresholds = listOf(50, 20, 10),
            selectedThreshold = 15,
            currentBatteryPercent = 34,
            notificationsAllowed = false,
            onSelectedThresholdChange = {},
            onAddThreshold = {},
            onRemoveThreshold = {},
            onRequestNotifications = {},
        )
    }
}
