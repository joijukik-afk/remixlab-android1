package com.joijukik.remixlab.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigateToEditor: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎧", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "RemixLab",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "استودیوی ریمیکس در جیب شما",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(48.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("✨ امکانات نسخه ۲",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• میکس چند آهنگ همزمان")
                Text("• حذف خواننده و بیت (AI)")
                Text("• اکولایزر ۱۰ باندی حرفه‌ای")
                Text("• کمپرسور چندباندی و لیمیتر")
                Text("• پریست‌های COMA Audio Lab")
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onNavigateToEditor,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("شروع ریمیکس", style = MaterialTheme.typography.titleLarge)
        }
    }
}
