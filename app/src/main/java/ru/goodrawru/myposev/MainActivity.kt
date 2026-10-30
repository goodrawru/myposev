package ru.goodrawru.myposev

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.text.TextWatcher
import android.text.Editable
import org.json.JSONArray
import org.json.JSONObject
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import com.caverock.androidsvg.SVG
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.GridLayout
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.ComponentName
import android.net.Uri
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.SeekBar
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

data class Plant(val name:String, val group:String, val sowing:String, val depth:String, val sprouts:String, val advice:String)
data class GardenCity(val city:String, val region:String, val zone:String, val latitude:Double, val longitude:Double)


class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var selectedDate = Calendar.getInstance()
    private var musicController: MediaController? = null
    private var musicControllerFuture: ListenableFuture<MediaController>? = null
    private var musicStatusView: TextView? = null
    private var musicProgress: SeekBar? = null
    private var userSeekingMusic = false
    private var weatherPageOpen = false
    private var weatherForecastOpen = false
    private var pendingPdfBytes: ByteArray? = null
    private var pendingBackupBytes: ByteArray? = null
    private var weatherLatitude: Double? = null
    private var weatherLongitude: Double? = null
    private val musicHandler = Handler(Looper.getMainLooper())
    private val musicProgressTask = object : Runnable {
        override fun run() {
            val player = musicController
            val bar = musicProgress
            if (player != null && bar != null && !userSeekingMusic) {
                val duration = player.duration
                if (duration > 0 && duration != androidx.media3.common.C.TIME_UNSET) {
                    bar.progress = ((player.currentPosition.toDouble() / duration) * 1000).toInt().coerceIn(0, 1000)
                } else bar.progress = 0
            }
            musicHandler.postDelayed(this, 1000)
        }
    }
    private val musicListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { updateMusicStatus() }
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { updateMusicStatus() }
        override fun onIsPlayingChanged(isPlaying: Boolean) { updateMusicStatus() }
        override fun onPlaybackStateChanged(playbackState: Int) { updateMusicStatus() }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Toast.makeText(this@MainActivity, "Не удалось воспроизвести аудио: ${error.errorCodeName}", Toast.LENGTH_LONG).show()
            updateMusicStatus()
        }
    }
    private val green = Color.rgb(39, 91, 57)
    private val pale = Color.rgb(239, 246, 235)
    private val cities = listOf(
        GardenCity("Москва","Москва","Центральный",55.7558,37.6173),
        GardenCity("Санкт-Петербург","Ленинградская область","Северо-Запад",59.9343,30.3351),
        GardenCity("Калининград","Калининградская область","Северо-Запад",54.7104,20.4522),
        GardenCity("Псков","Псковская область","Северо-Запад",57.8194,28.3318),
        GardenCity("Воронеж","Воронежская область","Центральный",51.6755,39.2089),
        GardenCity("Белгород","Белгородская область","Юг",50.5997,36.5983),
        GardenCity("Краснодар","Краснодарский край","Юг",45.0355,38.9753),
        GardenCity("Симферополь","Крым","Юг",44.9521,34.1024),
        GardenCity("Севастополь","Севастополь","Юг",44.6167,33.5254),
        GardenCity("Ростов-на-Дону","Ростовская область","Юг",47.2357,39.7015),
        GardenCity("Ставрополь","Ставропольский край","Юг",45.0445,41.9690),
        GardenCity("Волгоград","Волгоградская область","Поволжье",48.7080,44.5133),
        GardenCity("Самара","Самарская область","Поволжье",53.1959,50.1002),
        GardenCity("Казань","Республика Татарстан","Поволжье",55.7961,49.1064),
        GardenCity("Нижний Новгород","Нижегородская область","Центральный",56.2965,43.9361),
        GardenCity("Екатеринбург","Свердловская область","Урал",56.8389,60.6057),
        GardenCity("Пермь","Пермский край","Урал",58.0105,56.2502),
        GardenCity("Челябинск","Челябинская область","Урал",55.1644,61.4368),
        GardenCity("Тюмень","Тюменская область","Сибирь",57.1522,65.5272),
        GardenCity("Новосибирск","Новосибирская область","Сибирь",55.0084,82.9357),
        GardenCity("Омск","Омская область","Сибирь",54.9885,73.3242),
        GardenCity("Красноярск","Красноярский край","Сибирь",56.0153,92.8932),
        GardenCity("Иркутск","Иркутская область","Сибирь",52.2864,104.2807),
        GardenCity("Якутск","Республика Саха (Якутия)","Север",62.0355,129.6755),
        GardenCity("Хабаровск","Хабаровский край","Дальний Восток",48.4802,135.0719),
        GardenCity("Владивосток","Приморский край","Дальний Восток",43.1155,131.8855),
        GardenCity("Южно-Сахалинск","Сахалинская область","Дальний Восток",46.9591,142.7380),
        GardenCity("Мурманск","Мурманская область","Север",68.9585,33.0827),
        GardenCity("Архангельск","Архангельская область","Север",64.5399,40.5158),
        GardenCity("Минск","Беларусь","Центральный",53.9006,27.5590),
        GardenCity("Алматы","Казахстан","Юг",43.2383,76.9455)
    )
    private val plants = listOf(
        Plant("Томат","Овощи","Обычно за 50–65 дней до высадки","0,5–1 см","5–10 дней","Сейте в рыхлый грунт. После всходов обеспечьте хорошее освещение; сроки зависят от сорта и региона."),
        Plant("Перец сладкий","Овощи","Часто за 60–75 дней до высадки","0,5–1 см","7–14 дней","Перец любит тепло. Не высаживайте в холодную почву; учитывайте местную погоду."),
        Plant("Огурец","Овощи","За 20–30 дней до высадки или в прогретый грунт","2–3 см","3–7 дней","Не любит повреждения корней. Рассаду сейте в отдельные ёмкости."),
        Plant("Морковь","Корнеплоды","Весной после подготовки почвы","1–2 см","10–20 дней","Нужны рыхлая почва и равномерная влажность. Не вносите свежий навоз перед посевом."),
        Plant("Редис","Корнеплоды","Весной и в конце лета","1–2 см","3–7 дней","В жару может стрелковаться. Поддерживайте равномерный полив."),
        Plant("Укроп","Зелень","С ранней весны, повторными посевами","1–2 см","7–14 дней","Для непрерывного сбора сейте небольшими партиями."),
        Plant("Салат листовой","Зелень","Весной и в прохладные периоды","0,5–1 см","4–10 дней","Мелкие семена не заглубляйте; почва должна оставаться влажной."),
        Plant("Капуста белокочанная","Капустные","По сроку созревания и планируемой высадке","0,5–1 см","4–10 дней","Рассаде нужны свет и проветривание. Учитывайте сорт и риск заморозков."),
        Plant("Клубника","Ягоды","Обычно весной или в конце лета","Корни расправить; сердечко не заглублять","Зависит от способа","Выберите солнечное место и не засыпайте точку роста."),
        Plant("Бархатцы","Цветы","Весной на рассаду или после заморозков","0,5–1 см","5–10 дней","Теплолюбивы; высаживайте после угрозы заморозков."),
        Plant("Тыква","Овощи","После прогрева почвы или за 20–30 дней до высадки","3–5 см","5–10 дней","Нужны тепло, место для плетей и регулярный полив в период роста."),
        Plant("Кабачок","Овощи","После угрозы заморозков или за 20–25 дней до высадки","3–4 см","4–8 дней","Не загущайте посадки; собирайте молодые плоды регулярно."),
        Plant("Свёкла","Корнеплоды","Весной в прогретую почву; возможен летний посев","2–3 см","7–14 дней","Прореживайте всходы и поддерживайте равномерную влажность."),
        Plant("Лук репчатый","Луковые","Севок весной после подсыхания почвы","3–5 см для севка","7–14 дней","Не переувлажняйте грядку; прекращайте полив перед уборкой по погоде."),
        Plant("Чеснок","Луковые","Озимый осенью или яровой весной","3–6 см","Зависит от температуры","Выбирайте здоровые зубки и хорошо дренированное место."),
        Plant("Горох","Бобовые","Ранней весной, когда почва готова к обработке","3–5 см","7–14 дней","Предусмотрите опору для высокорослых сортов."),
        Plant("Фасоль","Бобовые","После прогрева почвы и окончания заморозков","3–5 см","7–14 дней","Теплолюбива; не сейте в холодную мокрую землю."),
        Plant("Базилик","Зелень","На рассаду в тепле или после заморозков","0,5–1 см","5–10 дней","Нужны тепло, свет и защита от холодных ночей."),
        Plant("Петрушка","Зелень","Ранней весной; возможен подзимний посев","1–2 см","14–25 дней","Всходит медленно; поддерживайте влажность до появления всходов."),
        Plant("Малина","Ягоды","Весной или осенью в подходящем регионе","Корневая шейка на уровне почвы","Зависит от посадочного материала","Подберите сорт и место с учётом зимостойкости и обеспечьте опору при необходимости."),
        Plant("Смородина","Ягоды","Весной или осенью в период покоя","Корни расправить; не пересушивать","Зависит от посадочного материала","Выберите сорт под климат и регулярно обновляйте обрезкой куст."),
        Plant("Яблоня","Плодовые деревья","Весной или осенью, когда нет экстремальных условий","Корневая шейка на уровне почвы","Не применимо","Подберите подвой и сорт для региона; не заглубляйте место прививки."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        scheduleDailyReminder()
        connectMusicController()
        showHome()
        showFirstRunOrUpdateNotice()
    }

    private fun baseLayout(title:String) {
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            background = getDrawable(R.drawable.bg_garden)
            // Respect Android 15 edge-to-edge status-bar inset.
            setOnApplyWindowInsetsListener { view, insets ->
                val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
                val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val navBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
                    val imeBottom = insets.getInsets(WindowInsets.Type.ime()).bottom
                    maxOf(navBottom, imeBottom)
                } else insets.systemWindowInsetBottom
                view.setPadding(0, topInset, 0, bottomInset)
                insets
            }
        }
        root.addView(TextView(this).apply {
            text = "🌱  " + title; textSize=25f; setTextColor(Color.WHITE); setBackgroundColor(green)
            setPadding(20,22,20,22); gravity=Gravity.CENTER_VERTICAL
        })
        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(Color.WHITE)
        }
        sectionRow.addView(TextView(this).apply {
            text = "Раздел: $title"
            textSize = 12f
            setTextColor(Color.rgb(65, 78, 67))
            maxLines = 2
        }, LinearLayout.LayoutParams(0, -2, 1f))
        sectionRow.addView(Button(this).apply {
            text = "О программе"; textSize = 11f; isAllCaps = false
            minWidth = dp(0); minHeight = dp(36); setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { showAbout() }
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        root.addView(sectionRow)
        val navScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setBackgroundColor(Color.WHITE) }
        val nav = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE) }
        listOf("Сегодня","Календарь","Культуры","Задачи","Огород").forEach { label ->
            nav.addView(Button(this).apply {
                text=label; textSize=10f; isAllCaps=false; minWidth=dp(68); minHeight=dp(44); setPadding(dp(5),dp(5),dp(5),dp(5))
                setOnClickListener { when(label) {
                    "Сегодня" -> showHome()
                    "Календарь" -> showCalendar()
                    "Культуры" -> showPlants("")
                    "Задачи" -> showTasks()
                    "Огород" -> showDiary()
                    else -> showAbout()
                } }
            }, LinearLayout.LayoutParams(-2,-2))
        }
        navScroll.addView(nav)
        root.addView(navScroll)
        val scroll=ScrollView(this).apply {
            isFillViewport = true
            isSmoothScrollingEnabled = true
            descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,28) }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        // When the keyboard opens, keep the focused text field visible in the scroll area.
        root.viewTreeObserver.addOnGlobalLayoutListener {
            val focused = currentFocus
            if (focused is EditText && focused.hasFocus()) {
                scroll.post { scroll.requestChildFocus(focused, focused) }
            }
        }
    }

    private fun showPaperPlanner() {
        baseLayout("Бумажный вариант")
        card("📘 Бумажный вариант огородного дневника", "Выберите разделы и период. PDF будет многостраничным, с переносом длинного текста и календарём по месяцам.")
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearInput = EditText(this).apply { hint = "Год"; setText(currentYear.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true) }
        content.addView(txt("Год календаря", 16f, true)); content.addView(yearInput)
        val choices = listOf("calendar" to "Посевной календарь", "work" to "Сезонные рекомендации и работы", "tasks" to "Мои задачи и статус выполнения", "plants" to "Каталог культур", "notes" to "Мои заметки", "blank" to "Страницы для записей")
        val checks = mutableMapOf<String, CheckBox>()
        choices.forEachIndexed { index, item -> val cb = CheckBox(this).apply { text = item.second; isChecked = index < 4; setTextColor(Color.rgb(42,55,43)) }; checks[item.first] = cb; content.addView(cb) }
        val period = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Весь год", "Весна (март–май)", "Лето (июнь–август)", "Осень (сентябрь–ноябрь)", "Зима (декабрь–февраль)")) }
        content.addView(txt("Период", 16f, true)); content.addView(period)
        val colorMode = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Цветной", "Чёрно-белый, экономный")) }
        content.addView(txt("Оформление", 16f, true)); content.addView(colorMode)
        button("Сформировать и сохранить PDF") {
            val year = yearInput.text.toString().toIntOrNull()
            if (year == null || year !in 2000..2100) { Toast.makeText(this, "Введите год от 2000 до 2100", Toast.LENGTH_LONG).show(); return@button }
            val selected = checks.filterValues { it.isChecked }.keys.toSet()
            if (selected.isEmpty()) { Toast.makeText(this, "Выберите хотя бы один раздел", Toast.LENGTH_LONG).show(); return@button }
            try {
                pendingPdfBytes = buildPaperPdf(year, period.selectedItemPosition, selected, colorMode.selectedItemPosition == 0)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"; putExtra(Intent.EXTRA_TITLE, "Мой_Посевной_" + year + ".pdf") }
                startActivityForResult(intent, 2027)
            } catch (e: Exception) { pendingPdfBytes = null; Toast.makeText(this, "Ошибка подготовки PDF: " + (e.message ?: "неизвестная ошибка"), Toast.LENGTH_LONG).show() }
        }
        card("Что попадёт в документ", "Календарь с датами и фазами Луны, рекомендации по месяцам, задачи с культурой/грядкой и статусом, каталог культур и выбранные заметки. Длинные разделы переносятся на следующие страницы.")
    }

    private fun buildPaperPdf(year: Int, periodIndex: Int, selected: Set<String>, colorMode: Boolean): ByteArray {
        val doc = PdfDocument(); val width = 595; val height = 842; val left = 42f; val right = 553f; val bottom = 795f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 19f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = if (colorMode) android.graphics.Color.rgb(39,91,57) else android.graphics.Color.BLACK }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = if (colorMode) android.graphics.Color.rgb(39,91,57) else android.graphics.Color.BLACK }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = android.graphics.Color.DKGRAY }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = android.graphics.Color.GRAY }
        var page: PdfDocument.Page? = null; var canvas: android.graphics.Canvas? = null; var y = 0f; var pageNo = 0
        fun newPage(title: String? = null) {
            if (page != null) { canvas!!.drawText("Мой Посевной • " + year + " • стр. " + pageNo, left, 822f, footerPaint); doc.finishPage(page!!) }
            pageNo++; page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create()); canvas = page!!.canvas; canvas!!.drawColor(android.graphics.Color.WHITE); y = 48f
            if (title != null) { canvas!!.drawText(title, left, y, titlePaint); y += 28f; canvas!!.drawLine(left, y, right, y, headPaint); y += 20f }
        }
        fun ensure(h: Float = 14f) { if (y + h > bottom) newPage() }
        fun line(value: String, paint: Paint = textPaint, indent: Float = 0f) {
            val maxWidth = right - left - indent; var row = ""
            for (word in value.split(Regex("\\s+"))) { val candidate = if (row.isEmpty()) word else row + " " + word; if (paint.measureText(candidate) > maxWidth && row.isNotEmpty()) { ensure(paint.textSize + 5f); canvas!!.drawText(row, left + indent, y, paint); y += paint.textSize + 4f; row = word } else row = candidate }
            if (row.isNotEmpty()) { ensure(paint.textSize + 5f); canvas!!.drawText(row, left + indent, y, paint); y += paint.textSize + 4f }
        }
        fun section(value: String) { ensure(25f); y += 5f; line(value, headPaint); y += 3f }
        val months = when (periodIndex) { 1 -> listOf(2,3,4); 2 -> listOf(5,6,7); 3 -> listOf(8,9,10); 4 -> listOf(11,0,1); else -> (0..11).toList() }
        newPage("Мой Посевной — бумажный вариант"); line("План огородника на " + year, headPaint); line("Сформировано: " + SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())); line("Период: " + listOf("весь год","весна","лето","осень","зима")[periodIndex]); line("Лунные обозначения — справочная подсказка. Учитывайте регион, погоду и температуру почвы."); y += 12f
        section("Содержание"); if ("calendar" in selected) line("Посевной календарь"); if ("work" in selected) line("Сезонные рекомендации"); if ("tasks" in selected) line("Мои задачи"); if ("plants" in selected) line("Каталог культур"); if ("notes" in selected) line("Мои заметки"); if ("blank" in selected) line("Страницы для записей")
        if ("calendar" in selected) months.forEach { month ->
            val actualYear = if (periodIndex == 4 && month == 11) year - 1 else year
            val c = Calendar.getInstance().apply { set(actualYear, month, 1, 12, 0, 0) }
            newPage("Календарь — " + SimpleDateFormat("LLLL yyyy", Locale("ru")).format(c.time))
            line("Пн       Вт       Ср       Чт       Пт       Сб       Вс", headPaint)
            val offset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7; val days = c.getActualMaximum(Calendar.DAY_OF_MONTH); val cells = MutableList(offset) { 0 } + (1..days).toList()
            cells.chunked(7).forEach { row -> val labels = (row + List(7-row.size) { 0 }).map { day -> if (day == 0) "       " else day.toString().padStart(2) + " " + lunarGlyph(Calendar.getInstance().apply { set(actualYear, month, day, 12, 0, 0) }) }; line(labels.joinToString("   ")) }
            y += 5f; line("Фазы Луны даны как ориентир; сроки работ корректируйте по местным условиям.")
            val monthNotes = prefs.all.toList().filter { entry ->
                val noteKey = entry.first
                val noteValue = entry.second
                noteKey.startsWith("note_") && noteValue is String && noteValue.isNotBlank() &&
                runCatching {
                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(noteKey.removePrefix("note_"))
                    d != null && Calendar.getInstance().apply { time = d }.get(Calendar.YEAR) == actualYear &&
                        Calendar.getInstance().apply { time = d }.get(Calendar.MONTH) == month
                }.getOrDefault(false)
            }.sortedBy { entry -> entry.first }
            if (monthNotes.isNotEmpty()) {
                section("Заметки к календарю")
                monthNotes.forEach { entry ->
                    val rawDate = entry.first.removePrefix("note_")
                    val noteValue = entry.second as String
                    val displayDate = runCatching { SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate)!!) }.getOrDefault(rawDate)
                    line("$displayDate — $noteValue")
                }
            }
        }
        if ("work" in selected) { newPage("Сезонные рекомендации"); months.forEach { month -> val actualYear = if (periodIndex == 4 && month == 11) year - 1 else year; val c = Calendar.getInstance().apply { set(actualYear, month, 15, 12, 0, 0) }; section(SimpleDateFormat("LLLL", Locale("ru")).format(c.time)); line(seasonalAdvice(c)) } }
        if ("tasks" in selected) {
            newPage("Мои задачи"); val tasks = tasksArray(); var count = 0
            for (i in 0 until tasks.length()) { val item = tasks.optJSONObject(i) ?: continue; val date = item.optString("date", ""); if (date.length < 7 || date.substring(0,4).toIntOrNull() != year || (date.substring(5,7).toIntOrNull()?.minus(1) !in months)) continue; count++; section(date + " • " + item.optString("title", "Задача")); line("Тип работы: " + item.optString("type", "Уход")); val crop = item.optString("crop", "").trim(); if (crop.isNotEmpty()) line("Культура/грядка: " + crop); line("Статус: " + if (item.optBoolean("done", false)) "Выполнено" else "Запланировано") }
            if (count == 0) line("На выбранный период задач нет.")
        }
        if ("plants" in selected) { newPage("Каталог культур"); plants.forEach { p -> section(p.name); line("Группа: " + p.group); line("Срок посева: " + p.sowing); line("Глубина: " + p.depth); line("Всходы: " + p.sprouts); line(p.advice) } }
        if ("notes" in selected) { newPage("Мои заметки"); val notes = prefs.all.filter { it.key.startsWith("note_") && it.value is String && (it.value as String).isNotBlank() }.toSortedMap(); if (notes.isEmpty()) line("Сохранённых заметок нет."); notes.forEach { entry -> section(entry.key.removePrefix("note_")); line(entry.value.toString()) } }
        if ("blank" in selected) repeat(3) { index -> newPage("Свободные записи " + (index + 1)); repeat(22) { canvas!!.drawLine(left, y, right, y, footerPaint); y += 30f } }
        if (page != null) { canvas!!.drawText("Мой Посевной • " + year + " • стр. " + pageNo, left, 822f, footerPaint); doc.finishPage(page!!) }
        val out = java.io.ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray()
    }
    // RuStore build: no third-party radio streams. Users can play audio files stored on their device.
    private fun showImmersionPlayer() {
        baseLayout("Аудиоплеер")
        content.setPadding(dp(14), dp(14), dp(14), dp(28))
        card("🎧 Локальный аудиоплеер",
            "Воспроизводите собственные аудиофайлы с телефона. Выбранные файлы не загружаются на сервер. Радиопотоки сторонних сервисов не входят в версию для RuStore.")
        val nowCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(Color.WHITE, 18f, Color.rgb(220, 230, 218))
        }
        nowCard.addView(TextView(this).apply {
            text = "СЕЙЧАС ИГРАЕТ"; textSize = 11f; letterSpacing = 0.08f
            setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.rgb(75, 112, 79))
        })
        val nowPlaying = TextView(this).apply {
            text = "Выберите аудиофайлы с телефона"; textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD); setTextColor(Color.rgb(35, 48, 38))
            setPadding(0, dp(5), 0, dp(3))
        }
        musicStatusView = nowPlaying
        nowCard.addView(nowPlaying)
        nowCard.addView(TextView(this).apply {
            text = "Воспроизведение может продолжаться в фоне"; textSize = 12f
            setTextColor(Color.rgb(105, 115, 103))
        })
        content.addView(nowCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        content.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            addView(playerControlButton("⏮", "Предыдущий файл") { musicController?.seekToPreviousMediaItem() })
            addView(playerControlButton("▶ / ⏸", "Воспроизвести или приостановить") {
                val player = musicController
                if (player == null) Toast.makeText(this@MainActivity, "Плеер ещё подключается", Toast.LENGTH_SHORT).show()
                else if (player.isPlaying) player.pause()
                else if (player.mediaItemCount == 0) chooseLocalAudio()
                else player.play()
            })
            addView(playerControlButton("⏭", "Следующий файл") { musicController?.seekToNextMediaItem() })
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        button("＋ Выбрать аудиофайлы") { chooseLocalAudio() }
        button("Очистить очередь") { musicController?.clearMediaItems(); updateMusicStatus() }
        button("← На главный экран") { showHome() }
        updateMusicStatus()
        musicHandler.removeCallbacks(musicProgressTask)
        musicHandler.post(musicProgressTask)
    }

    private fun playerControlButton(label: String, description: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            contentDescription = description
            isAllCaps = false
            minWidth = 0
            minHeight = dp(48)
            setPadding(dp(12), dp(5), dp(12), dp(5))
            setOnClickListener { action() }
        }

    private fun sectionHeading(title: String, subtitle: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(10), dp(2), dp(8))
            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 20f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.rgb(35, 65, 42))
            })
            addView(TextView(this@MainActivity).apply {
                text = subtitle
                textSize = 13f
                setTextColor(Color.rgb(103, 112, 101))
                setPadding(0, dp(3), 0, 0)
            })
        }

    private fun connectMusicController() {
        val token = SessionToken(this, ComponentName(this, MusicPlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        musicControllerFuture = future
        future.addListener({
            try {
                runOnUiThread {
                    try {
                        musicController = future.get()
                        musicController?.addListener(musicListener)
                        updateMusicStatus()
                    } catch (_: Exception) {
                        musicStatusView?.text = "Плеер запускается. Откройте раздел ещё раз."
                    }
                }
            } catch (_: Exception) { }
        }, java.util.concurrent.Executor { it.run() })
    }

    private fun chooseLocalAudio() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, 2301) }
        catch (_: Exception) { Toast.makeText(this, "Не удалось открыть выбор аудиофайлов", Toast.LENGTH_LONG).show() }
    }

    @Deprecated("Uses the document picker to support older Android versions")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2401) {
            val bytes = pendingBackupBytes; pendingBackupBytes = null
            if (resultCode != Activity.RESULT_OK || data?.data == null || bytes == null) return
            try { contentResolver.openOutputStream(data.data!!)?.use { it.write(bytes) }; Toast.makeText(this, "Резервная копия сохранена", Toast.LENGTH_LONG).show() }
            catch (e: Exception) { Toast.makeText(this, "Ошибка сохранения копии", Toast.LENGTH_LONG).show() }
            return
        }
        if (requestCode == 2402) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Заменить данные приложения?")
                    .setMessage("Восстановление заменит текущие настройки, задачи, заметки и каталог культур данными из резервной копии.")
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Восстановить") { _, _ -> importBackup(data.data!!) }
                    .show()
            }
            return
        }
        if (requestCode == 2027) {
            val bytes = pendingPdfBytes
            pendingPdfBytes = null
            if (resultCode != Activity.RESULT_OK || data?.data == null || bytes == null) return
            try {
                contentResolver.openOutputStream(data.data!!)?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Не удалось открыть файл")
                Toast.makeText(this, "PDF сохранён", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка сохранения: " + (e.message ?: "неизвестная ошибка"), Toast.LENGTH_LONG).show()
            }
            return
        }
        if (requestCode != 2301 || resultCode != Activity.RESULT_OK || data == null) return
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip -> for (i in 0 until clip.itemCount) uris.add(clip.getItemAt(i).uri) }
        if (uris.isEmpty()) data.data?.let { uris.add(it) }
        if (uris.isEmpty()) return
        uris.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }
        }
        val items = uris.map { uri ->
            val name = queryAudioName(uri) ?: uri.lastPathSegment ?: "Аудиофайл"
            MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(name).build()).build()
        }
        val player = musicController
        if (player == null) {
            Toast.makeText(this, "Плеер ещё подключается. Повторите выбор через пару секунд.", Toast.LENGTH_LONG).show()
            return
        }
        player.setMediaItems(items, true)
        player.prepare()
        player.play()
        updateMusicStatus()
    }

    private fun queryAudioName(uri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor != null && cursor.moveToFirst()) cursor.getString(0) else null
        } catch (_: Exception) { null } finally { cursor?.close() }
    }

    private fun updateMusicStatus() {
        val player = musicController ?: run {
            musicStatusView?.text = "Подключение к плееру…"
            return
        }
        val liveTitle = player.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() }
        val itemTitle = player.currentMediaItem?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
        val title = liveTitle?.takeUnless { it == "MediaMetadata" }
            ?: itemTitle
            ?: if (player.mediaItemCount == 0) "Выберите радио или аудиофайл" else "Аудио"
        val state = when {
            player.playerError != null -> "Ошибка воспроизведения"
            player.isPlaying -> "▶ Воспроизводится"
            player.playbackState == Player.STATE_BUFFERING -> "⏳ Загрузка…"
            player.playbackState == Player.STATE_ENDED -> "Воспроизведение завершено"
            player.mediaItemCount > 0 -> "⏸ На паузе"
            else -> "Готов к работе"
        }
        musicStatusView?.text = "$title\n$state"
    }

    override fun onDestroy() {
        musicHandler.removeCallbacks(musicProgressTask)
        musicController?.removeListener(musicListener)
        musicControllerFuture?.let { MediaController.releaseFuture(it) }
        musicController = null
        super.onDestroy()
    }

    private fun openMusicSource(url:String, sourceName:String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (_:Exception) {
            Toast.makeText(this, "Не удалось открыть $sourceName. Проверьте, установлен ли браузер.", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value:Int):Int = (value * resources.displayMetrics.density).toInt()

    private fun roundedBackground(color: Int, radius: Float, strokeColor: Int? = null): android.graphics.drawable.Drawable {
        val shape = android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius.toInt()).toFloat()
            if (strokeColor != null) setStroke(dp(1), strokeColor)
        }
        return shape
    }

    private fun txt(value:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply {
        text=value; textSize=size; setTextColor(Color.rgb(35,48,38))
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
        setPadding(0,dp(7),0,dp(7))
    }
    private fun button(label:String, action:()->Unit) {
        val b = Button(this).apply {
            text=label
            isAllCaps=false
            textSize=14f
            minHeight=dp(48)
            setPadding(dp(14),dp(8),dp(14),dp(8))
            setTextColor(Color.rgb(34,76,45))
            background=roundedBackground(Color.WHITE,14f,Color.rgb(207,222,202))
            setOnClickListener { action() }
        }
        content.addView(b,LinearLayout.LayoutParams(-1,-2).apply {
            topMargin=dp(4); bottomMargin=dp(5)
        })
    }
    private fun card(title:String, body:String, action:(()->Unit)?=null) {
        val box=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(12),dp(16),dp(13))
            background=roundedBackground(Color.WHITE,16f,Color.rgb(224,232,220))
            if(action!=null) {
                isClickable=true
                isFocusable=true
                setOnClickListener { action() }
            }
        }
        box.addView(txt(title,18f,true))
        box.addView(txt(body,14f))
        content.addView(box,LinearLayout.LayoutParams(-1,-2).apply {
            bottomMargin=dp(10)
        })
    }

    private fun showFirstRunOrUpdateNotice() {
        if (!prefs.getBoolean("welcome_shown", false)) {
            prefs.edit().putBoolean("welcome_shown", true).putString("last_seen_version", BuildConfig.VERSION_NAME).apply()
            android.app.AlertDialog.Builder(this).setTitle("Добро пожаловать в «Мой Посевной»! 🌱")
                .setMessage("Планируйте посевы и садовые работы, ведите заметки, добавляйте культуры, создавайте PDF и сохраняйте резервные копии. Ваши данные хранятся на устройстве.")
                .setPositiveButton("Начать работу", null).show()
            return
        }
        if (prefs.getString("last_seen_version", "") != BuildConfig.VERSION_NAME) {
            prefs.edit().putString("last_seen_version", BuildConfig.VERSION_NAME).apply()
            showUpdateNotes(false)
        }
    }

    private fun showUpdateNotes(manual: Boolean) {
        android.app.AlertDialog.Builder(this).setTitle("Что нового · версия " + BuildConfig.VERSION_NAME)
            .setMessage("• Добавление собственных культур и иконок.\n• Заметки под календарями месяцев в PDF.\n• Резервное копирование и восстановление.\n• Индикаторы дней в календаре.\n• Отдельный раздел «О программе».")
            .setPositiveButton("Понятно", null).show()
    }

    private fun createBackup() {
        try {
            val json = JSONObject().put("format", 1).put("created", System.currentTimeMillis())
            val values = JSONObject()
            prefs.all.forEach { (k,v) -> values.put(k, JSONObject().put("type", when(v) { is String -> "s"; is Boolean -> "b"; is Int -> "i"; is Long -> "l"; is Float -> "f"; is Set<*> -> "ss"; else -> "x" }).put("value", v)) }
            json.put("preferences", values)
            val bytes = java.io.ByteArrayOutputStream()
            java.util.zip.ZipOutputStream(bytes).use { z -> z.putNextEntry(java.util.zip.ZipEntry("backup.json")); z.write(json.toString(2).toByteArray(Charsets.UTF_8)); z.closeEntry() }
            pendingBackupBytes = bytes.toByteArray()
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/zip"; putExtra(Intent.EXTRA_TITLE, "Moi_Posevnoy_backup.zip") }
            startActivityForResult(intent, 2401)
        } catch (e: Exception) { Toast.makeText(this, "Ошибка создания копии: " + (e.message ?: "ошибка"), Toast.LENGTH_LONG).show() }
    }
    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivityForResult(intent, 2402) }
        catch (_: Exception) { Toast.makeText(this, "Не удалось открыть выбор файла", Toast.LENGTH_LONG).show() }
    }

    private fun importBackup(uri: Uri) {
        try {
            val jsonText = contentResolver.openInputStream(uri)?.use { input ->
                java.util.zip.ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    var found: String? = null
                    while (entry != null) {
                        if (entry.name == "backup.json" && !entry.isDirectory) {
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var total = 0
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                total += count
                                if (total > 5 * 1024 * 1024) throw IllegalArgumentException("Файл резервной копии слишком большой")
                                output.write(buffer, 0, count)
                            }
                            found = output.toString(Charsets.UTF_8.name())
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    found
                }
            } ?: throw IllegalArgumentException("В архиве не найден backup.json")
            val root = JSONObject(jsonText)
            if (root.optInt("format", -1) != 1) throw IllegalArgumentException("Неподдерживаемый формат копии")
            val values = root.optJSONObject("preferences") ?: throw IllegalArgumentException("В копии нет настроек")

            // Validate all supported values before changing any current data.
            val parsed = mutableListOf<Pair<String, Pair<String, Any?>>>()
            val keys = values.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val item = values.optJSONObject(key) ?: throw IllegalArgumentException("Повреждена запись: $key")
                val type = item.optString("type", "")
                val value = item.opt("value")
                when (type) {
                    "s" -> parsed.add(key to (type to (value?.takeUnless { it == JSONObject.NULL }?.toString() ?: "")))
                    "b" -> {
                        if (value !is Boolean) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value))
                    }
                    "i" -> {
                        if (value !is Number) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value.toInt()))
                    }
                    "l" -> {
                        if (value !is Number) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value.toLong()))
                    }
                    "f" -> {
                        if (value !is Number) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value.toFloat()))
                    }
                    "ss" -> {
                        if (value !is org.json.JSONArray) throw IllegalArgumentException("Неверное значение: $key")
                        val set = linkedSetOf<String>()
                        for (i in 0 until value.length()) {
                            val itemValue = value.opt(i)
                            if (itemValue !is String) throw IllegalArgumentException("Неверное значение: $key")
                            set.add(itemValue)
                        }
                        parsed.add(key to (type to set))
                    }
                    else -> throw IllegalArgumentException("Неизвестный тип данных: $key")
                }
            }

            val editor = prefs.edit().clear()
            parsed.forEach { (key, pair) ->
                when (pair.first) {
                    "s" -> editor.putString(key, pair.second as String)
                    "b" -> editor.putBoolean(key, pair.second as Boolean)
                    "i" -> editor.putInt(key, pair.second as Int)
                    "l" -> editor.putLong(key, pair.second as Long)
                    "f" -> editor.putFloat(key, pair.second as Float)
                    "ss" -> editor.putStringSet(key, pair.second as Set<String>)
                }
            }
            if (!editor.commit()) throw IllegalStateException("Android не смог сохранить восстановленные данные")
            Toast.makeText(this, "Копия восстановлена. Данные загружены.", Toast.LENGTH_LONG).show()
            showHome()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось восстановить копию: ${e.message ?: "неверный архив"}. Текущие данные не изменены.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showHome() {
        baseLayout("Мой Посевной")
        content.addView(txt("Ваш помощник для сада и огорода",18f,true))
        content.addView(txt(SimpleDateFormat("d MMMM yyyy, EEEE",Locale("ru")).format(Date()),15f))
        val city = prefs.getString("garden_city", "Не выбран") ?: "Не выбран"
        val region = prefs.getString("garden_region", "Выберите город или геолокацию") ?: "Выберите город или геолокацию"
        val zone = prefs.getString("garden_zone", "Центральный") ?: "Центральный"
        card("📍 Место вашего огорода","$city · $region\nКлиматическая зона: $zone\nНажмите, чтобы изменить место.",{showLocationSettings()})
        card("🌿 План на сегодня",regionalTodayAdvice())
        card("🧤 Задачи по уходу",todayTasksSummary(),{showTasks()})
        button("Политика конфиденциальности") { showPrivacyPolicy() }
        card("🌦️ Погода на 5 дней","Прогноз на ближайшие 5 дней: температура, осадки и предупреждение о заморозках. Яндекс Погода доступна отдельной кнопкой внизу.",{loadWeather()})
        card("🪴 Полезный принцип","Посевные сроки — ориентир, а не гарантия. Сверяйте рекомендации с прогнозом заморозков, температурой почвы и условиями конкретной грядки.")
        card("🌙 Лунный календарь",lunarSummary(Calendar.getInstance()),{showCalendar()})
        card("📅 Выбрать дату","Откройте календарь, чтобы посмотреть сезонные подсказки и заметки.",{showCalendar()})
        card("🌱 Каталог культур","Овощи, зелень, ягоды и цветы — сроки, глубина посева и советы.",{showPlants("")})
        card("🔔 Напоминания","Ежедневное локальное напоминание запланировано на 09:00.")
        card("ℹ️ Важно","Это стартовая база. Универсальных дат посева для всей России не существует.")
    }


    private fun showAbout() {
        baseLayout("О программе")
        content.addView(txt("Версия ${BuildConfig.VERSION_NAME}", 16f, true))
        content.addView(txt("Календарь с индикаторами дней, собственные культуры, PDF с заметками и резервное копирование.", 14f))
        button("Что нового в версии") { showUpdateNotes(true) }
        button("📄 Создать PDF-планировщик") { showPaperPlanner() }
        button("📦 Создать резервную копию") { createBackup() }
        button("♻️ Восстановить резервную копию") { restoreBackup() }
        content.setPadding(dp(18), dp(16), dp(18), dp(30))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBackground(Color.WHITE, 22f, Color.rgb(213, 229, 207))
        }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            contentDescription = "Иконка Мой Посевной"
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        hero.addView(logo, LinearLayout.LayoutParams(dp(124), dp(124)))
        hero.addView(TextView(this).apply {
            text = "Мой Посевной"
            textSize = 27f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(3))
        })
        hero.addView(TextView(this).apply {
            text = "Ваш помощник в саду и огороде"
            textSize = 15f
            setTextColor(Color.rgb(79, 99, 79))
            gravity = Gravity.CENTER
        })
        content.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        card("🌱 Всё для заботы об урожае",
            "Календарь садово-огородных работ, лунные фазы, каталог культур, планирование задач и личные заметки — полезные инструменты в одном месте. Планируйте сезон, отмечайте важные дела и уделяйте больше внимания растениям.")
        card("📵 Без рекламы",
            "В приложении нет рекламы: никаких баннеров, внезапных роликов и рекламных пауз. Просто пользуйтесь инструментами и занимайтесь своим садом и огородом.")
        card("☕ Поддержать разработку",
            "Если приложение оказалось полезным, вы можете добровольно поблагодарить разработчика небольшим донатом. Это именно благодарность, а не покупка: донат не открывает новые функции, не снимает ограничения и не даёт дополнительных преимуществ. Все возможности приложения остаются доступными без оплаты. Не хотите или не можете поддержать — просто пользуйтесь приложением, никаких обязательств нет.")
        button("☕ Поблагодарить разработчика") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tips.tips/000490423")))
            } catch (_: Exception) {
                Toast.makeText(this, "Не удалось открыть страницу поддержки", Toast.LENGTH_LONG).show()
            }
        }
        button("Политика конфиденциальности") { showPrivacyPolicy() }
        button("← На главный экран") { showHome() }
        content.addView(TextView(this).apply {
            text = "Спасибо, что выбрали «Мой Посевной». Пусть ваши труды дают добрые всходы, а урожай радует каждый год!\n\nВерсия ${BuildConfig.VERSION_NAME}"
            textSize = 13f
            setTextColor(Color.rgb(93, 108, 91))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(14), dp(8), dp(4))
        })
    }

    private fun showLocationSettings() {
        baseLayout("Место и регион")
        val city = prefs.getString("garden_city","Не выбран") ?: "Не выбран"
        val region = prefs.getString("garden_region","Не выбран") ?: "Не выбран"
        val zone = prefs.getString("garden_zone","Центральный") ?: "Центральный"
        card("📍 Сейчас выбрано","$city\n$region\nКлиматическая зона: $zone")
        card("Почему это важно","Сроки посева зависят от климата, высоты участка, микроклимата и погоды. Региональные подсказки в приложении ориентировочные — перед высадкой проверяйте прогноз и температуру почвы.")
        button("Выбрать город или регион вручную") { showCityPicker() }
        button("Определить местоположение по GPS") { requestGardenLocation() }
        button("Вернуться на главный экран") { showHome() }
    }

    private fun showCityPicker() {
        val wrap = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,12,20,4) }
        val search = EditText(this).apply { hint="Поиск города или региона"; setSingleLine(true) }
        val list = ListView(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            cities.sortedBy { it.city }.map { it.city + " — " + it.region + " · " + it.zone })
        list.adapter = adapter
        wrap.addView(search)
        wrap.addView(list,LinearLayout.LayoutParams(-1,520))
        val dialog = android.app.AlertDialog.Builder(this).setTitle("Выберите город").setView(wrap).setNegativeButton("Отмена",null).create()
        fun refreshCities(query:String) {
            val filtered = cities.filter { it.city.contains(query,true) || it.region.contains(query,true) || it.zone.contains(query,true) }.sortedBy { it.city }
            adapter.clear()
            adapter.addAll(filtered.map { it.city + " — " + it.region + " · " + it.zone })
        }
        list.setOnItemClickListener { _,_,position,_ ->
            val shown = adapter.getItem(position).orEmpty()
            val selected = cities.firstOrNull { shown.startsWith(it.city + " — ") }
            if(selected != null) {
                saveGardenLocation(selected.city,selected.region,selected.zone,selected.latitude,selected.longitude)
                dialog.dismiss()
                showHome()
            }
        }
        search.addTextChangedListener(object:TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { refreshCities(s?.toString().orEmpty()) }
            override fun afterTextChanged(s:Editable?) {}
        })
        dialog.show()
    }

    private fun saveGardenLocation(city:String,region:String,zone:String,lat:Double,lon:Double) {
        prefs.edit().putString("garden_city",city).putString("garden_region",region).putString("garden_zone",zone)
            .putString("garden_lat",lat.toString()).putString("garden_lon",lon.toString()).putBoolean("garden_location_set",true).apply()
        Toast.makeText(this,"Место сохранено: $city",Toast.LENGTH_SHORT).show()
    }

    private fun requestGardenLocation() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),2002)
            return
        }
        resolveGardenLocation()
    }

    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode == 2002) {
            if(grantResults.any { it == PackageManager.PERMISSION_GRANTED }) resolveGardenLocation()
            else Toast.makeText(this,"Без разрешения на геолокацию выберите город вручную.",Toast.LENGTH_LONG).show()
        }
    }

    private fun resolveGardenLocation() {
        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val location = manager.getProviders(true).mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_:SecurityException) { null }
            }.maxByOrNull { it.time }
            if(location == null) {
                android.app.AlertDialog.Builder(this).setTitle("Местоположение пока недоступно")
                    .setMessage("Включите геолокацию на телефоне и попробуйте снова. Либо выберите город вручную.")
                    .setPositiveButton("Настройки") { _,_ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    .setNegativeButton("Выбрать город") { _,_ -> showCityPicker() }.show()
                return
            }
            val nearest = cities.minByOrNull { distanceKm(location.latitude,location.longitude,it.latitude,it.longitude) }
            if(nearest != null && distanceKm(location.latitude,location.longitude,nearest.latitude,nearest.longitude) <= 180.0) {
                saveGardenLocation(nearest.city,nearest.region,nearest.zone,location.latitude,location.longitude)
            } else {
                val zone = when {
                    location.latitude >= 63 -> "Север"
                    location.latitude >= 57 -> "Сибирь"
                    location.latitude <= 48 -> "Юг"
                    else -> "Центральный"
                }
                saveGardenLocation("Моё местоположение","Определено по GPS",zone,location.latitude,location.longitude)
            }
            showHome()
        } catch (_:SecurityException) {
            Toast.makeText(this,"Не удалось получить геолокацию. Выберите город вручную.",Toast.LENGTH_LONG).show()
        } catch (_:Exception) {
            Toast.makeText(this,"Ошибка определения местоположения. Выберите город вручную.",Toast.LENGTH_LONG).show()
        }
    }

    private fun distanceKm(lat1:Double,lon1:Double,lat2:Double,lon2:Double):Double {
        val r=6371.0
        val dLat=Math.toRadians(lat2-lat1)
        val dLon=Math.toRadians(lon2-lon1)
        val a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2)
        return 2*r*Math.asin(Math.sqrt(a.coerceIn(0.0,1.0)))
    }

    private fun tasksArray():JSONArray = try { JSONArray(prefs.getString("care_tasks","[]")) } catch (_:Exception) { JSONArray() }

    private fun todayTasksSummary():String {
        val today=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
        val tasks=tasksArray()
        var due=0
        var total=0
        for(i in 0 until tasks.length()) {
            val item=tasks.optJSONObject(i) ?: continue
            if(!item.optBoolean("done",false)) {
                total++
                if(item.optString("date") <= today) due++
            }
        }
        return if(total==0) "Задач пока нет. Добавьте полив, подкормку или другую работу." else "Открытых задач: $total. Сегодня или просрочено: $due."
    }

    private fun loadWeather() {
        val lat = prefs.getString("garden_lat", null)?.toDoubleOrNull()
        val lon = prefs.getString("garden_lon", null)?.toDoubleOrNull()
        if (lat == null || lon == null) {
            Toast.makeText(this, "Сначала выберите город или определите местоположение.", Toast.LENGTH_LONG).show()
            showLocationSettings()
            return
        }
        weatherLatitude = lat
        weatherLongitude = lon
        loadWeatherForecast(lat, lon)
    }

    override fun onBackPressed() {
        if (weatherPageOpen) {
            val lat = weatherLatitude
            val lon = weatherLongitude
            weatherPageOpen = false
            if (lat != null && lon != null) loadWeatherForecast(lat, lon) else showHome()
            return
        }
        if (weatherForecastOpen) {
            weatherForecastOpen = false
            showHome()
            return
        }
        super.onBackPressed()
    }

    private fun showYandexWeather(lat: Double, lon: Double) {
        weatherPageOpen = true
        weatherForecastOpen = false
        val url = String.format(Locale.US, "https://yandex.ru/pogoda/?lat=%.4f&lon=%.4f", lat, lon)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pale)
            setOnApplyWindowInsetsListener { view, insets ->
                val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
                val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.navigationBars()).bottom else 0
                view.setPadding(0, topInset, 0, bottomInset)
                insets
            }
        }
        root.addView(TextView(this).apply {
            text = "🌦️  Яндекс Погода"
            textSize = 23f
            setTextColor(Color.WHITE)
            setBackgroundColor(green)
            setPadding(dp(18), dp(18), dp(18), dp(18))
            gravity = Gravity.CENTER_VERTICAL
        })
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(Color.WHITE)
            addView(TextView(this@MainActivity).apply {
                val city = prefs.getString("garden_city", "Выбранное место") ?: "Выбранное место"
                text = "📍 $city"
                textSize = 14f
                setTextColor(Color.rgb(45, 65, 48))
            }, LinearLayout.LayoutParams(0, -2, 1f))
            addView(Button(this@MainActivity).apply {
                text = "← Назад · прогноз"
                isAllCaps = false
                setOnClickListener {
                    weatherPageOpen = false
                    loadWeatherForecast(lat, lon)
                }
            })
        })
        val webView = android.webkit.WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            webViewClient = object : android.webkit.WebViewClient() {
                override fun onReceivedError(
                    view: android.webkit.WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame == true) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity,
                                "Не удалось загрузить Яндекс Погоду. Проверьте интернет-соединение.",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            loadUrl(url)
        }
        root.addView(webView, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun loadWeatherForecast(lat: Double, lon: Double) {
        weatherPageOpen = false
        weatherForecastOpen = true
        val city = prefs.getString("garden_city", "Выбранное место") ?: "Выбранное место"
        baseLayout("Прогноз на 5 дней")
        content.addView(txt("📍 " + city, 17f, true))
        content.addView(txt("Источник: MET Norway. Данные сгруппированы по дням; лицензия CC BY 4.0. Яндекс Погода доступна отдельно.", 13f))
        button("← На главную") {
            weatherForecastOpen = false
            showHome()
        }
        content.addView(txt("Загружаю прогноз…", 15f))
        Thread {
            try {
                val url = String.format(Locale.US,
                    "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f", lat, lon)
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 12000
                connection.readTimeout = 12000
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "MoiPosevnoy/1.1.0 (https://github.com/goodrawru/myposev/issues)")
                connection.setRequestProperty("Accept", "application/json")
                val status = connection.responseCode
                if (status !in 200..299) throw java.io.IOException("HTTP " + status)
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val timeseries = JSONObject(body).getJSONObject("properties").getJSONArray("timeseries")
                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val displayFormat = SimpleDateFormat("EEE, d MMM", Locale("ru"))
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                data class DayWeather(
                    val date: Date,
                    var min: Double = Double.POSITIVE_INFINITY,
                    var max: Double = Double.NEGATIVE_INFINITY,
                    var symbol: String = "",
                    var rain: Double = 0.0,
                    var hasRain: Boolean = false
                )
                val days = linkedMapOf<String, DayWeather>()
                val now = System.currentTimeMillis()
                for (i in 0 until timeseries.length()) {
                    val row = timeseries.optJSONObject(i) ?: continue
                    val date = try { parser.parse(row.optString("time")) } catch (_: Exception) { null } ?: continue
                    if (date.time < now - 2 * 60 * 60 * 1000L) continue
                    val key = dayFormat.format(date)
                    if (days.size >= 5 && !days.containsKey(key)) continue
                    val data = row.optJSONObject("data") ?: continue
                    val instant = data.optJSONObject("instant")?.optJSONObject("details") ?: continue
                    val temp = instant.optDouble("air_temperature", Double.NaN)
                    if (!temp.isFinite()) continue
                    val day = days.getOrPut(key) { DayWeather(date) }
                    day.min = minOf(day.min, temp)
                    day.max = maxOf(day.max, temp)
                    val hour = java.util.Calendar.getInstance().apply { time = date }.get(java.util.Calendar.HOUR_OF_DAY)
                    val next6 = data.optJSONObject("next_6_hours")
                    val next1 = data.optJSONObject("next_1_hours")
                    val symbol = next6?.optJSONObject("summary")?.optString("symbol_code", "")
                        ?.takeIf { it.isNotBlank() }
                        ?: next1?.optJSONObject("summary")?.optString("symbol_code", "").orEmpty()
                    if (day.symbol.isBlank() || hour in 11..15) day.symbol = symbol
                    val details = next6?.optJSONObject("details") ?: next1?.optJSONObject("details")
                    val precipitation = details?.optDouble("precipitation_amount", Double.NaN) ?: Double.NaN
                    if (precipitation.isFinite()) {
                        day.rain += precipitation
                        day.hasRain = true
                    }
                }
                val result = days.values.take(5)
                if (result.isEmpty()) throw java.io.IOException("Пустой прогноз")
                val lines = result.map { day ->
                    val symbol = weatherSymbolRu(day.symbol)
                    val rainText = if (day.hasRain && day.rain >= 0.5) " · осадки ~" + String.format(Locale.US, "%.1f", day.rain) + " мм" else ""
                    displayFormat.format(day.date).replaceFirstChar { it.uppercase(Locale("ru")) } + "\n" +
                        symbol + "  " + day.min.toInt() + "…" + day.max.toInt() + " °C" + rainText
                }
                val frost = result.any { it.min <= 0.0 }
                val summary = lines.joinToString("\n\n") +
                    if (frost) "\n\n⚠️ Возможен заморозок. Защитите теплолюбивые растения." else
                    "\n\nСверяйте прогноз с условиями именно вашего участка."
                prefs.edit().putString("weather_summary", summary).apply()
                runOnUiThread {
                    if (!isFinishing && weatherForecastOpen) renderWeatherForecast(city, summary, lat, lon)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing && weatherForecastOpen) {
                        content.removeAllViews()
                        val savedForecast = prefs.getString("weather_summary", null)
                        if (!savedForecast.isNullOrBlank()) {
                            content.addView(txt("Не удалось обновить прогноз. Показаны последние сохранённые данные.", 15f, true))
                            savedForecast.split("\n\n").forEachIndexed { index, part ->
                                card(if (index == 0) "Последний сохранённый прогноз" else "Информация", part)
                            }
                        } else {
                            content.addView(txt("Прогноз сейчас недоступен. Основные функции приложения продолжают работать без VPN и интернета; попробуйте позже или откройте Яндекс Погоду.", 15f))
                        }
                        button("Повторить загрузку") { loadWeatherForecast(lat, lon) }
                        button("Открыть Яндекс Погоду") {
                            weatherForecastOpen = false
                            showYandexWeather(lat, lon)
                        }
                        button("← На главную") {
                            weatherForecastOpen = false
                            showHome()
                        }
                    }
                }
            }
        }.start()
    }

    private fun renderWeatherForecast(city: String, summary: String, lat: Double, lon: Double) {
        baseLayout("Прогноз на 5 дней")
        content.addView(txt("📍 " + city, 17f, true))
        content.addView(txt("Источник: MET Norway · данные адаптированы и сгруппированы по дням · CC BY 4.0", 12f))
        button("Лицензия CC BY 4.0") { openExternalLink("https://creativecommons.org/licenses/by/4.0/") }
        val parts = summary.split("\n\n")
        parts.forEachIndexed { index, part ->
            if (index == parts.lastIndex && (part.startsWith("⚠️") || part.startsWith("Сверяйте"))) {
                card("Совет для огорода", part)
            } else {
                card(if (index == 0) "🌤️ Ближайший прогноз" else "📅 День " + (index + 1), part)
            }
        }
        button("↻ Обновить прогноз") { loadWeatherForecast(lat, lon) }
        button("🌦️ Открыть Яндекс Погоду") {
            weatherForecastOpen = false
            showYandexWeather(lat, lon)
        }
        button("← На главную") {
            weatherForecastOpen = false
            showHome()
        }
    }

    private fun weatherSymbolRu(code: String): String {
        val normalized = code.lowercase(Locale.US)
        return when {
            "thunder" in normalized -> "⛈️ Гроза"
            "snow" in normalized && "rain" in normalized -> "🌨️ Мокрый снег"
            "snow" in normalized -> "❄️ Снег"
            "sleet" in normalized -> "🌨️ Дождь со снегом"
            "rain" in normalized -> "🌧️ Дождь"
            "fog" in normalized -> "🌫️ Туман"
            "partlycloudy" in normalized -> "🌤️ Переменная облачность"
            "cloudy" in normalized -> "☁️ Облачно"
            "fair" in normalized -> "🌤️ Малооблачно"
            "clearsky" in normalized -> "☀️ Ясно"
            else -> "🌥️ Переменная погода"
        }
    }

    private fun showPrivacyPolicy() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Конфиденциальность")
            .setMessage("Приложение не требует учётной записи и не содержит рекламы или аналитических трекеров. Задачи, заметки и выбранный город хранятся локально на устройстве. Если вы запрашиваете прогноз, координаты выбранного места передаются сервису погоды MET Norway для получения прогноза. Календарь посева и заметки работают внутри приложения. Ссылки на внешние источники открываются отдельно в браузере. Выбранные аудиофайлы воспроизводятся с устройства и не отправляются разработчику.\n\nПолная политика конфиденциальности доступна в репозитории проекта.")
            .setPositiveButton("Открыть политику") { _, _ ->
                openExternalLink("https://github.com/goodrawru/myposev/blob/rustore/PRIVACY_POLICY.md")
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showTasks() {
        baseLayout("Задачи по уходу")
        content.addView(txt("Добавляйте планы и отмечайте выполненное. Для посева и пересадки показываем срок до даты или возраст посадки. Все даты указаны в формате день.месяц.год.",14f))
        val titleInput=EditText(this).apply { hint="Задача (например, посеять томаты)"; setSingleLine(true) }
        val cropInput=EditText(this).apply { hint="Культура или грядка (необязательно)"; setSingleLine(true) }
        val dateInput=EditText(this).apply {
            hint="Дата (дд.мм.гггг)"; setSingleLine(true); isFocusable=false; isClickable=true
            setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date()))
            setOnClickListener {
                val initial=parseTaskDate(text.toString()) ?: Calendar.getInstance()
                DatePickerDialog(this@MainActivity,{ _,year,month,day ->
                    val picked=Calendar.getInstance().apply {
                        set(Calendar.YEAR,year); set(Calendar.MONTH,month); set(Calendar.DAY_OF_MONTH,day)
                        set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
                    }
                    setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(picked.time))
                },initial.get(Calendar.YEAR),initial.get(Calendar.MONTH),initial.get(Calendar.DAY_OF_MONTH)).show()
            }
        }
        val type=Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                listOf("Уход","Посев","Полив","Подкормка","Прополка","Пересадка","Защита растений","Сбор урожая"))
        }
        content.addView(titleInput); content.addView(cropInput); content.addView(dateInput); content.addView(type)
        button("Добавить задачу") {
            val title=titleInput.text.toString().trim()
            val date=parseTaskDate(dateInput.text.toString().trim())
            if(title.isBlank()) { Toast.makeText(this,"Введите название задачи",Toast.LENGTH_SHORT).show(); return@button }
            if(date==null) { Toast.makeText(this,"Выберите корректную дату",Toast.LENGTH_SHORT).show(); return@button }
            val storedDate=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.time)
            val all=tasksArray()
            all.put(JSONObject().put("id",System.currentTimeMillis()).put("title",title).put("crop",cropInput.text.toString().trim())
                .put("type",type.selectedItem.toString()).put("date",storedDate).put("done",false))
            prefs.edit().putString("care_tasks",all.toString()).apply()
            showTasks()
        }
        content.addView(txt("Мои задачи",19f,true))
        val all=tasksArray()
        val entries=(0 until all.length()).mapNotNull { all.optJSONObject(it) }
            .sortedWith(compareBy<JSONObject> { it.optBoolean("done",false) }.thenBy { it.optString("date") })
        if(entries.isEmpty()) content.addView(txt("Пока задач нет. Добавьте первую задачу выше — она появится здесь.",14f))
        entries.forEach { item ->
            val done=item.optBoolean("done",false)
            val name=item.optString("title","Задача")
            val crop=item.optString("crop","")
            val taskType=item.optString("type","Уход")
            val details=formatTaskDate(item.optString("date"))+" · "+taskType+(if(crop.isNotBlank()) " · $crop" else "")
            val timing=taskTiming(item.optString("date"),taskType)
            card((if(done) "✅ " else "🟢 ")+name,details+(if(timing.isNotBlank()) "\n"+timing else "")+(if(done) "\nВыполнено" else ""))
            if(!done) button("Отметить выполненной: $name") {
                val current=tasksArray()
                for(i in 0 until current.length()) {
                    val row=current.optJSONObject(i) ?: continue
                    if(row.optLong("id")==item.optLong("id")) { row.put("done",true); break }
                }
                prefs.edit().putString("care_tasks",current.toString()).apply()
                showTasks()
            } else button("Удалить из истории: $name") {
                val current=tasksArray(); val filtered=JSONArray()
                for(i in 0 until current.length()) {
                    val row=current.optJSONObject(i) ?: continue
                    if(row.optLong("id")!=item.optLong("id")) filtered.put(row)
                }
                prefs.edit().putString("care_tasks",filtered.toString()).apply()
                showTasks()
            }
        }
    }

    private fun parseTaskDate(value:String):Calendar? {
        for(pattern in listOf("dd.MM.yyyy","yyyy-MM-dd")) {
            try {
                val format=SimpleDateFormat(pattern,Locale.US).apply { isLenient=false }
                val parsed=format.parse(value) ?: continue
                if(format.format(parsed)!=value) continue
                return Calendar.getInstance().apply {
                    time=parsed; set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0)
                }
            } catch (_:Exception) { }
        }
        return null
    }

    private fun formatTaskDate(value:String):String {
        val date=parseTaskDate(value) ?: return value
        return SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(date.time)
    }

    private fun calendarDayNumber(date:Calendar):Long = Math.floorDiv(date.timeInMillis + date.get(Calendar.ZONE_OFFSET) + date.get(Calendar.DST_OFFSET),86400000L)

    private fun taskTiming(value:String,taskType:String):String {
        val target=parseTaskDate(value) ?: return ""
        val today=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
        val days=(calendarDayNumber(target)-calendarDayNumber(today)).toInt()
        fun dayWord(number:Int):String {
            val n=number%100
            return when { n in 11..14 -> "дней"; number%10==1 -> "день"; number%10 in 2..4 -> "дня"; else -> "дней" }
        }
        val isPlanting=taskType=="Посев" || taskType=="Пересадка"
        return when {
            days>0 -> "До ${isPlanting.let { if (it) "посадки" else "задачи" }}: $days ${dayWord(days)}"
            days<0 && isPlanting -> "После даты посадки прошло ${-days} ${dayWord(-days)}"
            days<0 -> "Просрочено на ${-days} ${dayWord(-days)}"
            isPlanting -> if(taskType=="Посев") "Посев сегодня" else "Пересадка сегодня"
            else -> "Срок сегодня"
        }
    }
    private fun showCalendar() {
        baseLayout("Лунный посевной календарь")
        val monthTitle = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(selectedDate.time).replaceFirstChar { it.uppercase(Locale("ru")) }
        content.addView(txt("Посевной календарь · $monthTitle", 21f, true))
        content.addView(txt("Выберите дату, чтобы увидеть фазу Луны, сезонные работы, задачи и заметки. Лунные подсказки — традиционный ориентир; учитывайте погоду и состояние почвы.", 14f))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("‹" to -1, "Сегодня" to 0, "›" to 1).forEach { (label, delta) ->
            controls.addView(Button(this).apply { text = label; isAllCaps = false; setOnClickListener { if (delta == 0) selectedDate = Calendar.getInstance() else selectedDate.add(Calendar.MONTH, delta); showCalendar() } }, LinearLayout.LayoutParams(0, -2, if (delta == 0) 2f else 1f))
        }
        controls.addView(Button(this).apply { text = "Год −"; isAllCaps = false; setOnClickListener { selectedDate.add(Calendar.YEAR, -1); showCalendar() } }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(Button(this).apply { text = "Год +"; isAllCaps = false; setOnClickListener { selectedDate.add(Calendar.YEAR, 1); showCalendar() } }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(controls)
        val weekdays = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
        val header = GridLayout(this).apply { columnCount = 7; rowCount = 1 }
        weekdays.forEach { day -> header.addView(TextView(this).apply { text = day; gravity = Gravity.CENTER; textSize = 12f; setTextColor(if (day == "Сб" || day == "Вс") Color.rgb(180,75,55) else green); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(6), 0, dp(6)) }, GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0 }) }
        content.addView(header)
        val grid = GridLayout(this).apply { columnCount = 7; rowCount = 6; useDefaultMargins = false }
        val first = selectedDate.clone() as Calendar; first.set(Calendar.DAY_OF_MONTH, 1)
        val offset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val today = Calendar.getInstance()
        val tasks = tasksArray()
        for (cell in 0 until 42) {
            val number = cell - offset + 1
            val lp = GridLayout.LayoutParams(GridLayout.spec(cell / 7), GridLayout.spec(cell % 7, 1f)).apply { width = 0; height = dp(60); setMargins(dp(2), dp(2), dp(2), dp(2)) }
            if (number !in 1..first.getActualMaximum(Calendar.DAY_OF_MONTH)) { grid.addView(TextView(this), lp); continue }
            val day = (first.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, number) }
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
            val isToday = day.get(Calendar.YEAR) == today.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            val isSelected = day.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
            val note = !prefs.getString("note_" + dateString(day), "").isNullOrBlank()
            var count = 0
            for (i in 0 until tasks.length()) { val task = tasks.optJSONObject(i) ?: continue; if (task.optString("date") == key && !task.optBoolean("done", false)) count++ }
            val cellFill = when { isSelected -> Color.rgb(232,234,232); count > 0 || note -> Color.rgb(226,241,219); else -> Color.WHITE }
            val cellStroke = when { isSelected -> Color.rgb(91,119,94); isToday -> green; else -> Color.rgb(216,228,211) }
            val cellView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(1), dp(2), dp(1), dp(2)); background = roundedBackground(cellFill, 8f, cellStroke); isClickable = true; isFocusable = true; setOnClickListener { selectedDate = day.clone() as Calendar; showCalendar() } }
            val dayHeader = FrameLayout(this)
            dayHeader.addView(TextView(this).apply { text = lunarGlyph(day) + " " + number.toString(); textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.rgb(35,48,38)); setTypeface(null, if (isToday || isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL) }, FrameLayout.LayoutParams(-1, -1))
            dayHeader.addView(TextView(this).apply { text = "●"; textSize = 10f; gravity = Gravity.TOP or Gravity.END; setTextColor(dayIndicatorColor(day)); setPadding(0, 0, dp(2), 0); contentDescription = when (dayIndicatorColor(day)) { Color.rgb(46,139,87) -> "Условно благоприятный день"; Color.rgb(232,174,35) -> "Смешанный день"; else -> "День для осторожного планирования" } }, FrameLayout.LayoutParams(-1, -1))
            cellView.addView(dayHeader, LinearLayout.LayoutParams(-1, 0, 1f))
            cellView.addView(TextView(this).apply { text = (if (count > 0) "● $count" else "") + if (note) " ◆" else ""; textSize = 9f; gravity = Gravity.CENTER; setTextColor(if (count > 0 || note) Color.rgb(39,91,57) else Color.rgb(100,110,100)) }, LinearLayout.LayoutParams(-1, dp(14)))
            grid.addView(cellView, lp)
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2))
        val legend = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); background = roundedBackground(Color.WHITE, 10f, Color.rgb(216,228,211)) }
        legend.addView(txt("Обозначения календаря", 17f, true))
        listOf(
            Triple(Color.rgb(46,139,87), "Зелёная точка", "Условно благоприятный ориентир"),
            Triple(Color.rgb(232,174,35), "Жёлтая точка", "Смешанный день"),
            Triple(Color.rgb(210,65,65), "Красная точка", "Для осторожного планирования")
        ).forEach { item ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { text = "●"; textSize = 20f; setTextColor(item.first); setPadding(0, 0, dp(10), 0) })
            row.addView(txt(item.second + " — " + item.third, 13f))
            legend.addView(row)
        }
        legend.addView(txt("Традиционная лунная подсказка, не научный прогноз урожайности. ● с числом — незавершённые задачи; ◆ — есть заметка.", 12f))
        content.addView(legend)
        card("Фаза Луны", lunarSummary(selectedDate))
        card("Что делать в этом месяце", seasonalAdvice(selectedDate))
        button("Подробности выбранного дня") { showCalendarDay(selectedDate) }
        button("Добавить заметку") { val key = dateString(selectedDate); val input = EditText(this).apply { hint = "Заметка на выбранную дату"; setText(prefs.getString("note_" + key, "")) }; android.app.AlertDialog.Builder(this).setTitle(key).setView(input).setNegativeButton("Отмена", null).setPositiveButton("Сохранить") { _, _ -> prefs.edit().putString("note_" + key, input.text.toString().trim()).apply(); showCalendar() }.show() }
    }

    private fun showCalendarDay(date: Calendar) {
        val day = date.clone() as Calendar
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
        val tasks = tasksArray(); val lines = mutableListOf<String>()
        for (i in 0 until tasks.length()) { val item = tasks.optJSONObject(i) ?: continue; if (item.optString("date") == key) { val crop = item.optString("crop", "").trim(); val taskLine = (if (item.optBoolean("done", false)) "✓ " else "• ") + item.optString("title", "Задача") + " — " + item.optString("type", "Уход"); lines.add(if (crop.isBlank()) taskLine else "$taskLine\n  Культура/грядка: $crop") } }
        val note = prefs.getString("note_" + dateString(day), "").orEmpty()
        val body = dateString(day) + "\n\n" + lunarSummary(day) + "\n\n" + seasonalAdvice(day) + "\n\nЗадачи:\n" + (if (lines.isEmpty()) "На эту дату задач нет." else lines.joinToString("\n")) + "\n\nЗаметка: " + if (note.isBlank()) "нет" else note
        android.app.AlertDialog.Builder(this).setTitle("Календарь на день").setMessage(body).setPositiveButton("Добавить задачу") { _, _ -> addTaskForDate(day) }.setNegativeButton("Закрыть", null).show()
    }

    private fun addTaskForDate(date:Calendar) {
        val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,8,24,0) }
        val title=EditText(this).apply { hint="Например, проверить грядки"; setSingleLine(true) }
        val crop=EditText(this).apply { hint="Культура или грядка (необязательно)" }
        val types=listOf("Уход","Посев","Полив","Подкормка","Прополка","Пересадка","Защита растений","Сбор урожая")
        val type=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types) }
        val dateText=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.time)
        wrap.addView(title); wrap.addView(crop); wrap.addView(type)
        android.app.AlertDialog.Builder(this).setTitle("Запланировать на "+dateString(date)).setView(wrap)
            .setNegativeButton("Отмена",null).setPositiveButton("Сохранить") { _,_ ->
                val name=title.text.toString().trim()
                if(name.isBlank()) { Toast.makeText(this,"Введите название действия",Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val all=tasksArray()
                all.put(JSONObject().put("id",System.currentTimeMillis()).put("title",name).put("crop",crop.text.toString().trim())
                    .put("type",type.selectedItem.toString()).put("date",dateText).put("done",false))
                prefs.edit().putString("care_tasks",all.toString()).apply()
                Toast.makeText(this,"Задача добавлена",Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun openExternalLink(url:String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_:Exception) { Toast.makeText(this,"Не удалось открыть браузер.",Toast.LENGTH_LONG).show() }
    }
    private fun dateString(c:Calendar)=SimpleDateFormat("d MMMM yyyy",Locale("ru")).format(c.time)
    private fun seasonalAdvice(c:Calendar):String {
        val actualMonth = c.get(Calendar.MONTH)
        val month = if(actualMonth in Calendar.FEBRUARY..Calendar.SEPTEMBER)
            (actualMonth + regionMonthOffset() + 12) % 12 else actualMonth
        return when(month) {
        Calendar.DECEMBER,Calendar.JANUARY -> "Планируйте сорта и закупку семян. Зимний посев требует подходящих условий и досветки."
        Calendar.FEBRUARY -> "В некоторых регионах начинают посев культур с длинным рассадным периодом."
        Calendar.MARCH -> "Планируйте рассаду. Обеспечьте свет, умеренный полив и подходящую температуру."
        Calendar.APRIL -> "Посев холодостойких культур возможен по погоде и состоянию грунта."
        Calendar.MAY -> "Учитывайте регион и возвратные заморозки. Закаливайте рассаду."
        Calendar.JUNE -> "Следите за влажностью почвы, прореживанием всходов и защитой молодых растений."
        Calendar.JULY -> "Возможны повторные посевы некоторых зеленных и скороспелых культур."
        Calendar.AUGUST -> "Планируйте осенние посевы с учётом климата."
        Calendar.SEPTEMBER -> "Собирайте урожай, готовьте грядки и учитывайте сроки подзимних посевов."
        Calendar.OCTOBER -> "Убирайте участок и готовьте почву. Подзимний посев зависит от местной погоды."
        else -> "Подготовьте участок к зиме и составьте план следующего сезона."
        }
    }

    private fun regionMonthOffset():Int = when(prefs.getString("garden_zone","Центральный")) {
        "Юг" -> 1
        "Северо-Запад", "Урал", "Дальний Восток" -> -1
        "Сибирь", "Север" -> -2
        else -> 0
    }

    private fun regionalTodayAdvice():String {
        val city = prefs.getString("garden_city","город не выбран") ?: "город не выбран"
        val zone = prefs.getString("garden_zone","Центральный") ?: "Центральный"
        return "Регион: $city ($zone). " + seasonalAdvice(Calendar.getInstance()) +
            "\nСроки ориентировочные: проверяйте прогноз, температуру почвы и риск возвратных заморозков."
    }

    private fun lunarSummary(c:Calendar):String {
        val knownNewMoon=Calendar.getInstance().apply { set(2000,Calendar.JANUARY,6,18,14,0) }.timeInMillis
        val month=29.530588853*24*60*60*1000
        val age=((((c.timeInMillis-knownNewMoon)%month)+month)%month)/month
        val phase=when {
            age<0.035 || age>=0.965 -> "Новолуние"
            age<0.285 -> "Растущая Луна"
            age<0.535 -> "Полнолуние"
            age<0.785 -> "Убывающая Луна"
            else -> "Убывающая Луна"
        }
        return "$phase. Фаза вычислена приближённо. Лунные советы — традиционная практика и не заменяют агрономические рекомендации."
    }

    // Условная цветовая маркировка по лунному циклу: это традиционная подсказка, а не прогноз урожайности.
    private fun dayIndicatorColor(c: Calendar): Int {
        val knownNewMoon = Calendar.getInstance().apply { set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
        val synodicMonth = 29.530588853 * 24 * 60 * 60 * 1000
        val age = ((((c.timeInMillis - knownNewMoon) % synodicMonth) + synodicMonth) % synodicMonth) / synodicMonth
        return when {
            age < 0.035 || age in 0.22..0.28 || age in 0.47..0.535 || age in 0.72..0.78 || age >= 0.965 -> Color.rgb(232,174,35)
            age < 0.5 -> Color.rgb(46,139,87)
            else -> Color.rgb(210,65,65)
        }
    }

    private fun lunarGlyph(c: Calendar): String {
        val knownNewMoon = Calendar.getInstance().apply { set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
        val synodicMonth = 29.530588853 * 24 * 60 * 60 * 1000
        val age = ((((c.timeInMillis - knownNewMoon) % synodicMonth) + synodicMonth) % synodicMonth) / synodicMonth
        return when {
            age < 0.035 || age >= 0.965 -> "🌑"
            age < 0.22 -> "🌒"
            age < 0.28 -> "🌓"
            age < 0.47 -> "🌔"
            age < 0.535 -> "🌕"
            age < 0.72 -> "🌖"
            age < 0.78 -> "🌗"
            else -> "🌘"
        }
    }

    private fun plantEmoji(name: String): String {
        val n = name.lowercase(Locale.ROOT)
        return when {
            "томат" in n || "помидор" in n -> "🍅"
            "перец" in n -> "🫑"
            "огур" in n -> "🥒"
            "морков" in n -> "🥕"
            "редис" in n || "свёкл" in n || "свекл" in n -> "🌱"
            "укроп" in n || "петруш" in n || "зелень" in n -> "🌿"
            "салат" in n || "шпинат" in n -> "🥬"
            "капуст" in n -> "🥬"
            "клубник" in n || "земляник" in n -> "🍓"
            "цвет" in n || "бархатц" in n -> "🌼"
            "тыкв" in n -> "🎃"
            "кабач" in n -> "🥒"
            "лук" in n || "чеснок" in n -> "🧅"
            "горох" in n || "фасол" in n -> "🫛"
            "картоф" in n -> "🥔"
            "кукуруз" in n -> "🌽"
            "яблон" in n || "дерев" in n -> "🌳"
            "ягод" in n || "смородин" in n || "малин" in n -> "🫐"
            else -> "🌱"
        }
    }

    private fun plantIllustration(name:String): Int {
        val n=name.lowercase(Locale.ROOT)
        return when {
            "томат" in n -> R.drawable.plant_tomato
            "перец" in n -> R.drawable.plant_pepper
            "огурец" in n -> R.drawable.plant_cucumber
            "морковь" in n -> R.drawable.plant_carrot
            "редис" in n -> R.drawable.plant_radish
            "укроп" in n -> R.drawable.plant_dill
            "салат" in n -> R.drawable.plant_lettuce
            "капуста" in n -> R.drawable.plant_cabbage
            "клубник" in n || "земляник" in n -> R.drawable.plant_strawberry
            "бархатц" in n || "цвет" in n -> R.drawable.plant_flower
            "тыкв" in n || "кабач" in n -> R.drawable.plant_pumpkin
            "свёкл" in n || "свекл" in n -> R.drawable.plant_radish
            "лук" in n || "чеснок" in n -> R.drawable.plant_onion
            "горох" in n || "фасол" in n -> R.drawable.plant_pea
            "базилик" in n || "петруш" in n -> R.drawable.plant_herb
            "малин" in n || "смородин" in n -> R.drawable.plant_berry
            "яблон" in n -> R.drawable.plant_tree
            else -> R.drawable.plant_generic
        }
    }

    private val cropIconCatalog: JSONArray by lazy {
        try {
            val json = assets.open("crop-icons/catalog.json").bufferedReader().use { it.readText() }
            JSONObject(json).optJSONArray("crops") ?: JSONArray()
        } catch (_: Exception) { JSONArray() }
    }

    private fun cropIconChoices(): List<Pair<String, String>> {
        val choices = mutableListOf("generic" to "Росток")
        for (i in 0 until cropIconCatalog.length()) {
            val crop = cropIconCatalog.optJSONObject(i) ?: continue
            val id = crop.optString("id")
            val name = crop.optString("name")
            if (id.isNotBlank() && name.isNotBlank()) choices.add(id to name)
        }
        return choices
    }

    private fun userCropIconResource(key: String): Int = when (key) {
        "tomato" -> R.drawable.plant_tomato
        "pepper", "sweet_pepper", "hot_pepper" -> R.drawable.plant_pepper
        "cucumber" -> R.drawable.plant_cucumber
        "carrot" -> R.drawable.plant_carrot
        "radish", "daikon", "oilseed_radish", "beet" -> R.drawable.plant_radish
        "dill" -> R.drawable.plant_dill
        "lettuce", "spinach", "arugula", "watercress" -> R.drawable.plant_lettuce
        "cabbage", "white_cabbage", "red_cabbage", "cauliflower", "broccoli", "brussels_sprouts", "kohlrabi", "napa_cabbage", "kale" -> R.drawable.plant_cabbage
        "strawberry", "wild_strawberry" -> R.drawable.plant_strawberry
        "berry", "raspberry", "blackberry", "black_currant", "red_currant", "white_currant", "gooseberry", "honeysuckle", "blueberry", "cranberry", "lingonberry", "sea_buckthorn" -> R.drawable.plant_berry
        "flower", "marigold", "nasturtium", "calendula", "petunia", "aster", "zinnia", "cosmos", "snapdragon", "sweet_pea", "morning_glory", "dahlia", "gladiolus", "tulip", "daffodil" -> R.drawable.plant_flower
        "pumpkin", "zucchini", "pattypan", "melon", "watermelon" -> R.drawable.plant_pumpkin
        "onion", "leek", "garlic", "shallot", "chives", "welsh_onion" -> R.drawable.plant_onion
        "peas", "green_beans", "broad_beans", "vetch" -> R.drawable.plant_pea
        "basil", "parsley", "cilantro", "mint", "lemon_balm", "sorrel", "tarragon", "rosemary", "thyme", "oregano", "sage", "lovage", "marjoram", "savory", "fennel" -> R.drawable.plant_herb
        "apple", "pear", "plum", "sour_cherry", "sweet_cherry", "apricot", "peach", "cherry_plum", "quince", "rowan", "serviceberry", "rosehip", "walnut", "hazelnut", "almond", "viburnum", "hawthorn", "mulberry", "cornelian_cherry", "lemon" -> R.drawable.plant_tree
        "generic" -> R.drawable.plant_generic
        else -> R.drawable.plant_generic
    }

    private fun cropIconKeyForName(name: String): String {
        val n = name.lowercase(Locale.ROOT)
        val aliases = listOf(
            "томат" to "tomato", "огур" to "cucumber", "перец слад" to "sweet_pepper",
            "перец ост" to "hot_pepper", "перец" to "sweet_pepper", "баклаж" to "eggplant",
            "картоф" to "potato", "морков" to "carrot", "свёкл" to "beet", "свекл" to "beet",
            "редис" to "radish", "дайкон" to "daikon", "репа" to "turnip", "брюкв" to "rutabaga",
            "капуст" to "white_cabbage", "броккол" to "broccoli", "кольраб" to "kohlrabi",
            "кабач" to "zucchini", "патиссон" to "pattypan", "тыкв" to "pumpkin",
            "арбуз" to "watermelon", "дын" to "melon", "горох" to "peas", "фасол" to "green_beans",
            "кукуруз" to "corn", "лук-порей" to "leek", "чеснок" to "garlic", "лук" to "onion",
            "укроп" to "dill", "петруш" to "parsley", "кинз" to "cilantro", "базилик" to "basil",
            "мят" to "mint", "мелисс" to "lemon_balm", "шпинат" to "spinach", "салат" to "lettuce",
            "руккол" to "arugula", "щавел" to "sorrel", "мангольд" to "swiss_chard",
            "клубник" to "strawberry", "земляник" to "wild_strawberry", "малин" to "raspberry",
            "ежевик" to "blackberry", "голубик" to "blueberry", "яблон" to "apple", "груш" to "pear",
            "слив" to "plum", "вишн" to "sour_cherry", "черешн" to "sweet_cherry", "абрикос" to "apricot",
            "персик" to "peach", "миндал" to "almond", "бархатц" to "marigold", "настурц" to "nasturtium",
            "календул" to "calendula", "петуни" to "petunia", "астр" to "aster", "подсолнеч" to "sunflower",
            "горчиц" to "white_mustard", "фацели" to "phacelia", "люпин" to "lupin", "рож" to "rye",
            "овёс" to "oat", "овес" to "oat", "ячмен" to "barley", "гречих" to "buckwheat",
            "хмель" to "hops", "фенхел" to "fennel", "душиц" to "oregano", "шалф" to "sage"
        )
        return aliases.firstOrNull { n.contains(it.first) }?.second ?: "generic"
    }

    private fun setCropIcon(image: ImageView, key: String) {
        // Always install a visible fallback first; SVG parsing failures must never leave a blank card.
        image.setImageResource(R.drawable.plant_generic)
        try {
            var slug: String? = null
            for (i in 0 until cropIconCatalog.length()) {
                val crop = cropIconCatalog.optJSONObject(i) ?: continue
                if (crop.optString("id") == key) {
                    slug = crop.optString("sourceSlug").takeIf { it.isNotBlank() && it != "null" }
                    break
                }
            }
            if (slug != null && assets.list("crop-icons/svg")?.contains("$slug.svg") == true) {
                val svg = SVG.getFromAsset(assets, "crop-icons/svg/$slug.svg")
                val size = 128
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                svg.renderToCanvas(canvas)
                // Some SVGs parse without errors but render fully transparent on Android.
                // Only accept the SVG when it actually produced visible pixels.
                val pixels = IntArray(size * size)
                bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
                if (pixels.any { Color.alpha(it) > 8 }) {
                    image.setImageBitmap(bitmap)
                    return
                }
                bitmap.recycle()
            }
        } catch (_: Exception) { }
        image.setImageResource(userCropIconResource(key))
    }

    private fun userCrops(): JSONArray = try { JSONArray(prefs.getString("user_crops", "[]")) } catch (_: Exception) { JSONArray() }

    private fun addUserCrop(existing: JSONObject? = null) {
        val name = EditText(this).apply { hint = "Название культуры *"; setText(existing?.optString("name", "") ?: "") }
        val category = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Овощи","Зелень","Ягоды","Цветы","Другое")); setSelection((listOf("Овощи","Зелень","Ягоды","Цветы","Другое").indexOf(existing?.optString("category")).coerceAtLeast(0))) }
        val sowing = EditText(this).apply { hint = "Сроки посева"; setText(existing?.optString("sowing", "") ?: "") }
        val depth = EditText(this).apply { hint = "Глубина посева"; setText(existing?.optString("depth", "") ?: "") }
        val sprouts = EditText(this).apply { hint = "Сроки всходов"; setText(existing?.optString("sprouts", "") ?: "") }
        val advice = EditText(this).apply { hint = "Советы по выращиванию"; setText(existing?.optString("advice", "") ?: ""); minLines = 2 }
        val iconChoices = cropIconChoices()
        val legacyIconKey = when (existing?.optString("icon", "")) {
            "pepper" -> "sweet_pepper"
            "lettuce" -> "lettuce"
            "berry" -> "raspberry"
            "flower" -> "marigold"
            "pumpkin" -> "pumpkin"
            "onion" -> "onion"
            "pea" -> "peas"
            "herb" -> "basil"
            "tree" -> "apple"
            else -> existing?.optString("icon", "") ?: ""
        }
        val iconPicker = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, iconChoices.map { it.second })
            val index = iconChoices.indexOfFirst { it.first == legacyIconKey }.let { if (it >= 0) it else 0 }
            setSelection(index)
        }
        val iconPreview = ImageView(this).apply {
            run { val key = iconChoices[iconPicker.selectedItemPosition].first; setCropIcon(this, key) }
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        iconPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                setCropIcon(iconPreview, iconChoices[position].first)
            }
        }
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), dp(4)) }
        listOf<View>(name, category, sowing, depth, sprouts, advice, txt("Цветная иконка культуры",14f,true), iconPreview, iconPicker).forEach { form.addView(it) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle(if(existing == null) "Добавить свою культуру" else "Изменить культуру")
            .setView(ScrollView(this).apply { addView(form) }).setNegativeButton("Отмена", null).setPositiveButton("Сохранить", null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cropName = name.text.toString().trim()
                if (cropName.isBlank()) { name.error = "Введите название"; return@setOnClickListener }
                val all = userCrops(); val id = existing?.optString("id") ?: java.util.UUID.randomUUID().toString()
                val item = JSONObject().put("id",id).put("name",cropName).put("category",category.selectedItem.toString()).put("sowing",sowing.text.toString()).put("depth",depth.text.toString()).put("sprouts",sprouts.text.toString()).put("advice",advice.text.toString()).put("icon",iconChoices[iconPicker.selectedItemPosition].first).put("emoji",plantEmoji(cropName))
                val next = JSONArray(); var replaced = false
                for(i in 0 until all.length()) { val old = all.optJSONObject(i) ?: continue; if(old.optString("id")==id) { next.put(item); replaced=true } else next.put(old) }
                if(!replaced) next.put(item)
                prefs.edit().putString("user_crops",next.toString()).apply()
                dialog.dismiss(); showPlants("")
            }
        }
        dialog.show()
    }

    private var builtInCatalogExpanded = false

    private fun showPlants(query:String) {
        baseLayout("Каталог культур")
        val search = EditText(this).apply { hint = "Найти культуру или группу"; setSingleLine(true); setText(query) }
        content.addView(search)
        content.addView(Button(this).apply { text = "＋ Добавить свою культуру"; isAllCaps = false; setOnClickListener { addUserCrop() } })
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)

        fun addTileRows(target: LinearLayout, tiles: List<View>) {
            tiles.chunked(2).forEach { pair ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
                pair.forEach { tile -> row.addView(tile, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(5); marginStart = dp(5); bottomMargin = dp(10) }) }
                if (pair.size == 1) row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(5); marginEnd = dp(5) })
                target.addView(row, LinearLayout.LayoutParams(-1, -2))
            }
        }

        fun fill(filter:String) {
            results.removeAllViews()
            val mine = userCrops()
            val matches = (0 until mine.length()).mapNotNull { mine.optJSONObject(it) }
                .filter { it.optString("name").contains(filter,true) || it.optString("category").contains(filter,true) }
            if (matches.isNotEmpty()) {
                results.addView(txt("Мои культуры",19f,true))
                val userTiles = matches.map { crop ->
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(dp(8),dp(10),dp(8),dp(8))
                        background = roundedBackground(Color.WHITE,14f,Color.rgb(220,230,216))
                        elevation = dp(1).toFloat()
                    }
                    tile.addView(ImageView(this).apply {
                        run { val key = crop.optString("icon", "generic"); setCropIcon(this, key) }
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        contentDescription = "Иконка культуры " + crop.optString("name")
                    }, LinearLayout.LayoutParams(dp(76),dp(76)).apply { bottomMargin = dp(4) })
                    tile.addView(txt(crop.optString("name"),15f,true).apply { gravity = Gravity.CENTER; maxLines = 2 })
                    tile.addView(txt(crop.optString("category"),12f).apply { gravity = Gravity.CENTER })
                    tile.addView(Button(this).apply {
                        text = "Подробнее"; textSize = 11f; isAllCaps = false
                        setOnClickListener {
                            val details = listOf("Сроки посева" to "sowing","Глубина" to "depth","Всходы" to "sprouts","Советы" to "advice")
                                .mapNotNull { (label,key) -> crop.optString(key).takeIf { it.isNotBlank() }?.let { "$label: $it" } }
                                .joinToString("\n\n")
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle(crop.optString("name"))
                                .setMessage(crop.optString("category") + if(details.isBlank()) "" else "\n\n$details")
                                .setNeutralButton("Изменить") { _,_ -> addUserCrop(crop) }
                                .setNegativeButton("Закрыть",null)
                                .setPositiveButton("Удалить") { _,_ ->
                                    android.app.AlertDialog.Builder(this@MainActivity).setTitle("Удалить культуру?")
                                        .setMessage(crop.optString("name")).setNegativeButton("Отмена",null)
                                        .setPositiveButton("Удалить") { _,_ ->
                                            val next = JSONArray()
                                            for (i in 0 until mine.length()) { val v = mine.optJSONObject(i) ?: continue; if (v.optString("id") != crop.optString("id")) next.put(v) }
                                            prefs.edit().putString("user_crops",next.toString()).apply()
                                            showPlants("")
                                        }.show()
                                }.show()
                        }
                    })
                    tile
                }
                addTileRows(results,userTiles)
            }

            val builtIns = plants.filter { it.name.contains(filter,true) || it.group.contains(filter,true) }
            val shouldExpand = filter.isNotBlank() || builtInCatalogExpanded
            val heading = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,dp(4),0,dp(4)) }
            heading.addView(txt("Встроенные культуры (${builtIns.size})",19f,true),LinearLayout.LayoutParams(0,-2,1f))
            heading.addView(Button(this).apply {
                text = if(shouldExpand) "Свернуть ▲" else "Показать ▼"; textSize = 11f; isAllCaps = false
                setOnClickListener { builtInCatalogExpanded = !shouldExpand; fill(search.text.toString()) }
            })
            results.addView(heading)

            if (shouldExpand) {
                val builtInTiles = builtIns.map { p ->
                    val tile = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                        setPadding(dp(8),dp(10),dp(8),dp(10))
                        background = roundedBackground(Color.WHITE,14f,Color.rgb(220,230,216))
                        elevation = dp(1).toFloat(); isClickable = true; isFocusable = true
                        setOnClickListener {
                            android.app.AlertDialog.Builder(this@MainActivity).setTitle(p.name)
                                .setMessage("${p.group}\n\nПосев: ${p.sowing}\nГлубина: ${p.depth}\nВсходы: ${p.sprouts}\n\n${p.advice}")
                                .setPositiveButton("Закрыть",null).show()
                        }
                    }
                    tile.addView(ImageView(this).apply {
                        setCropIcon(this, cropIconKeyForName(p.name)); scaleType = ImageView.ScaleType.FIT_CENTER
                        setPadding(dp(3),dp(3),dp(3),dp(3))
                        background = roundedBackground(Color.rgb(241,247,237),12f)
                        contentDescription = "Иконка культуры: " + p.name
                    },LinearLayout.LayoutParams(dp(82),dp(82)).apply { bottomMargin = dp(5) })
                    tile.addView(txt(p.name,15f,true).apply { gravity = Gravity.CENTER; maxLines = 2 })
                    tile.addView(txt(p.group,12f).apply { gravity = Gravity.CENTER })
                    tile
                }
                addTileRows(results,builtInTiles)
            }
            if (results.childCount == 0) results.addView(txt("Ничего не найдено. Попробуйте другое название."))
        }
        fill(query)
        search.addTextChangedListener(object:android.text.TextWatcher {
            override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int) {}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int) { fill(s?.toString().orEmpty()) }
            override fun afterTextChanged(s:android.text.Editable?) {}
        })
    }

    private fun showDiary() {
        baseLayout("Мой огород")
        content.addView(txt("Мои посадки",20f,true))
        content.addView(txt("Записывайте сорт, место выращивания и заметки. Данные сохраняются на устройстве.",14f))
        val name=EditText(this@MainActivity).apply { hint="Культура (например, томат)"; setSingleLine(true) }
        val variety=EditText(this@MainActivity).apply { hint="Сорт"; setSingleLine(true) }
        val sowing=EditText(this@MainActivity).apply { hint="Дата посева (дд.мм.гггг)"; setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date())); setSingleLine(true) }
        val place=EditText(this@MainActivity).apply { hint="Место (теплица, грядка №1, балкон)"; setSingleLine(true) }
        val notes=EditText(this@MainActivity).apply { hint="Заметки (необязательно)"; minLines=2; maxLines=4 }
        content.addView(name); content.addView(variety); content.addView(sowing); content.addView(place); content.addView(notes)
        button("Добавить посадку") {
            val crop=name.text.toString().trim()
            val date=sowing.text.toString().trim()
            if(crop.isBlank()) { Toast.makeText(this@MainActivity,"Введите название культуры",Toast.LENGTH_SHORT).show(); return@button }
            if(parseTaskDate(date)==null) { Toast.makeText(this@MainActivity,"Введите дату в формате дд.мм.гггг",Toast.LENGTH_SHORT).show(); return@button }
            val records=try { JSONArray(prefs.getString("planting_records","[]")) } catch (_:Exception) { JSONArray() }
            records.put(JSONObject().put("id",System.currentTimeMillis()).put("crop",crop)
                .put("variety",variety.text.toString().trim()).put("sowing",date)
                .put("place",place.text.toString().trim()).put("notes",notes.text.toString().trim())
                .put("created",System.currentTimeMillis()))
            prefs.edit().putString("planting_records",records.toString()).apply()
            showDiary()
        }
        content.addView(txt("Сохранённые посадки",18f,true))
        val records=try { JSONArray(prefs.getString("planting_records","[]")) } catch (_:Exception) { JSONArray() }
        if(records.length()==0) content.addView(txt("Пока нет подробных посадок. Добавьте первую запись."))
        for(i in records.length()-1 downTo 0) {
            val item=records.optJSONObject(i) ?: continue
            val crop=item.optString("crop","Культура")
            val varName=item.optString("variety","")
            val sow=item.optString("sowing","")
            val placeName=item.optString("place","")
            val note=item.optString("notes","")
            val age=try {
                val d=parseTaskDate(sow)
                if(d!=null) {
                    val days=((System.currentTimeMillis()-d.timeInMillis)/86400000L).toInt()
                    if(days>=0) "Возраст: $days дн." else "До посева: ${-days} дн."
                } else ""
            } catch (_:Exception) { "" }
            val details=listOf("Посев: $sow",varName.takeIf{it.isNotBlank()}?.let{"Сорт: $it"},
                placeName.takeIf{it.isNotBlank()}?.let{"Место: $it"},age.takeIf{it.isNotBlank()},
                note.takeIf{it.isNotBlank()}?.let{"Заметки: $it"}).filterNotNull().joinToString("\n")
            card("🌱 $crop",details)
            button("Удалить посадку: $crop") {
                val current=try { JSONArray(prefs.getString("planting_records","[]")) } catch (_:Exception) { JSONArray() }
                val kept=JSONArray()
                for(j in 0 until current.length()) if(current.optJSONObject(j)?.optLong("id")!=item.optLong("id")) kept.put(current.getJSONObject(j))
                prefs.edit().putString("planting_records",kept.toString()).apply(); showDiary()
            }
        }
        val legacy=prefs.getString("plantings","").orEmpty().split("\n").filter{it.isNotBlank()}
        if(legacy.isNotEmpty()) {
            content.addView(txt("Старые записи",16f,true))
            legacy.forEach { card("🌿 $it","Запись из прежней версии дневника.") }
        }
        content.addView(txt("Журнал урожая",20f,true))
        val harvestCrop=EditText(this@MainActivity).apply { hint="Что собрали?"; setSingleLine(true) }
        val harvestAmount=EditText(this@MainActivity).apply { hint="Количество (например, 2,5 кг)"; setSingleLine(true) }
        val harvestDate=EditText(this@MainActivity).apply { hint="Дата сбора"; setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date())); setSingleLine(true) }
        val harvestNotes=EditText(this@MainActivity).apply { hint="Примечание (необязательно)"; setSingleLine(true) }
        content.addView(harvestCrop); content.addView(harvestAmount); content.addView(harvestDate); content.addView(harvestNotes)
        button("Записать урожай") {
            val crop=harvestCrop.text.toString().trim()
            val date=harvestDate.text.toString().trim()
            if(crop.isBlank() || harvestAmount.text.toString().trim().isBlank()) {
                Toast.makeText(this@MainActivity,"Укажите культуру и количество",Toast.LENGTH_SHORT).show(); return@button
            }
            if(parseTaskDate(date)==null) { Toast.makeText(this@MainActivity,"Проверьте дату сбора",Toast.LENGTH_SHORT).show(); return@button }
            val list=try { JSONArray(prefs.getString("harvest_records","[]")) } catch (_:Exception) { JSONArray() }
            list.put(JSONObject().put("id",System.currentTimeMillis()).put("crop",crop)
                .put("amount",harvestAmount.text.toString().trim()).put("date",date)
                .put("notes",harvestNotes.text.toString().trim()))
            prefs.edit().putString("harvest_records",list.toString()).apply(); showDiary()
        }
        val harvests=try { JSONArray(prefs.getString("harvest_records","[]")) } catch (_:Exception) { JSONArray() }
        if(harvests.length()==0) content.addView(txt("Записей об урожае пока нет."))
        for(i in harvests.length()-1 downTo 0) {
            val h=harvests.optJSONObject(i) ?: continue
            val desc=listOf("Сбор: ${h.optString("date")}","Количество: ${h.optString("amount")}",
                h.optString("notes").takeIf{it.isNotBlank()}?.let{"Примечание: $it"}).filterNotNull().joinToString("\n")
            card("🧺 ${h.optString("crop","Урожай")}",desc)
        }
        button("Очистить дневник посадок") {
            android.app.AlertDialog.Builder(this@MainActivity).setTitle("Очистить посадки?")
                .setMessage("Удалятся подробные и старые записи о посадках. Журнал урожая останется.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить") { _,_ -> prefs.edit().remove("plantings").remove("planting_records").apply(); showDiary() }.show()
        }
        button("Очистить журнал урожая") {
            android.app.AlertDialog.Builder(this@MainActivity).setTitle("Очистить журнал урожая?")
                .setMessage("Все записи о собранном урожае будут удалены.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить") { _,_ -> prefs.edit().remove("harvest_records").apply(); showDiary() }.show()
        }
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            val channel=NotificationChannel("posev_reminders","Напоминания о посадках",NotificationManager.IMPORTANCE_DEFAULT)
            channel.description="Ежедневные напоминания для сада и огорода"
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    private fun scheduleDailyReminder() {
        val alarm=getSystemService(ALARM_SERVICE) as AlarmManager
        val intent=Intent(this,ReminderReceiver::class.java)
        val pending=PendingIntent.getBroadcast(this,91,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val calendar=Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY,9); set(Calendar.MINUTE,0); set(Calendar.SECOND,0)
            if(before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR,1)
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,AlarmManager.INTERVAL_DAY,pending)
    }
}
