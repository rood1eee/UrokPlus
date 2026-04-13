package com.example.urokplus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AttendanceScreen(
    textColor: Color,
    scheduleBg: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Посещаемость", color = textColor, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(20.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items((1..31).toList()) { day ->
                val isWeekend = day % 7 == 0 || day % 7 == 6
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (day < 20 && !isWeekend) Color(0xFF68B93E).copy(0.2f) else Color.Transparent)
                        .border(1.dp, if (day < 20 && !isWeekend) Color(0xFF68B93E) else textColor.copy(0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$day", color = if (isWeekend) Color.Gray else textColor)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(Color(0xFF68B93E), CircleShape))
            Text(" — Присутствовал", color = textColor, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
