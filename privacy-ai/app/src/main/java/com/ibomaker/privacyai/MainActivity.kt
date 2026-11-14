package com.ibomaker.privacyai

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF080914)
private val Surface = Color(0xFF151626)
private val Purple = Color(0xFF8655FF)
private val Muted = Color(0xFFAAA8C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("privacy_ai", MODE_PRIVATE)
        setContent {
            var protection by remember { mutableStateOf(prefs.getBoolean("protection", true)) }
            var level by remember { mutableFloatStateOf(prefs.getFloat("level", 0.85f)) }

            // FLAG_SECURE protects only this app's window, not other apps or the whole display.
            SideEffect {
                if (protection) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple,
                    background = Background,
                    surface = Surface,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides
                        androidx.compose.ui.unit.LayoutDirection.Rtl
                ) {
                    Column(
                        Modifier.fillMaxSize()
                            .background(Background)
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Shield, null, tint = Purple, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Privacy AI", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("خصوصيتك أولويتنا", color = Muted, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.Security, contentDescription = "الخصوصية", tint = Muted)
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PhonelinkLock, null, tint = Purple, modifier = Modifier.size(76.dp))
                                Text("حماية نافذة التطبيق", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "منع لقطات الشاشة وتسجيل المحتوى داخل Privacy AI",
                                    color = Muted,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp
                                )
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (protection) "الحماية مفعّلة" else "الحماية متوقفة", fontWeight = FontWeight.Bold)
                                        Text("تطبق على هذه النافذة فقط", color = Muted, fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = protection,
                                        onCheckedChange = {
                                            protection = it
                                            prefs.edit().putBoolean("protection", it).apply()
                                        }
                                    )
                                }
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                Text("مستوى الخصوصية", color = Purple, fontWeight = FontWeight.Bold)
                                Text(
                                    if (level > .75f) "مرتفع" else if (level > .4f) "متوسط" else "أساسي",
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Slider(
                                    value = level,
                                    onValueChange = {
                                        level = it
                                        prefs.edit().putFloat("level", it).apply()
                                    },
                                    colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple)
                                )
                                Text(
                                    "مؤشر بصري للتفضيل فقط؛ لا يغيّر زاوية رؤية الشاشة أو سطوعها.",
                                    color = Muted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Text("ميزات قيد التطوير", color = Muted, fontWeight = FontWeight.Bold)
                        InfoCard(
                            icon = Icons.Default.NotificationsOff,
                            title = "إخفاء الإشعارات",
                            detail = "غير مفعّل حالياً. يتطلب تكاملاً خاصاً مع إعدادات وصلاحيات Android، ولن نطلب صلاحية حساسة دون حاجة واضحة."
                        )
                        InfoCard(
                            icon = Icons.Default.Apps,
                            title = "حماية تطبيقات محددة",
                            detail = "قائمة التطبيقات وخيارات الحماية لها لم تُنفّذ بعد. الحماية الحالية لا تمتد إلى التطبيقات الأخرى."
                        )
                        InfoCard(
                            icon = Icons.Default.Schedule,
                            title = "الجدولة التلقائية",
                            detail = "ستضاف لاحقاً بعد تنفيذ جدولة فعلية وإظهار حالة عملها بوضوح."
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF211A3A)),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Purple, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "مهم: لا يستطيع تطبيق عادي تقليد ميزة Samsung Privacy Display التي تعتمد على عتاد خاص بالشاشة لإخفاء الرؤية من الجوانب.",
                                    color = Color(0xFFD8D1F4),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Text(
                            "Privacy AI • الإصدار 0.1.1",
                            color = Muted,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.LockClock, null, tint = Muted)
        }
    }
}
