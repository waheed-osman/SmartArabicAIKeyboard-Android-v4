package com.qalamai.keyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qalamai.keyboard.ime.SmartKeyboardService
import com.qalamai.keyboard.ui.theme.KeyboardPalette

private val EMOJI_TABS: List<Pair<String, List<String>>> = listOf(
    "وجوه" to ("😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕").split(" "),
    "إيماءات" to ("👍 👎 👌 🤌 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤝 🙏 👏 🙌 👐 🤲 💪 ✍️ 🤳 💅 👀 👁️ 👅 👄").split(" "),
    "قلوب" to ("❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 😻 💑 💏 🌹 🥀 🌷 🌸 💐").split(" "),
    "أشياء" to ("🔥 ✨ ⭐ 🌟 💫 ⚡ 🎉 🎊 🎁 🎈 🏆 🥇 📌 📍 📎 ✂️ 📝 📖 📚 💡 🔔 📣 📢 💰 💳 🛒 📱 💻 ⌚ 📷 🎧 🎵 🚀 ✈️ 🚗 🏠 🌍 🌙 ☀️ ☁️ 🌧️ ❄️ 🕌 🕋 📿 🤲").split(" "),
    "رموز" to ("✅ ❌ ✔️ ✖️ ➕ ➖ ➗ ❓ ❗ ‼️ ⚠️ 🚫 ⭕ 🔴 🟢 🔵 🟡 ⬆️ ⬇️ ⬅️ ➡️ 🔄 ♻️ ™️ ©️ ®️ ﷺ ﷽ ☪️ ✝️ ☮️ ♾️ № ٪ ‰ ° ± × ÷ √ ∞").split(" ")
)

private val STICKERS = listOf(
    "السلام عليكم ورحمة الله وبركاته 🌹", "جمعة مباركة 🕌", "صباح الخير ☀️", "مساء الخير 🌙",
    "كل عام وأنتم بخير 🎉", "رمضان كريم 🌙", "عيد مبارك 🎊", "تقبل الله منا ومنكم 🤲",
    "بارك الله فيك 🌹", "جزاك الله خيراً 🤍", "ما شاء الله 🧿", "إن شاء الله ✨",
    "مبروك 🎉", "الله يسعدك 💛", "تصبح على خير 🌙", "شكراً جزيلاً 🙏",
    "حبيبي ❤️", "وحشتني 🥺", "أنا في الطريق 🚗", "دقيقة وأرجع ⏳",
    "تم ✅", "للأسف ❌", "على راسي 🙌", "ألف مبروك 🥳"
)

@Composable
fun EmojiStickersPanel(service: SmartKeyboardService, palette: KeyboardPalette) {
    // آخر عنصر في التبويبات هو «الملصقات» (نصوص جاهزة)
    var tab by remember { mutableIntStateOf(0) }
    val stickersIndex = EMOJI_TABS.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.toolbarSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (EMOJI_TABS.map { it.first } + "الملصقات").forEachIndexed { i, name ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tab == i) palette.accentColor.copy(alpha = 0.25f) else palette.keySurface)
                        .clickable { tab = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name, color = if (tab == i) palette.accentColor else palette.keyText, fontSize = 12.sp)
                }
            }
        }

        if (tab == stickersIndex) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(STICKERS) { text ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.keySurface)
                            .clickable { service.insertPhrase(text) }
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text, color = palette.keyText, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(40.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                items(EMOJI_TABS[tab].second) { e ->
                    Box(
                        modifier = Modifier
                            .clickable { service.insertPhrase(e) }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 22.sp)
                    }
                }
            }
        }
    }
}
