package com.shahbuddindairy.app.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shahbuddindairy.app.ai.AiParseResult
import com.shahbuddindairy.app.ai.DairyAiMunshiCommand
import com.shahbuddindairy.app.ai.DairyAiMunshiEngine
import com.shahbuddindairy.app.ai.PrivateAiApiClient
import com.shahbuddindairy.app.data.model.CustomerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Locale

@Composable
fun AiMunshiDialog(
    customers: List<CustomerEntity>,
    onDismiss: () -> Unit,
    onExecuteCommand: (DairyAiMunshiCommand) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val aiClient = remember { PrivateAiApiClient(context) }

    var inputText by remember { mutableStateOf("") }
    var parsedCommand by remember { mutableStateOf<DairyAiMunshiCommand?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var isAiProcessing by remember { mutableStateOf(false) }
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Check private server connectivity on launch
    LaunchedEffect(Unit) {
        isServerOnline = aiClient.checkHealth()
    }

    // Function to trigger parsing (Immediate Local Heuristic + Async Private AI)
    fun processDairyText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // 1. Immediate deterministic local parse (Zero lag)
        val localCmd = DairyAiMunshiEngine.parseInput(trimmed, customers)
        parsedCommand = localCmd
        if (localCmd.matchedCustomer != null) {
            selectedCustomer = localCmd.matchedCustomer
        }

        // 2. If private AI is enabled, enhance asynchronously with Debian 13 LLM
        if (aiClient.isAiEnabled) {
            coroutineScope.launch {
                isAiProcessing = true
                val result = aiClient.parseDairyInstruction(trimmed, customers)
                isAiProcessing = false
                when (result) {
                    is AiParseResult.Success -> {
                        isServerOnline = true
                        parsedCommand = result.command
                        if (result.command.matchedCustomer != null) {
                            selectedCustomer = result.command.matchedCustomer
                        }
                    }
                    is AiParseResult.Fallback -> {
                        if (result.reason.contains("ऑफ़लाइन") || result.reason.contains("HTTP")) {
                            isServerOnline = false
                        }
                    }
                }
            }
        }
    }

    // Speech Recognizer Launcher for voice
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
                processDairyText(spoken)
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "वॉइस इनपुट उपलब्ध नहीं है। कृपया लिखकर दर्ज करें।", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलें: जैसे 'सलमान 4 लीटर 70 दर'")
        }
        try {
            isListening = true
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(context, "माइक शुरू नहीं हो सका: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Photo / Receipt / Handwritten Slip Image Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAiProcessing = true
                try {
                    val base64Jpeg = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bitmap = BitmapFactory.decodeStream(stream)
                            // Scale down to max 800px to conserve memory and transfer
                            val scaled = if (bitmap.width > 800 || bitmap.height > 800) {
                                val ratio = 800f / kotlin.math.max(bitmap.width, bitmap.height)
                                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                            } else bitmap
                            val baos = ByteArrayOutputStream()
                            scaled.compress(Bitmap.CompressFormat.JPEG, 75, baos)
                            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                        }
                    }
                    if (base64Jpeg != null) {
                        val result = aiClient.parseReceiptImage(base64Jpeg, customers)
                        when (result) {
                            is AiParseResult.Success -> {
                                isServerOnline = true
                                parsedCommand = result.command
                                if (result.command.matchedCustomer != null) {
                                    selectedCustomer = result.command.matchedCustomer
                                }
                            }
                            is AiParseResult.Fallback -> {
                                Toast.makeText(context, "पर्ची स्कैन: ${result.reason}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "फ़ोटो लोड नहीं हो सकी: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isAiProcessing = false
                }
            }
        }
    }

    // Filter customers for manual picker if needed
    val filteredCustomers = remember(inputText, selectedCustomer) {
        if (selectedCustomer != null || inputText.isBlank()) {
            emptyList()
        } else {
            customers.filter {
                it.name.contains(inputText, ignoreCase = true) ||
                it.mohalla.contains(inputText, ignoreCase = true)
            }.take(5)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Header with AI Server Status & Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF15803D),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AI मुंशी",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Server Status Indicator Pill
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isServerOnline == true) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isServerOnline == true) Color(0xFF86EFAC) else Color(0xFFCBD5E1)),
                                    modifier = Modifier.clickable { showSettingsDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isServerOnline == true) Color(0xFF16A34A) else Color(0xFF94A3B8))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isServerOnline == true) "Debian 13 AI" else "लोकल AI",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isServerOnline == true) Color(0xFF15803D) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "दूध, नागा या नकद हिसाब बोलें/लिखें",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "AI Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Input Box with Mic and Receipt Scanner
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        if (it.isNotBlank()) {
                            val local = DairyAiMunshiEngine.parseInput(it, customers)
                            parsedCommand = local
                            if (local.matchedCustomer != null) {
                                selectedCustomer = local.matchedCustomer
                            }
                        } else {
                            parsedCommand = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "जैसे: सलमान 4 लीटर 70 दर या नागा",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // Camera / Slip Scanner Icon
                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Scan Slip",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Big Mic Button
                            Surface(
                                shape = CircleShape,
                                color = if (isListening) Color(0xFFDC2626) else Color(0xFF15803D),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable { startListening() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Mic",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        if (inputText.isNotBlank()) {
                            processDairyText(inputText)
                        }
                    }),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // AI Processing Spinner Indicator
                AnimatedVisibility(visible = isAiProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF15803D)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI मुंशी समझ रहे हैं...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF15803D)
                        )
                    }
                }

                // 3. Customer Suggestions dropdown if no exact match
                if (filteredCustomers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "ग्राहक चुनें:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            filteredCustomers.forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCustomer = c
                                            val currentCmd = parsedCommand ?: DairyAiMunshiEngine.parseInput(inputText, customers)
                                            parsedCommand = currentCmd.copy(
                                                matchedCustomer = c,
                                                explanationHindi = "ग्राहक: ${c.name} (${c.mohalla})\nदूध: ${currentCmd.liters ?: c.defaultLiters}L @ ₹${c.ratePerLiter}/L"
                                            )
                                        }
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("₹${c.ratePerLiter}/L • ${c.defaultLiters}L", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Quick Action Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickPills = listOf("आज नागा है", "1 लीटर", "2 लीटर", "100 जमा")
                    quickPills.forEach { pill ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.clickable {
                                val currentText = if (selectedCustomer != null) "${selectedCustomer?.name} $pill" else pill
                                inputText = currentText
                                processDairyText(currentText)
                            }
                        ) {
                            Text(
                                text = pill,
                                fontSize = 10.5.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Active Command Reviewable Draft
                val activeCustomer = selectedCustomer ?: parsedCommand?.matchedCustomer
                val cmd = parsedCommand

                if (activeCustomer != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ग्राहक: ${activeCustomer.name}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "दर: ₹${activeCustomer.ratePerLiter}/L",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val isNaga = cmd?.isNaga == true
                            val liters = if (isNaga) 0.0 else (cmd?.liters ?: activeCustomer.defaultLiters)
                            val cash = cmd?.cash ?: 0.0
                            val bill = (liters * activeCustomer.ratePerLiter)

                            Text(
                                text = if (isNaga) "आज नागा (0L)" else "दूध: $liters L • बिल: ₹%.0f".format(bill),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isNaga) Color(0xFFDC2626) else Color(0xFF1F2937)
                            )
                            if (cash > 0) {
                                Text(
                                    text = "नकद जमा: ₹%.0f".format(cash),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                            if (cmd?.explanationHindi?.isNotBlank() == true) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cmd.explanationHindi,
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF15803D).copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else if (inputText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 कृपया ग्राहक का नाम बोलें या ऊपर सूची में से चुनें",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Action Buttons: Dismiss & Explicit Save Confirmation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("रद्द करें")
                    }

                    Button(
                        onClick = {
                            val finalCust = selectedCustomer ?: parsedCommand?.matchedCustomer
                            if (finalCust == null) {
                                Toast.makeText(context, "कृपया ग्राहक चुनें", Toast.LENGTH_SHORT).show()
                            } else {
                                val currentCmd = parsedCommand ?: DairyAiMunshiEngine.parseInput(inputText, customers)
                                val finalCmd = currentCmd.copy(matchedCustomer = finalCust)
                                onExecuteCommand(finalCmd)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        enabled = (selectedCustomer != null || parsedCommand?.matchedCustomer != null),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("सेव करें (Save)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 7. Private AI Settings & Diagnostics Dialog
    if (showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(aiClient.serverUrl) }
        var tempEnabled by remember { mutableStateOf(aiClient.isAiEnabled) }
        var isTestingConnection by remember { mutableStateOf(false) }
        var testResult by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("प्राइवेट AI सर्वर सेटिंग्स", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Debian 13 AI सर्वर", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = tempEnabled,
                            onCheckedChange = { tempEnabled = it }
                        )
                    }

                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Tailscale HTTPS URL", fontSize = 11.sp) },
                        placeholder = { Text("https://fahad.tail2abf56.ts.net") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isTestingConnection = true
                                aiClient.serverUrl = tempUrl
                                aiClient.isAiEnabled = tempEnabled
                                val ok = aiClient.checkHealth()
                                isTestingConnection = false
                                isServerOnline = ok
                                testResult = if (ok) "✅ कनेक्टेड! MiniCPM-V-4.7-1B सक्रिय है।" else "❌ कनेक्ट नहीं हो सका (Tailscale चेक करें)"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("कनेक्शन टेस्ट करें (Test)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (testResult != null) {
                        Text(
                            text = testResult ?: "",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (testResult?.contains("✅") == true) Color(0xFF15803D) else Color(0xFFDC2626)
                        )
                    }

                    Text(
                        text = "🔒 सुरक्षा: AI सर्वर आपके निजी Tailscale नेटवर्क पर चलता है। अगर सर्वर बंद हो तो ऐप 100% ऑफ़लाइन काम करता रहेगा।",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        aiClient.serverUrl = tempUrl
                        aiClient.isAiEnabled = tempEnabled
                        showSettingsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    Text("सेव करें", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("बंद करें")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
