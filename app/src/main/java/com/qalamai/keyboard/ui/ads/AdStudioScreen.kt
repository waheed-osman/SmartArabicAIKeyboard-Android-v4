package com.qalamai.keyboard.ui.ads

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdStudioScreen(
    vm: AdStudioViewModel,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String, com.qalamai.keyboard.ai.GeneratedImage?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استوديو الإعلانات بالذكاء الاصطناعي", fontWeight = FontWeight.Bold) },
                actions = { TextButton(onClick = onClose) { Text("إغلاق") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            StepIndicator(vm)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (vm.step) {
                    AdStep.PRODUCT -> ProductStep(vm)
                    AdStep.ANALYSIS -> AnalysisStep(vm)
                    AdStep.IDEAS -> IdeasStep(vm)
                    AdStep.COPY -> CopyStep(vm)
                    AdStep.VISUAL -> VisualStep(vm)
                    AdStep.PACK -> PackStep(vm, onCopy, onShare)
                }

                if (vm.loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("جاري العمل مع Gemini…", modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.cancel() }) { Text("إلغاء") }
                    }
                }
                vm.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }

            BottomBar(vm)
        }
    }
}

@Composable
private fun StepIndicator(vm: AdStudioViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AdStep.entries.forEach { s ->
            val current = s == vm.step
            val done = s.ordinal < vm.step.ordinal
            Surface(
                onClick = { vm.jumpBackTo(s) },
                enabled = done,
                shape = RoundedCornerShape(50),
                color = if (current) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                border = if (done) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Text(
                    text = (if (done) "✓ " else "${s.ordinal + 1}. ") + s.titleAr,
                    color = if (current) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun BottomBar(vm: AdStudioViewModel) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { vm.back() },
                enabled = vm.step != AdStep.PRODUCT && !vm.loading,
                modifier = Modifier.weight(1f)
            ) { Text("رجوع") }

            if (vm.step != AdStep.PACK) {
                Button(
                    onClick = { vm.approveAndNext() },
                    enabled = vm.canApprove() && !vm.loading,
                    modifier = Modifier.weight(2f)
                ) { Text("اعتمد وانتقل للتالي") }
            }
        }
    }
}

// ============================================================================ المراحل

@Composable
private fun ProductStep(vm: AdStudioViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onImagePicked(uri)
    }
    StageTitle("1. المنتج", "ارفع صورة واضحة للمنتج وأخبرني عنه. لا يُرسل شيء إلى الذكاء الاصطناعي في هذه المرحلة.")

    vm.productBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "صورة المنتج",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
        )
    }
    OutlinedButton(
        onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (vm.productBitmap == null) "اختيار صورة المنتج" else "تغيير الصورة") }

    Field("اسم المنتج", vm.productName, singleLine = true) { vm.productName = it }
    Field("ملاحظات عن المنتج (سعر، ميزات، عرض… ما تريد ذكره فقط)", vm.productNotes) { vm.productNotes = it }
    Field("الجمهور المستهدف (اختياري)", vm.audience) { vm.audience = it }
    Field("نبرة الإعلان (مثال: راقية، مرحة، شبابية)", vm.tone) { vm.tone = it }
}

@Composable
private fun AnalysisStep(vm: AdStudioViewModel) {
    StageTitle(
        "2. تحليل المنتج",
        "عند الضغط ستُرسل صورة المنتج (مصغّرة) وبياناتك إلى Gemini. عدّل التحليل كما تشاء قبل الاعتماد."
    )
    GenerateRow(vm, if (vm.analysis.isBlank()) "حلّل المنتج بالذكاء الاصطناعي" else "أعد التحليل") { vm.analyze() }
    EditableResult("التحليل (قابل للتعديل)", vm.analysis) { vm.analysis = it }
    FeedbackField(vm)
}

@Composable
private fun IdeasStep(vm: AdStudioViewModel) {
    StageTitle("3. الفكرة الإعلانية", "اقترح 3 أفكار، اختر واحدة، ثم عدّلها بكلماتك.")
    GenerateRow(vm, if (vm.ideas.isEmpty()) "اقترح 3 أفكار" else "اقترح أفكاراً جديدة") { vm.makeIdeas() }
    FeedbackField(vm)

    vm.ideas.forEachIndexed { i, idea ->
        val selected = vm.selectedIdea == i
        Card(
            onClick = { vm.selectIdea(i) },
            border = BorderStroke(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            colors = CardDefaults.cardColors(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${i + 1}. ${idea.title}", fontWeight = FontWeight.Bold)
                Text(idea.concept, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (vm.selectedIdea >= 0) {
        Field("عنوان الفكرة (عدّله)", vm.ideaTitle, singleLine = true) { vm.ideaTitle = it }
        Field("شرح الفكرة (عدّله)", vm.ideaConcept) { vm.ideaConcept = it }
    }
}

@Composable
private fun CopyStep(vm: AdStudioViewModel) {
    StageTitle("4. نص الإعلان", "يُكتب النص بناءً على فكرتك المعتمدة. عدّل أي كلمة قبل الاعتماد.")
    GenerateRow(vm, if (vm.copyText.isBlank()) "اكتب الإعلان" else "أعد الكتابة") { vm.makeCopy() }
    EditableResult("نص الإعلان (قابل للتعديل)", vm.copyText) { vm.copyText = it }
    FeedbackField(vm)
}

@Composable
private fun VisualStep(vm: AdStudioViewModel) {
    StageTitle(
        "5. التصميم (الصورة الإعلانية)",
        "أولاً يُقترح وصف التصميم لتعدّله، ثم تُولَّد الصورة اعتماداً على صورة منتجك. " +
            "الصورة تُولَّد بلا نص مكتوب داخلها (حروف العربية في صور الذكاء الاصطناعي غير موثوقة)، ونصك يُنشر مع الصورة."
    )
    GenerateRow(vm, if (vm.visualPrompt.isBlank()) "اقترح وصف التصميم" else "أعد اقتراح الوصف") { vm.makeVisualPrompt() }
    EditableResult("وصف التصميم (بالإنجليزية، قابل للتعديل)", vm.visualPrompt) { vm.visualPrompt = it }
    FeedbackField(vm)
    Button(
        onClick = { vm.makeImage() },
        enabled = !vm.loading && vm.visualPrompt.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (vm.adImage == null) "ولّد الصورة الإعلانية" else "أعد توليد الصورة") }

    vm.adBitmap?.let {
        Image(
            bitmap = it,
            contentDescription = "الصورة الإعلانية المولّدة",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "راجع الصورة بعناية: تأكد أن المنتج يطابق الأصل. لا تعتمدها إن اختلف شكله أو شعاره.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun PackStep(
    vm: AdStudioViewModel,
    onCopy: (String) -> Unit,
    onShare: (String, com.qalamai.keyboard.ai.GeneratedImage?) -> Unit
) {
    StageTitle("6. حزمة المحتوى والاعتماد النهائي", "منشورات وسيناريو فيديو وهاشتاغات. راجع كل شيء ثم اعتمد نهائياً لتُفعَّل المشاركة.")
    GenerateRow(vm, if (vm.packText.isBlank()) "جهّز حزمة المحتوى" else "أعد التجهيز") { vm.makePack() }
    EditableResult("حزمة المحتوى (قابلة للتعديل)", vm.packText) { vm.packText = it }
    FeedbackField(vm)

    if (vm.packText.isNotBlank()) {
        Button(
            onClick = { vm.approveFinal() },
            enabled = !vm.finalApproved && !vm.loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (vm.finalApproved) "✓ تم الاعتماد النهائي" else "أوافق على الإعلان كما هو") }
    }

    if (vm.finalApproved) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onCopy(vm.fullAdText()) }, modifier = Modifier.weight(1f)) {
                Text("نسخ النص")
            }
            Button(onClick = { onShare(vm.fullAdText(), vm.adImage) }, modifier = Modifier.weight(1f)) {
                Text("مشاركة الصورة والنص")
            }
        }
        Text(
            "تنبيه: الصورة مولّدة بالذكاء الاصطناعي؛ بعض المنصات تطلب الإفصاح عن ذلك في الإعلانات.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ============================================================================ مكونات مساعدة

@Composable
private fun StageTitle(title: String, hint: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun GenerateRow(vm: AdStudioViewModel, label: String, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !vm.loading, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EditableResult(label: String, value: String, onChange: (String) -> Unit) {
    if (value.isBlank()) return
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        minLines = 4,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FeedbackField(vm: AdStudioViewModel) {
    OutlinedTextField(
        value = vm.feedback,
        onValueChange = { vm.feedback = it },
        label = { Text("ملاحظاتك لإعادة التوليد (اختياري)") },
        placeholder = { Text("مثال: اجعلها أقصر وأكثر فخامة") },
        modifier = Modifier.fillMaxWidth()
    )
}
