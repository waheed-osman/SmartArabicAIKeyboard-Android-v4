package com.qalamai.keyboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qalamai.keyboard.data.ClipboardRepository
import com.qalamai.keyboard.data.SecurePrefsManager
import com.qalamai.keyboard.ui.ads.AdStudioActivity
import com.qalamai.keyboard.ui.theme.KeyboardThemeId

class SettingsActivity : ComponentActivity() {

    private lateinit var prefsManager: SecurePrefsManager
    private lateinit var clipboardRepo: ClipboardRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = SecurePrefsManager(this)
        clipboardRepo = ClipboardRepository(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        prefsManager = prefsManager,
                        onClearClipboard = { clipboardRepo.clearAll() },
                        onBugReport = { BugReport.share(this@SettingsActivity) },
                        onOpenAdStudio = {
                            startActivity(Intent(this@SettingsActivity, AdStudioActivity::class.java))
                        },
                        onOpenImeSettings = {
                            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                        },
                        onShowImePicker = {
                            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: SecurePrefsManager,
    onClearClipboard: () -> Unit,
    onOpenAdStudio: () -> Unit,
    onBugReport: () -> Unit,
    onOpenImeSettings: () -> Unit,
    onShowImePicker: () -> Unit
) {
    var apiKey by remember { mutableStateOf(prefsManager.getGeminiApiKey()) }
    var showKey by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(prefsManager.getSelectedTheme()) }
    var hapticEnabled by remember { mutableStateOf(prefsManager.isHapticEnabled()) }
    var autoPcLandscape by remember { mutableStateOf(prefsManager.isAutoPcInLandscape()) }
    var heightScale by remember { mutableStateOf(prefsManager.getHeightScale()) }
    var textScale by remember { mutableStateOf(prefsManager.getTextScale()) }
    var numberRow by remember { mutableStateOf(prefsManager.isNumberRowVisible()) }
    var oneHanded by remember { mutableStateOf(prefsManager.getOneHandedMode()) }
    var clipboardEnabled by remember { mutableStateOf(prefsManager.isClipboardHistoryEnabled()) }
    var clipboardCleared by remember { mutableStateOf(false) }
    var savedBanner by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات لوحة القلم الذكية", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // خطوات تفعيل لوحة المفاتيح
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. تفعيل لوحة المفاتيح في النظام", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onOpenImeSettings, modifier = Modifier.weight(1f)) {
                            Text("تفعيل من الإعدادات")
                        }
                        OutlinedButton(onClick = onShowImePicker, modifier = Modifier.weight(1f)) {
                            Text("اختيار اللوحة")
                        }
                    }
                }
            }

            // استوديو الإعلانات
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("استوديو الإعلانات بالذكاء الاصطناعي", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ارفع صورة منتجك، ودع الذكاء الاصطناعي يقترح الفكرة ويكتب الإعلان ويصمم الصورة ويجهّز المحتوى، " +
                            "وكل مرحلة تحتاج موافقتك قبل الانتقال للتالية. يتطلب مفتاح Gemini أدناه.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = onOpenAdStudio, modifier = Modifier.fillMaxWidth()) {
                        Text("فتح استوديو الإعلانات")
                    }
                }
            }

            // تخزين مفتاح Gemini API المشفر
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. مفتاح Gemini API (مشفر محلياً)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "يُحفظ المفتاح مشفراً على جهازك. عند ضغطك على زر ذكاء اصطناعي يُرسل النص المحدد (أو نص الحقل) إلى خدمة Gemini من Google، " +
                            "ولا يُرسل أي شيء تلقائياً أثناء الكتابة. الأدوات معطلة في حقول كلمات المرور.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; savedBanner = false },
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { showKey = !showKey }) {
                                Text(if (showKey) "إخفاء" else "إظهار")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            prefsManager.saveGeminiApiKey(apiKey)
                            savedBanner = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("حفظ المفتاح بأمان")
                    }
                    if (savedBanner) {
                        Text("تم حفظ المفتاح المشفر بنجاح.", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // التفضيلات والاختصارات
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. التجربة ووضع الحاسوب", style = MaterialTheme.typography.titleMedium)
                    SwitchRow("تفعيل وضع الحاسوب الكامل عند التدوير الأفقي", autoPcLandscape) {
                        autoPcLandscape = it
                        prefsManager.setAutoPcInLandscape(it)
                    }
                    SwitchRow("حفظ سجل الحافظة (مشفر على جهازك)", clipboardEnabled) {
                        clipboardEnabled = it
                        prefsManager.setClipboardHistoryEnabled(it)
                    }
                    OutlinedButton(
                        onClick = {
                            onClearClipboard()
                            clipboardCleared = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("مسح سجل الحافظة")
                    }
                    if (clipboardCleared) {
                        Text("تم مسح سجل الحافظة.", color = MaterialTheme.colorScheme.primary)
                    }
                    SwitchRow("الاهتزاز اللمسي عند الضغط", hapticEnabled) {
                        hapticEnabled = it
                        prefsManager.setHapticEnabled(it)
                    }
                }
            }

            // الحجم والواجهة
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("حجم لوحة المفاتيح والأحرف", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "يمكنك أيضاً تغييرها مباشرة من الكيبورد: زر ⚙ ثم «حجم اللوحة والأحرف».",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("ارتفاع اللوحة: ${(heightScale * 100).toInt()}%")
                    Slider(
                        value = heightScale,
                        onValueChange = { heightScale = it },
                        onValueChangeFinished = { prefsManager.setHeightScale(heightScale) },
                        valueRange = SecurePrefsManager.SCALE_MIN..SecurePrefsManager.SCALE_MAX
                    )
                    Text("حجم الأحرف: ${(textScale * 100).toInt()}%")
                    Slider(
                        value = textScale,
                        onValueChange = { textScale = it },
                        onValueChangeFinished = { prefsManager.setTextScale(textScale) },
                        valueRange = SecurePrefsManager.SCALE_MIN..SecurePrefsManager.SCALE_MAX
                    )
                    SwitchRow("إظهار صف الأرقام العلوي", numberRow) {
                        numberRow = it
                        prefsManager.setNumberRowVisible(it)
                    }
                    Text("طريقة اليد الواحدة")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("OFF" to "إيقاف", "RIGHT" to "اليمنى", "LEFT" to "اليسرى").forEach { (mode, name) ->
                            FilterChip(
                                selected = oneHanded == mode,
                                onClick = {
                                    oneHanded = mode
                                    prefsManager.setOneHandedMode(mode)
                                },
                                label = { Text(name) }
                            )
                        }
                    }
                }
            }

            // الأسئلة الشائعة + تقرير الأخطاء
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الأسئلة الشائعة", style = MaterialTheme.typography.titleMedium)
                    FAQ.forEach { (q, a) ->
                        var open by remember { mutableStateOf(false) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { open = !open }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    (if (open) "▾ " else "◂ ") + q,
                                    modifier = Modifier.fillMaxWidth(),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (open) Text(a, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    OutlinedButton(onClick = onBugReport, modifier = Modifier.fillMaxWidth()) {
                        Text("تقرير الأخطاء")
                    }
                }
            }

            // الثيم
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("4. ثيم لوحة المفاتيح", style = MaterialTheme.typography.titleMedium)
                    KeyboardThemeId.entries.forEach { theme ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedTheme == theme,
                                onClick = {
                                    selectedTheme = theme
                                    prefsManager.saveSelectedTheme(theme)
                                }
                            )
                            Text(theme.titleAr)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val FAQ = listOf(
    "كيف أفعّل لوحة المفاتيح؟" to "اضغط «تفعيل من الإعدادات» وفعّل اللوحة، ثم «اختيار اللوحة» وحدّدها.",
    "من أين أغيّر حجم اللوحة والأحرف؟" to "من الكيبورد: زر ⚙ ثم «حجم لوحة المفاتيح»، أو من هذه الشاشة.",
    "هل يُرسَل ما أكتبه إلى الإنترنت؟" to "لا. يُرسل فقط ما تطلبه أنت بزر ذكاء اصطناعي (أو استوديو الإعلانات) إلى Gemini بمفتاحك. الأدوات معطلة في حقول كلمات المرور.",
    "كيف أحرّك المؤشر؟" to "اسحب إصبعك يميناً ويساراً على زر المسافة، أو افتح لوحة المؤشر من الشريط.",
    "كيف أضيف جملة أو كلمة لقاموسي؟" to "اكتبها في الحقل ثم افتح «جمل» أو «قاموسي» من لوحة الإعدادات واضغط «إضافة».",
    "الصوت لا يعمل" to "فعّل «صوت لوحة المفاتيح» من الإعدادات وتأكد من تفعيل أصوات اللمس في إعدادات صوت هاتفك.",
    "الكتابة بالتمرير (Glide)؟" to "غير متوفرة حالياً، وهي ضمن خطة التطوير القادمة."
)
