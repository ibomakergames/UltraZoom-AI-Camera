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
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = AppBackground, surface = Panel)) {
                CameraScreen()
            }
        }
    }

    @Composable
    private fun CameraScreen() {
        val context = LocalContext.current
        var hasCameraPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
        }
        var hasAudioPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        }
        val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasCameraPermission = it
        }
        val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            hasAudioPermission = it
            if (!it) Toast.makeText(context, "سيتم تسجيل الفيديو من دون صوت", Toast.LENGTH_SHORT).show()
        }

        var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
        var torchEnabled by remember { mutableStateOf(false) }
        var selectedZoom by remember { mutableFloatStateOf(1f) }
        var actualZoom by remember { mutableFloatStateOf(1f) }
        var maxZoom by remember { mutableFloatStateOf(1f) }
        var camera by remember { mutableStateOf<Camera?>(null) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
        var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
        var recording by remember { mutableStateOf<Recording?>(null) }
        var videoMode by remember { mutableStateOf(false) }
        var lowProcessing by remember { mutableStateOf(true) }
        var superZoom by remember { mutableStateOf(false) }

        val previewView = remember {
            PreviewView(this).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }

        LaunchedEffect(Unit) {
            if (!hasCameraPermission) cameraPermission.launch(Manifest.permission.CAMERA)
        }

        DisposableEffect(hasCameraPermission, lensFacing, lowProcessing) {
            if (!hasCameraPermission) {
                onDispose { }
            } else {
                val future = ProcessCameraProvider.getInstance(this@MainActivity)
                future.addListener({
                    try {
                        val provider = future.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(if (lowProcessing) ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY else ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                            .build()
                        val recorderBuilder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        val video = VideoCapture.withOutput(recorderBuilder.build())
                        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        provider.unbindAll()
                        val bound = provider.bindToLifecycle(this@MainActivity, selector, preview, capture, video)
                        camera = bound
                        imageCapture = capture
                        videoCapture = video
                        maxZoom = bound.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        selectedZoom = 1f
                        actualZoom = 1f
                        torchEnabled = false
                    } catch (e: Exception) {
                        Toast.makeText(context, "تعذر تشغيل الكاميرا: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }, ContextCompat.getMainExecutor(this@MainActivity))
                onDispose {
                    recording?.stop()
                    camera?.cameraControl?.enableTorch(false)
                    camera = null
                    imageCapture = null
                    videoCapture = null
                }
            }
        }

        Surface(Modifier.fillMaxSize(), color = AppBackground) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ULTRAZOOM", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("AI CAMERA • BETA", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    }
                    Text(if (hasCameraPermission) "CAMERA READY" else "PERMISSION REQUIRED", color = if (hasCameraPermission) Color(0xFF6EE7B7) else Color(0xFFFBBF24), fontSize = 10.sp)
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(4.dp)) {
                    ModeButton("صورة", !videoMode) { videoMode = false }
                    ModeButton("فيديو", videoMode) { videoMode = true }
                }
                Spacer(Modifier.height(8.dp))

                Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black)) {
                    if (hasCameraPermission) {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                        Box(
                            Modifier.fillMaxSize()
                                .pointerInput(camera) {
                                    detectTapGestures { p ->
                                        val c = camera ?: return@detectTapGestures
                                        try {
                                            val point = previewView.meteringPointFactory.createPoint(p.x, p.y)
                                            c.cameraControl.startFocusAndMetering(
                                                FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
                                            )
                                        } catch (_: Exception) { }
                                    }
                                }
                                .pointerInput(camera, maxZoom) {
                                    detectTransformGestures { _, _, change, _ ->
                                        val c = camera ?: return@detectTransformGestures
                                        val current = c.cameraInfo.zoomState.value?.zoomRatio ?: 1f
                                        val z = (current * change).coerceIn(1f, maxZoom.coerceAtLeast(1f))
                                        actualZoom = z
                                        selectedZoom = z
                                        c.cameraControl.setZoomRatio(z)
                                    }
                                }
                        )
                        Column(Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xAA000000)).padding(8.dp)) {
                            Text("LIVE PREVIEW", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Zoom ${"%.1f".format(Locale.US, actualZoom)}x", color = Color(0xFFDDD6FE), fontSize = 12.sp)
                            if (superZoom) Text("SUPER ZOOM • DIGITAL", color = Color(0xFFFBBF24), fontSize = 9.sp)
                        }
                    } else {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("نحتاج إذن الكاميرا", color = Color.White)
                            Button(onClick = { cameraPermission.launch(Manifest.permission.CAMERA) }) { Text("منح الإذن") }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("التقريب", color = Muted, fontSize = 11.sp)
                    Text("الحد الفعلي للهاتف: ${"%.1f".format(Locale.US, maxZoom)}X", color = Muted, fontSize = 10.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(1f, 3f, 10f, 30f, 60f, 100f).forEach { z ->
                        val active = selectedZoom == z
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
                                .background(if (active) Accent else Panel)
                                .border(1.dp, if (active) Accent else Color(0xFF252B38), RoundedCornerShape(9.dp))
                                .clickable {
                                    selectedZoom = z
                                    superZoom = z > maxZoom
                                    val physical = z.coerceAtMost(maxZoom.coerceAtLeast(1f))
                                    actualZoom = physical
                                    camera?.cameraControl?.setZoomRatio(physical)
                                    if (z > maxZoom) Toast.makeText(context, "تم اختيار وضع 100X الرقمي؛ المعاينة الفعلية محدودة بقدرات الكاميرا حالياً", Toast.LENGTH_LONG).show()
                                }.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("${z.toInt()}X", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                }
                if (maxZoom > 1.05f) {
                    Slider(value = actualZoom.coerceIn(1f, maxZoom), onValueChange = {
                        actualZoom = it
                        selectedZoom = it
                        superZoom = false
                        camera?.cameraControl?.setZoomRatio(it)
                    }, valueRange = 1f..maxZoom, modifier = Modifier.fillMaxWidth())
                }

                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Panel).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("أقل معالجة من التطبيق", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("بدون فلاتر أو تحسينات مضافة من UltraZoom", color = Muted, fontSize = 9.sp)
                    }
                    Switch(checked = lowProcessing, onCheckedChange = {
                        lowProcessing = it
                        Toast.makeText(context, "سيُطبّق إعداد الالتقاط عند إعادة ربط الكاميرا", Toast.LENGTH_SHORT).show()
                    })
                }
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("Super Zoom 100X", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("تكبير رقمي؛ لا يوجد تحسين AI حقيقي مدمج بعد", color = Muted, fontSize = 9.sp)
                    }
                    Switch(checked = superZoom, onCheckedChange = { enabled ->
                        superZoom = enabled
                        if (enabled) {
                            selectedZoom = 100f
                            actualZoom = maxZoom.coerceAtLeast(1f)
                            camera?.cameraControl?.setZoomRatio(actualZoom)
                            Toast.makeText(context, "100X يعرض الحد الأقصى الفعلي حالياً؛ المعالجة الذكية قيد التطوير", Toast.LENGTH_LONG).show()
                        } else {
                            selectedZoom = 1f
                            actualZoom = 1f
                            camera?.cameraControl?.setZoomRatio(1f)
                        }
                    })
                }

                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    RoundControl(if (torchEnabled) "الفلاش يعمل" else "الفلاش", if (torchEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff) {
                        torchEnabled = !torchEnabled
                        camera?.cameraControl?.enableTorch(torchEnabled)
                    }
                    Box(
                        Modifier.size(68.dp).clip(CircleShape).background(if (recording != null) Color(0xFFEF4444) else Color.White)
                            .border(5.dp, Accent.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                if (videoMode) {
                                    if (recording != null) {
                                        recording?.stop()
                                        recording = null
                                    } else if (!hasAudioPermission) {
                                        audioPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        startVideo(videoCapture, hasAudioPermission, onStarted = { newRecording -> recording = newRecording })
                                    }
                                } else takePhoto(imageCapture)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (videoMode) {
                            Icon(Icons.Filled.Videocam, contentDescription = "تسجيل فيديو", tint = if (recording != null) Color.White else Color.Black, modifier = Modifier.size(30.dp))
                        } else {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = "التقاط صورة", tint = Color.Black, modifier = Modifier.size(30.dp))
                        }
                    }
                    RoundControl("تبديل", Icons.Filled.FlipCameraAndroid) {
                        if (recording != null) Toast.makeText(context, "أوقف التسجيل قبل تبديل الكاميرا", Toast.LENGTH_SHORT).show()
                        else lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                    }
                }
                Text(if (videoMode) if (recording != null) "اضغط لإيقاف التسجيل" else "اضغط لبدء تسجيل الفيديو" else "الصور تحفظ في Pictures/UltraZoom", color = Muted, fontSize = 9.sp)
            }
        }
    }

    @Composable
    private fun RowScope.ModeButton(label: String, active: Boolean, onClick: () -> Unit) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (active) Accent else Color.Transparent).clickable(onClick = onClick).padding(9.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }

    private fun startVideo(video: VideoCapture<Recorder>?, withAudio: Boolean, onStarted: (Recording?) -> Unit) {
        if (video == null) {
            Toast.makeText(this, "الكاميرا لم تصبح جاهزة بعد", Toast.LENGTH_SHORT).show()
            return
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "UltraZoom_$stamp.mp4")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/UltraZoom")
        }
        val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values).build()
        try {
            var pending = video.output.prepareRecording(this, output)
            if (withAudio) pending = pending.withAudioEnabled()
            val active = pending.start(ContextCompat.getMainExecutor(this)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    if (event.hasError()) Toast.makeText(this, "فشل تسجيل الفيديو: ${event.error}", Toast.LENGTH_LONG).show()
                    else Toast.makeText(this, "تم حفظ الفيديو في Movies/UltraZoom", Toast.LENGTH_SHORT).show()
                    onStarted(null)
                }
            }
            onStarted(active)
        } catch (e: Exception) {
            Toast.makeText(this, "تعذر بدء الفيديو: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
        val output = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                Toast.makeText(this@MainActivity, "تم حفظ الصورة في Pictures/UltraZoom", Toast.LENGTH_SHORT).show()
            }
            override fun onError(error: ImageCaptureException) {
                Toast.makeText(this@MainActivity, "فشل حفظ الصورة: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    @Composable
    private fun RoundControl(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable(onClick = onClick).padding(4.dp)) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Panel), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = Color.White)
            }
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}
