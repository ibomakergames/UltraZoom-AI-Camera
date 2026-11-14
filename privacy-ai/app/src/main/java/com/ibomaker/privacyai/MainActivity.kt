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
            var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", false)) }
            var schedule by remember { mutableStateOf(prefs.getBoolean("schedule", false)) }
            var level by remember { mutableFloatStateOf(prefs.getFloat("level", 0.85f)) }
            var showAppsInfo by remember { mutableStateOf(false) }

            // Android permits FLAG_SECURE only for this app's own window.
            SideEffect {
                if (protection) window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                ) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
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
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    Column(
                        Modifier.fillMaxSize().background(Background)
                            .verticalScroll(rememberScrollState()).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Shield, null, tint = Purple, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Privacy AI", fontSize = 29.sp, fontWeight = FontWeight.Bold)
                                Text("خصوصيتك أولويتنا", color = Muted, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.Settings, contentDescription = "الإعدادات", tint = Muted)
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PhonelinkLock, null, tint = Purple, modifier = Modifier.size(76.dp))
                                Text("حماية خصوصية الشاشة", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Text("تحكم سريع بإعدادات الحماية على هذا الجهاز", color = Muted, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(14.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (protection) "الحماية مفعّلة" else "الحماية متوقفة", fontWeight = FontWeight.Bold)
                                        Text("منع لقطات الشاشة داخل Privacy AI", color = Muted, fontSize = 12.sp)
                                    }
                                    Switch(checked = protection, onCheckedChange = {
                                        protection = it; prefs.edit().putBoolean("protection", it).apply()
                                    })
                                }
                            }
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(22.dp)) {
                            Column(Modifier.padding(18.dp)) {
                                Text("مستوى الخصوصية", color = Purple, fontWeight = FontWeight.Bold)
                                Text(if (level > .75f) "مرتفع" else if (level > .4f) "متوسط" else "أساسي", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Slider(value = level, onValueChange = { level = it; prefs.edit().putFloat("level", it).apply() }, colors = SliderDefaults.colors(thumbColor = Purple, activeTrackColor = Purple))
                                Text("هذا المؤشر يضبط تفضيلك داخل التطبيق، ولا يغيّر زاوية رؤية الشاشة فعلياً.", color = Muted, fontSize = 12.sp)
                            }
                        }

                        Text("خيارات إضافية", color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        SettingRow(Icons.Default.NotificationsOff, "إخفاء الإشعارات", "إعداد أولي؛ يتطلب صلاحية الوصول للإشعارات لتطبيقه على النظام", notifications) {
                            notifications = it; prefs.edit().putBoolean("notifications", it).apply()
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Apps, null, tint = Purple, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("التطبيقات المحمية", fontWeight = FontWeight.Bold)
                                    Text("قائمة التطبيقات ستضاف في المرحلة التالية", color = Muted, fontSize = 12.sp)
                                }
                                IconButton(onClick = { showAppsInfo = true }) { Icon(Icons.Default.ChevronLeft, null, tint = Muted) }
                            }
                        }
                        SettingRow(Icons.Default.Schedule, "الجدولة التلقائية", "تفضيل الجدولة محفوظ محلياً؛ لم تُفعّل مهام النظام بعد", schedule) {
                            schedule = it; prefs.edit().putBoolean("schedule", it).apply()
                        }

                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF211A3A)), shape = RoundedCornerShape(18.dp)) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Purple, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("تنبيه: لا يستطيع أي تطبيق عادي جعل الشاشة مظلمة من الجوانب مثل تقنية سامسونج؛ ذلك يحتاج إلى عتاد شاشة مخصص.", color = Color(0xFFD8D1F4), fontSize = 12.sp)
                            }
                        }
                        Text("Privacy AI • الإصدار 0.1.0", color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    }
                }
            }
            if (showAppsInfo) AlertDialog(
                onDismissRequest = { showAppsInfo = false },
                title = { Text("التطبيقات المحمية") },
                text = { Text("سنضيف اختيار التطبيقات وتفعيل الحماية لها بعد تنفيذ خدمة الصلاحيات بشكل آمن.") },
                confirmButton = { TextButton(onClick = { showAppsInfo = false }) { Text("حسناً") } }
            )
        }
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}
