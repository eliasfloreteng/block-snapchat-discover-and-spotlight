package se.floreteng.spotlightblocker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var blockedApps by remember { mutableStateOf(AppSettings.getBlockedApps(context)) }
    var showAddAppDialog by remember { mutableStateOf(false) }
    var editingApp by remember { mutableStateOf<BlockedApp?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAppDialog = true }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Blocked Apps",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (blockedApps.isEmpty()) {
                Text(
                    text = "No apps blocked. Tap + to add an app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(blockedApps) { app ->
                        BlockedAppCard(
                            app = app,
                            onEdit = { editingApp = app },
                            onDelete = {
                                blockedApps = blockedApps.filter { it.packageName != app.packageName }
                                AppSettings.saveBlockedApps(context, blockedApps)
                            }
                        )
                    }
                }
            }
        }

        if (showAddAppDialog) {
            AddAppDialog(
                onDismiss = { showAddAppDialog = false },
                onAdd = { app ->
                    blockedApps = blockedApps + app
                    AppSettings.saveBlockedApps(context, blockedApps)
                    showAddAppDialog = false
                }
            )
        }

        if (editingApp != null) {
            EditAppDialog(
                app = editingApp!!,
                onDismiss = { editingApp = null },
                onSave = { updatedApp ->
                    blockedApps = blockedApps.map {
                        if (it.packageName == editingApp!!.packageName) updatedApp else it
                    }
                    AppSettings.saveBlockedApps(context, blockedApps)
                    editingApp = null
                }
            )
        }
    }
}

@Composable
fun BlockedAppCard(
    app: BlockedApp,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Blocked strings:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            app.blockedStrings.forEach { string ->
                Text(
                    text = "• $string",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AddAppDialog(
    onDismiss: () -> Unit,
    onAdd: (BlockedApp) -> Unit
) {
    var appName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    var blockedStringsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Blocked App") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package Name") },
                    placeholder = { Text("com.example.app") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = blockedStringsText,
                    onValueChange = { blockedStringsText = it },
                    label = { Text("Blocked Strings (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (appName.isNotBlank() && packageName.isNotBlank()) {
                        val blockedStrings = blockedStringsText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        onAdd(BlockedApp(packageName, appName, blockedStrings))
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditAppDialog(
    app: BlockedApp,
    onDismiss: () -> Unit,
    onSave: (BlockedApp) -> Unit
) {
    var appName by remember { mutableStateOf(app.appName) }
    var blockedStringsText by remember { mutableStateOf(app.blockedStrings.joinToString("\n")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${app.appName}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Package: ${app.packageName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = blockedStringsText,
                    onValueChange = { blockedStringsText = it },
                    label = { Text("Blocked Strings (one per line)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (appName.isNotBlank()) {
                        val blockedStrings = blockedStringsText
                            .split("\n")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }

                        onSave(BlockedApp(app.packageName, appName, blockedStrings))
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
