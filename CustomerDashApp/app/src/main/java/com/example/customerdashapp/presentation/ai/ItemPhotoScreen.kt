package com.example.customerdashapp.presentation.ai

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.customerdashapp.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Data class for a detected item label from ML Kit.
 */
data class DetectedLabel(
    val text: String,
    val vietnameseText: String,
    val confidence: Float,
    val isSelected: Boolean = true
)

/**
 * English -> Vietnamese translation map for common ML Kit labels.
 * ML Kit returns labels in English, we display Vietnamese to the user.
 */
private val labelTranslations = mapOf(
    // Furniture
    "furniture" to "Đồ nội thất",
    "chair" to "Ghế",
    "table" to "Bàn",
    "desk" to "Bàn làm việc",
    "couch" to "Ghế sofa",
    "sofa" to "Ghế sofa",
    "bed" to "Giường",
    "shelf" to "Kệ",
    "cabinet" to "Tủ",
    "drawer" to "Ngăn kéo",
    "wardrobe" to "Tủ quần áo",
    "bookshelf" to "Kệ sách",
    "stool" to "Ghế đẩu",
    "bench" to "Băng ghế",
    "mattress" to "Nệm",
    "pillow" to "Gối",
    "cushion" to "Đệm",
    "mirror" to "Gương",
    "lamp" to "Đèn",
    "curtain" to "Rèm cửa",
    "rug" to "Thảm",
    "carpet" to "Thảm trải sàn",

    // Electronics
    "electronic device" to "Thiết bị điện tử",
    "electronics" to "Đồ điện tử",
    "computer" to "Máy tính",
    "laptop" to "Máy tính xách tay",
    "monitor" to "Màn hình",
    "screen" to "Màn hình",
    "display" to "Màn hình",
    "keyboard" to "Bàn phím",
    "mouse" to "Chuột máy tính",
    "phone" to "Điện thoại",
    "mobile phone" to "Điện thoại di động",
    "smartphone" to "Điện thoại thông minh",
    "tablet" to "Máy tính bảng",
    "television" to "Tivi",
    "tv" to "Tivi",
    "speaker" to "Loa",
    "headphone" to "Tai nghe",
    "headphones" to "Tai nghe",
    "camera" to "Máy ảnh",
    "printer" to "Máy in",
    "charger" to "Sạc",
    "cable" to "Dây cáp",
    "wire" to "Dây điện",
    "battery" to "Pin",
    "fan" to "Quạt",
    "air conditioner" to "Máy lạnh",
    "refrigerator" to "Tủ lạnh",
    "washing machine" to "Máy giặt",
    "microwave" to "Lò vi sóng",
    "oven" to "Lò nướng",
    "blender" to "Máy xay sinh tố",
    "rice cooker" to "Nồi cơm điện",
    "iron" to "Bàn ủi",
    "vacuum cleaner" to "Máy hút bụi",

    // Books & Stationery
    "book" to "Sách",
    "notebook" to "Vở",
    "document" to "Tài liệu",
    "paper" to "Giấy",
    "pen" to "Bút",
    "pencil" to "Bút chì",
    "magazine" to "Tạp chí",
    "newspaper" to "Báo",
    "envelope" to "Phong bì",
    "folder" to "Bìa hồ sơ",

    // Containers & Packaging
    "box" to "Hộp",
    "package" to "Gói hàng",
    "parcel" to "Bưu kiện",
    "container" to "Thùng chứa",
    "bag" to "Túi",
    "backpack" to "Ba lô",
    "suitcase" to "Vali",
    "luggage" to "Hành lý",
    "basket" to "Giỏ",
    "bucket" to "Xô",
    "jar" to "Lọ",
    "can" to "Lon",
    "carton" to "Thùng carton",
    "crate" to "Thùng gỗ",
    "barrel" to "Thùng tròn",
    "wrap" to "Bọc",
    "packaging" to "Bao bì",
    "plastic bag" to "Túi ni lông",

    // Food & Drinks
    "food" to "Thức ăn",
    "drink" to "Đồ uống",
    "bottle" to "Chai",
    "cup" to "Cốc",
    "glass" to "Ly",
    "plate" to "Đĩa",
    "bowl" to "Tô",
    "pot" to "Nồi",
    "pan" to "Chảo",
    "fruit" to "Trái cây",
    "vegetable" to "Rau",
    "meat" to "Thịt",
    "bread" to "Bánh mì",
    "cake" to "Bánh",
    "snack" to "Đồ ăn vặt",
    "rice" to "Gạo",
    "noodle" to "Mì",
    "water" to "Nước",
    "juice" to "Nước ép",
    "coffee" to "Cà phê",
    "tea" to "Trà",
    "milk" to "Sữa",
    "wine" to "Rượu vang",
    "beer" to "Bia",

    // Clothing & Fashion
    "clothing" to "Quần áo",
    "clothes" to "Quần áo",
    "shirt" to "Áo",
    "t-shirt" to "Áo thun",
    "pants" to "Quần",
    "jeans" to "Quần jean",
    "dress" to "Váy",
    "skirt" to "Chân váy",
    "jacket" to "Áo khoác",
    "coat" to "Áo choàng",
    "sweater" to "Áo len",
    "hat" to "Mũ",
    "cap" to "Nón",
    "shoe" to "Giày",
    "shoes" to "Giày",
    "boot" to "Giày bốt",
    "sandal" to "Dép",
    "sock" to "Tất",
    "glove" to "Găng tay",
    "scarf" to "Khăn quàng",
    "watch" to "Đồng hồ",
    "glasses" to "Kính",
    "sunglasses" to "Kính râm",
    "jewelry" to "Trang sức",
    "ring" to "Nhẫn",
    "necklace" to "Dây chuyền",
    "bracelet" to "Vòng tay",

    // Tools & Hardware
    "tool" to "Dụng cụ",
    "hammer" to "Búa",
    "screwdriver" to "Tua vít",
    "wrench" to "Cờ lê",
    "pliers" to "Kìm",
    "drill" to "Máy khoan",
    "saw" to "Cưa",
    "nail" to "Đinh",
    "screw" to "Ốc vít",
    "tape" to "Băng keo",
    "rope" to "Dây thừng",
    "chain" to "Dây xích",
    "lock" to "Ổ khóa",
    "key" to "Chìa khóa",

    // Toys & Sports 
    "toy" to "Đồ chơi",
    "doll" to "Búp bê",
    "ball" to "Bóng",
    "bicycle" to "Xe đạp",
    "bike" to "Xe đạp",
    "helmet" to "Mũ bảo hiểm",
    "racket" to "Vợt",

    // Home & Garden
    "plant" to "Cây",
    "flower" to "Hoa",
    "vase" to "Bình hoa",
    "clock" to "Đồng hồ treo tường",
    "picture" to "Tranh ảnh",
    "painting" to "Tranh vẽ",
    "frame" to "Khung ảnh",
    "decoration" to "Đồ trang trí",
    "candle" to "Nến",
    "towel" to "Khăn tắm",
    "blanket" to "Chăn",
    "sheet" to "Drap giường",
    "soap" to "Xà phòng",
    "shampoo" to "Dầu gội",
    "toothbrush" to "Bàn chải đánh răng",

    // Vehicles & Parts
    "wheel" to "Bánh xe",
    "tire" to "Lốp xe",
    "engine" to "Động cơ",
    "motor" to "Mô tơ",

    // Generic / Common ML Kit labels
    "product" to "Sản phẩm",
    "material" to "Vật liệu",
    "object" to "Vật thể",
    "item" to "Vật phẩm",
    "equipment" to "Thiết bị",
    "appliance" to "Thiết bị gia dụng",
    "machine" to "Máy móc",
    "device" to "Thiết bị",
    "gadget" to "Thiết bị nhỏ",
    "accessory" to "Phụ kiện",
    "part" to "Linh kiện",
    "component" to "Bộ phận",
    "metal" to "Kim loại",
    "wood" to "Gỗ",
    "plastic" to "Nhựa",
    "glass" to "Thủy tinh",
    "textile" to "Vải",
    "leather" to "Da",
    "rubber" to "Cao su",
    "ceramic" to "Gốm sứ",
    "animal" to "Động vật",
    "person" to "Người",
    "indoor" to "Trong nhà",
    "outdoor" to "Ngoài trời",
    "room" to "Phòng",
    "floor" to "Sàn nhà",
    "wall" to "Tường",
    "door" to "Cửa",
    "window" to "Cửa sổ"
)

/**
 * Translate an English ML Kit label to Vietnamese.
 * Falls back to original text if no translation found.
 */
private fun translateLabel(englishLabel: String): String {
    // Try exact match (lowercase)
    labelTranslations[englishLabel.lowercase()]?.let { return it }

    // Try matching each word
    val words = englishLabel.lowercase().split(" ")
    for (word in words) {
        labelTranslations[word]?.let { return it }
    }

    // Fallback: return original with first letter capitalized
    return englishLabel.replaceFirstChar { it.uppercase() }
}

/**
 * ItemPhotoScreen - AI Object Recognition (Vietnamese + Multi-Photo)
 *
 * Users can take MULTIPLE photos to accumulate detected items.
 * Labels are displayed in Vietnamese.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemPhotoScreen(
    onNavigateBack: () -> Unit = {},
    onItemsDetected: (List<String>) -> Unit = {},
    onPhotoCaptured: (List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val maxPhotos = 5

    // State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var isAnalyzing by remember { mutableStateOf(false) }
    // Accumulated labels across all photos
    var allDetectedLabels by remember { mutableStateOf<List<DetectedLabel>>(emptyList()) }
    var showResults by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var photoCount by remember { mutableIntStateOf(0) }
    // Saved photo file paths for upload
    var savedPhotoUris by remember { mutableStateOf<List<String>>(emptyList()) }

    // Camera
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_scan_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                    }
                },
                actions = {
                    if (photoCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text("$photoCount")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { paddingValues ->

        if (!hasCameraPermission) {
            // Permission not granted UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.camera_permission_needed),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        } else if (showResults) {
            // Results view with accumulated items
            ResultsView(
                labels = allDetectedLabels,
                photoCount = photoCount,
                onToggleLabel = { index ->
                    allDetectedLabels = allDetectedLabels.toMutableList().also {
                        it[index] = it[index].copy(isSelected = !it[index].isSelected)
                    }
                },
                onRemoveLabel = { index ->
                    allDetectedLabels = allDetectedLabels.toMutableList().also {
                        it.removeAt(index)
                    }
                },
                onConfirm = {
                    val selectedItems = allDetectedLabels
                        .filter { it.isSelected }
                        .map { it.vietnameseText }
                    onPhotoCaptured(savedPhotoUris)
                    onItemsDetected(selectedItems)
                },
                onTakeMore = {
                    // Go back to camera, keep accumulated labels
                    showResults = false
                    errorMessage = null
                },
                onClearAll = {
                    allDetectedLabels = emptyList()
                    photoCount = 0
                    savedPhotoUris = emptyList()
                    showResults = false
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Camera view
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Camera Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageCapture
                                )
                            } catch (e: Exception) {
                                Log.e("ItemPhotoScreen", "Camera binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay guide
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = if (photoCount == 0)
                                stringResource(R.string.ai_scan_hint)
                            else
                                stringResource(R.string.ai_scan_more_hint, photoCount, allDetectedLabels.size),
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Show accumulated items count badge
                    if (allDetectedLabels.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = stringResource(R.string.ai_items_count, allDetectedLabels.size),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Loading overlay
                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Color.White)
                            Text(
                                text = stringResource(R.string.ai_analyzing),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Error banner
                errorMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 120.dp)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                // Bottom bar: Capture + View Results
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View results button (only if items detected)
                    if (allDetectedLabels.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { showResults = true },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.ai_view_items, allDetectedLabels.size), fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Capture button
                    FloatingActionButton(
                        onClick = {
                            if (!isAnalyzing && savedPhotoUris.size < maxPhotos) {
                                isAnalyzing = true
                                errorMessage = null

                                imageCapture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            // Save photo to local file for later upload
                                            val savedUri = saveImageProxyToFile(context, imageProxy)

                                            analyzeImage(imageProxy) { newLabels, error ->
                                                isAnalyzing = false
                                                if (newLabels != null) {
                                                    photoCount++
                                                    if (savedUri != null) {
                                                        savedPhotoUris = savedPhotoUris + savedUri
                                                    }
                                                    // Merge with existing labels (no duplicates)
                                                    val existingTexts = allDetectedLabels.map { it.text.lowercase() }.toSet()
                                                    val uniqueNew = newLabels.filter {
                                                        it.text.lowercase() !in existingTexts
                                                    }
                                                    allDetectedLabels = allDetectedLabels + uniqueNew
                                                    showResults = true
                                                } else {
                                                    errorMessage = error ?: context.getString(R.string.ai_error_generic)
                                                }
                                                imageProxy.close()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            isAnalyzing = false
                                            errorMessage = context.getString(R.string.ai_capture_error)
                                            Log.e("ItemPhotoScreen", "Capture failed", exception)
                                        }
                                    }
                                )
                            }
                        },
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.take_photo),
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Spacer for symmetry
                    if (allDetectedLabels.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(80.dp))
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                }
            }
        }
    }
}

/**
 * Analyze image using ML Kit Image Labeling with Vietnamese translation.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeImage(
    imageProxy: ImageProxy,
    onResult: (List<DetectedLabel>?, String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        onResult(null, "Không thể đọc ảnh")
        return
    }

    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

    val options = ImageLabelerOptions.Builder()
        .setConfidenceThreshold(0.5f)
        .build()

    val labeler = ImageLabeling.getClient(options)

    labeler.process(inputImage)
        .addOnSuccessListener { labels ->
            val detected = labels.map { label ->
                DetectedLabel(
                    text = label.text,
                    vietnameseText = translateLabel(label.text),
                    confidence = label.confidence
                )
            }.distinctBy { it.text.lowercase() }
                .sortedByDescending { it.confidence }
                .take(10)

            if (detected.isEmpty()) {
                onResult(listOf(DetectedLabel("Package", "Gói hàng", 0.5f)), null)
            } else {
                onResult(detected, null)
            }
        }
        .addOnFailureListener { e ->
            Log.e("ItemPhotoScreen", "ML Kit failed", e)
            onResult(null, e.localizedMessage ?: "Phân tích thất bại")
        }
}

/**
 * Save an ImageProxy to a local JPEG file in the app's cache directory.
 * Returns the file URI as a string, or null on failure.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun saveImageProxyToFile(context: android.content.Context, imageProxy: ImageProxy): String? {
    return try {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        // Decode the image
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        // Apply rotation from CameraX
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees.toFloat()
        val matrix = android.graphics.Matrix()
        if (rotationDegrees != 0f) {
            matrix.postRotate(rotationDegrees)
        }

        val rotated = if (rotationDegrees != 0f) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } else {
            original
        }

        if (rotated !== original) original.recycle()

        // Scale down to max 1024 pixels on the longest side to save memory
        val maxWidth = 1024
        val scaled = if (Math.max(rotated.width, rotated.height) > maxWidth) {
            val ratio = maxWidth.toFloat() / Math.max(rotated.width, rotated.height)
            Bitmap.createScaledBitmap(rotated, (rotated.width * ratio).toInt(), (rotated.height * ratio).toInt(), true)
        } else {
            rotated
        }

        val photoFile = File(context.cacheDir, "delivery_photo_${UUID.randomUUID()}.jpg")
        FileOutputStream(photoFile).use { fos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, fos)
        }

        if (scaled !== rotated) scaled.recycle()
        rotated.recycle()

        Uri.fromFile(photoFile).toString()
    } catch (e: Exception) {
        Log.e("ItemPhotoScreen", "Failed to save photo", e)
        null
    }
}

/**
 * Results view showing accumulated detected labels with multi-photo support.
 */
@Composable
private fun ResultsView(
    labels: List<DetectedLabel>,
    photoCount: Int,
    onToggleLabel: (Int) -> Unit,
    onRemoveLabel: (Int) -> Unit,
    onConfirm: () -> Unit,
    onTakeMore: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = stringResource(R.string.ai_results_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.ai_results_multi_subtitle, photoCount, labels.size),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Detected items list
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(labels.size) { index ->
                val label = labels[index]
                Card(
                    onClick = { onToggleLabel(index) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (label.isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (label.isSelected) Icons.Default.CheckCircle
                                else Icons.Default.ThumbUp,
                                contentDescription = null,
                                tint = if (label.isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = label.vietnameseText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${label.text} · ${(label.confidence * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { onRemoveLabel(index) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.ai_remove_item),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Clear all button
        if (labels.size > 1) {
            TextButton(
                onClick = onClearAll,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ai_clear_all))
            }
        }

        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Take more photos button
            OutlinedButton(
                onClick = onTakeMore,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ai_take_more), fontSize = 13.sp)
            }

            // Confirm button
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                enabled = labels.any { it.isSelected }
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.ai_confirm), fontSize = 13.sp)
            }
        }
    }
}
