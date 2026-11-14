package com.ibomaker.ultrazoom

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Slider
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val AppBackground = Color(0xFF080A0F)
private val Panel = Color(0xFF151922)
private val Accent = Color(0xFF8B5CF6)
private val Muted = Color(0xFF9AA3B2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 10, 15)
        window.navigationBarColor = android.graphics.Color.rgb(8, 10, 15)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Accent,
                background = AppBackground,
                surface = Panel,
                onBackground = Color.White,
                onSurface = Color.White
            )) {
                CameraScreen()
            }
        }
    }

    @Composable
    private fun CameraScreen() {
        val context = LocalContext.current
        var hasPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> hasPermission = granted }

        var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
        var torchEnabled by remember { mutableStateOf(false) }
        var selectedZoom by remember { mutableFloatStateOf(1f) }
        var actualZoom by remember { mutableFloatStateOf(1f) }
        var maxZoom by remember { mutableFloatStateOf(1f) }
        var camera by remember { mutableStateOf<Camera?>(null) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
        val previewView = remember {
            PreviewView(this).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        LaunchedEffect(Unit) {
            if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        DisposableEffect(hasPermission, lensFacing) {
            if (!hasPermission) {
                onDispose { }
            } else {
                val providerFuture = ProcessCameraProvider.getInstance(this@MainActivity)
                val listener = Runnable {
                    try {
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()
                        provider.unbindAll()
                        val boundCamera = provider.bindToLifecycle(
                            this@MainActivity, selector, preview, capture
                        )
                        camera = boundCamera
                        imageCapture = capture
                        maxZoom = boundCamera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        selectedZoom = 1f
                        actualZoom = 1f
                        torchEnabled = false
                    } catch (error: Exception) {
                        Toast.makeText(context, "تعذر تشغيل الكاميرا: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
                providerFuture.addListener(listener, ContextCompat.getMainExecutor(this@MainActivity))
                onDispose {
                    camera?.cameraControl?.enableTorch(false)
                    camera = null
                    imageCapture = null
                }
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ULTRAZOOM", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("AI CAMERA  •  BETA", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    Text(
                        text = if (hasPermission) "CAMERA READY" else "PERMISSION REQUIRED",
                        color = if (hasPermission) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.Black)
                ) {
                    if (hasPermission) {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                        // Tap to focus and pinch with two fingers to zoom.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(camera) {
                                    detectTapGestures { point ->
                                        val activeCamera = camera ?: return@detectTapGestures
                                        try {
                                            val meteringPoint = previewView.meteringPointFactory
                                                .createPoint(point.x, point.y)
                                            val action = FocusMeteringAction.Builder(meteringPoint)
                                                .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                                .build()
                                            activeCamera.cameraControl.startFocusAndMetering(action)
                                        } catch (_: Exception) {
                                            // Some front cameras do not expose focus metering.
                                        }
                                    }
                                }
                                .pointerInput(camera, maxZoom) {
                                    detectTransformGestures { _, _, zoomChange, _ ->
                                        val activeCamera = camera ?: return@detectTransformGestures
                                        val current = activeCamera.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                        val target = (current * zoomChange).coerceIn(1f, maxZoom.coerceAtLeast(1f))
                                        actualZoom = target
                                        selectedZoom = target
                                        activeCamera.cameraControl.setZoomRatio(target)
                                    }
                                }
                        )
                        Column(
                            modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
                                .clip(RoundedCornerShape(10.dp)).background(Color(0x99000000)).padding(9.dp)
                        ) {
                            Text("LIVE PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Actual ${"%.1f".format(Locale.US, actualZoom)}x", color = Color(0xFFDDD6FE), fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("نحتاج إذن الكاميرا", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("منح الإذن")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ZOOM CONTROL", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("MAX DEVICE: ${"%.1f".format(Locale.US, maxZoom)}x", color = Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(9.dp))
                val zoomSteps = listOf(1f, 3f, 10f, 30f, 60f, 100f, 180f)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    zoomSteps.forEach { zoom ->
                        val active = selectedZoom == zoom
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Accent else Panel)
                                .border(1.dp, if (active) Accent else Color(0xFF252B38), RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedZoom = zoom
                                    val supported = zoom.coerceAtMost(maxZoom.coerceAtLeast(1f))
                                    actualZoom = supported
                                    camera?.cameraControl?.setZoomRatio(supported)
                                    if (zoom > maxZoom) {
                                        Toast.makeText(context, "الحد الأقصى المدعوم حالياً ${"%.1f".format(Locale.US, maxZoom)}x", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${zoom.toInt()}x",
                                color = if (active) Color.White else Color(0xFFD1D5DB),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (maxZoom > 1.05f) {
                    Slider(
                        value = actualZoom.coerceIn(1f, maxZoom),
                        onValueChange = { value ->
                            actualZoom = value
                            selectedZoom = value
                            camera?.cameraControl?.setZoomRatio(value)
                        },
                        valueRange = 1f..maxZoom,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (selectedZoom > maxZoom) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "هذا الهاتف لا يتيح هذا المستوى عبر واجهة الكاميرا الحالية؛ لم نضف تكبيراً وهمياً.",
                        color = Color(0xFFFBBF24),
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RoundControl(
                        onClick = {
                            torchEnabled = !torchEnabled
                            camera?.cameraControl?.enableTorch(torchEnabled)
                        },
                        label = if (torchEnabled) "الفلاش يعمل" else "الفلاش",
                        icon = if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff
                    )
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(Color.White)
                            .border(5.dp, Accent.copy(alpha = 0.8f), CircleShape)
                            .clickable { takePhoto(imageCapture) },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = "التقاط صورة",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    RoundControl(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        },
                        label = "تبديل",
                        icon = Icons.Filled.FlipCameraAndroid
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AI PHOTO ENHANCEMENT", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("معالجة الصور بالذكاء الاصطناعي", color = Muted, fontSize = 10.sp)
                    }
                    Text("قريباً", color = Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("التحكم بالتقريب يلتزم بالحدود التي يعلنها الهاتف.", color = Muted, fontSize = 9.sp)
            }
        }
    }

    private fun takePhoto(capture: ImageCapture?) {
        if (capture == null) {
            Toast.makeText(this, "الكاميرا لم تصبح جاهزة بعد", Toast.LENGTH_SHORT).show()
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "UltraZoom_$stamp.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/UltraZoom")
        }
        val options = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@MainActivity, "تم حفظ الصورة في Pictures/UltraZoom", Toast.LENGTH_SHORT).show()
                }
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "فشل حفظ الصورة: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    @Composable
    private fun RoundControl(
        onClick: () -> Unit,
        label: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(Panel),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = label, tint = Color.White)
            }
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}
