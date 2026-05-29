const fs = require('fs');

let appendMe = `
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val isArabic by AppConfig.isArabic.collectAsState()
    val errorLogs by AppConfig.errorLogs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberDarkCard,
        titleContentColor = CyberTextHigh,
        textContentColor = CyberTextHigh,
        title = {
            Text(Trans.ts("GLOBAL APP PREFERENCES"), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Language Toggle
                Column {
                    Text(Trans.ts("Language"), color = CyberNeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("English", color = if (!isArabic) CyberTextHigh else CyberTextDim)
                        Switch(
                            checked = isArabic,
                            onCheckedChange = { AppConfig.isArabic.value = it },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBackground,
                                checkedTrackColor = CyberNeonRed,
                                uncheckedThumbColor = CyberTextDim,
                                uncheckedTrackColor = CyberDarkCard
                            )
                        )
                        Text("العربية", color = if (isArabic) CyberTextHigh else CyberTextDim)
                    }
                    Text(Trans.ts("System Language Overlay"), fontSize = 10.sp, color = CyberTextDim)
                }

                HorizontalDivider(color = CyberDarkCrimson)

                // Error Logs
                Column {
                    Text(Trans.ts("Error Logs"), color = CyberNeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(Trans.ts("API Network Exceptions"), fontSize = 10.sp, color = CyberTextDim)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorLogs.isEmpty()) {
                        Text("No errors tracked.", color = CyberTextDim, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    } else {
                        errorLogs.forEach { log ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(CyberDarkSurface, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(log, color = CyberTextHigh, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Trans.ts("CANCEL"), color = CyberTextHigh)
            }
        }
    )
}
`;

fs.appendFileSync('app/src/main/java/com/example/ui/DashboardScreen.kt', appendMe);
