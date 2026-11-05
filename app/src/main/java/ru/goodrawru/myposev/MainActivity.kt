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
import android.location.Geocoder
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
import java.util.TimeZone
import android.net.Uri

data class Plant(val name:String, val group:String, val sowing:String, val depth:String, val sprouts:String, val advice:String)
data class GardenCity(val city:String, val region:String, val zone:String, val latitude:Double, val longitude:Double)


class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var selectedDate = Calendar.getInstance()
    // Temporary payloads retained while Android document picker is open.
    private var pendingPdfBytes: ByteArray? = null
    private var pendingBackupBytes: ByteArray? = null
    // Weather screen state; coordinates are null until a forecast location is selected.
    private var weatherForecastOpen = false

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
        GardenCity("Ялта","Крым","Юг",44.4952,34.1663),
        GardenCity("Алушта","Крым","Юг",44.6764,34.4100),
        GardenCity("Керчь","Крым","Юг",45.3568,36.4674),
        GardenCity("Евпатория","Крым","Юг",45.1906,33.3669),
        GardenCity("Феодосия","Крым","Юг",45.0319,35.3824),
        GardenCity("Судак","Крым","Юг",44.8500,34.9762),
        GardenCity("Саки","Крым","Юг",45.1342,33.6039),
        GardenCity("Бахчисарай","Крым","Юг",44.7521,33.8608),
        GardenCity("Джанкой","Крым","Юг",45.7092,34.3931),
        GardenCity("Армянск","Крым","Юг",46.1059,33.6921),
        GardenCity("Красноперекопск","Крым","Юг",45.9572,33.7965),
        GardenCity("Белогорск","Крым","Юг",45.0546,34.5999),
        GardenCity("Старый Крым","Крым","Юг",45.0292,35.0917),
        GardenCity("Щёлкино","Крым","Юг",45.4289,35.8251),
        GardenCity("Черноморское","Крым","Юг",45.5066,32.6977),
        GardenCity("Инкерман","Севастополь","Юг",44.6131,33.6090),
        GardenCity("Балаклава","Севастополь","Юг",44.5008,33.5982),
        GardenCity("Алупка","Крым","Юг",44.4180,34.0450),
        GardenCity("Гурзуф","Крым","Юг",44.5460,34.2780),
        GardenCity("Коктебель","Крым","Юг",44.9615,35.2477),
        GardenCity("Нижнегорский","Крым","Юг",45.4479,34.7384),
        GardenCity("Советский","Крым","Юг",45.3425,34.9241),
        GardenCity("Первомайское","Крым","Юг",45.7178,33.8553),
        GardenCity("Красногвардейское","Крым","Юг",45.5035,34.2936),
        GardenCity("Кировское","Крым","Юг",45.2296,35.2034),
        GardenCity("Раздольное","Крым","Юг",45.7681,33.4878),
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
        Plant("Салат листовой","Зелень","Весной и в прохладные периоды","0,5–1 см","4–10 дней","Мелкие семена не заглубляйте; почва должна оставаться влажной."),
        Plant("Капуста белокочанная","Капустные","По сроку созревания и планируемой высадке","0,5–1 см","4–10 дней","Рассаде нужны свет и проветривание. Учитывайте сорт и риск заморозков."),
        Plant("Клубника","Ягоды","Обычно весной или в конце лета","Корни расправить; сердечко не заглублять","Зависит от способа","Выберите солнечное место и не засыпайте точку роста."),
        Plant("Тыква","Овощи","После прогрева почвы или за 20–30 дней до высадки","3–5 см","5–10 дней","Нужны тепло, место для плетей и регулярный полив в период роста."),
        Plant("Кабачок","Овощи","После угрозы заморозков или за 20–25 дней до высадки","3–4 см","4–8 дней","Не загущайте посадки; собирайте молодые плоды регулярно."),
        Plant("Свёкла","Корнеплоды","Весной в прогретую почву; возможен летний посев","2–3 см","7–14 дней","Прореживайте всходы и поддерживайте равномерную влажность."),
        Plant("Чеснок","Луковые","Озимый осенью или яровой весной","3–6 см","Зависит от температуры","Выбирайте здоровые зубки и хорошо дренированное место."),
        Plant("Горох","Бобовые","Ранней весной, когда почва готова к обработке","3–5 см","7–14 дней","Предусмотрите опору для высокорослых сортов."),
        Plant("Фасоль","Бобовые","После прогрева почвы и окончания заморозков","3–5 см","7–14 дней","Теплолюбива; не сейте в холодную мокрую землю."),
        Plant("Базилик","Зелень","На рассаду в тепле или после заморозков","0,5–1 см","5–10 дней","Нужны тепло, свет и защита от холодных ночей."),
        Plant("Петрушка","Зелень","Ранней весной; возможен подзимний посев","1–2 см","14–25 дней","Всходит медленно; поддерживайте влажность до появления всходов."),
        Plant("Малина","Ягоды","Весной или осенью в подходящем регионе","Корневая шейка на уровне почвы","Зависит от посадочного материала","Подберите сорт и место с учётом зимостойкости и обеспечьте опору при необходимости."),
        Plant("Яблоня","Плодовые деревья","Весной или осенью, когда нет экстремальных условий","Корневая шейка на уровне почвы","Не применимо","Подберите подвой и сорт для региона; не заглубляйте место прививки."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED)
        migrateLegacyNoteKeys()
        createNotificationChannel()
        scheduleDailyReminder()
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
            text = tr("🌱  ") + tr(title); textSize=25f; setTextColor(Color.WHITE); setBackgroundColor(green)
            setPadding(20,22,20,22); gravity=Gravity.CENTER_VERTICAL
        })
        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedBackground(Color.rgb(248, 251, 245), 14f, Color.rgb(213, 229, 207))
        }
        sectionRow.addView(TextView(this).apply {
            text = tr("Раздел: $title")
            textSize = 12f
            setTextColor(Color.rgb(65, 78, 67))
            maxLines = 2
        }, LinearLayout.LayoutParams(0, -2, 1f))
        sectionRow.addView(Button(this).apply {
            text = tr("О программе")
            textSize = 12f
            isAllCaps = false
            minWidth = dp(72)
            minHeight = dp(44)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setTextColor(green)
            background = roundedBackground(Color.rgb(239, 246, 235), 12f, Color.rgb(213, 229, 207))
            stateListAnimator = null
            setOnClickListener { showAbout() }
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        root.addView(sectionRow, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = dp(10)
            marginEnd = dp(10)
            topMargin = dp(6)
            bottomMargin = dp(6)
        })
        val navScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; setBackgroundColor(Color.WHITE) }
        val nav = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE) }
        listOf("Сегодня","Календарь","Культуры","Задачи","Огород").forEach { label ->
            nav.addView(Button(this).apply {
                text=tr(label)
                textSize=12f
                isAllCaps=false
                minWidth=dp(72)
                minHeight=dp(44)
                setPadding(dp(10),dp(6),dp(10),dp(6))
                setTextColor(green)
                background=roundedBackground(Color.rgb(239,246,235),12f,Color.rgb(213,229,207))
                stateListAnimator = null
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
        val yearInput = EditText(this).apply { hint = tr("Год"); setText(currentYear.toString()); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setSingleLine(true) }
        content.addView(txt("Год календаря", 16f, true)); content.addView(yearInput)
        val choices = listOf("calendar" to "Посевной календарь", "work" to "Сезонные рекомендации и работы", "tasks" to "Мои задачи и статус выполнения", "plants" to "Каталог культур (культуры с доступными значками)", "notes" to "Мои заметки", "blank" to "Страницы для записей")
        val checks = mutableMapOf<String, CheckBox>()
        choices.forEachIndexed { index, item -> val cb = CheckBox(this).apply { text = item.second; isChecked = index < 4; setTextColor(Color.rgb(42,55,43)) }; checks[item.first] = cb; content.addView(cb) }
        val periodLabels = listOf("Весь год", "Весна (март–май)", "Лето (июнь–август)", "Осень (сентябрь–ноябрь)", "Зима (декабрь–февраль)")
        val period = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, periodLabels.map { tr(it) }) }
        content.addView(txt("Период", 16f, true)); content.addView(period)
        val colorMode = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, listOf("Цветной", "Чёрно-белый, экономный").map { tr(it) }) }
        content.addView(txt("Оформление", 16f, true)); content.addView(colorMode)
        button("Сформировать и сохранить PDF") {
            val year = yearInput.text.toString().toIntOrNull()
            if (year == null || year !in 2000..2100) { Toast.makeText(this, tr("Введите год от 2000 до 2100"), Toast.LENGTH_LONG).show(); return@button }
            val selected = checks.filterValues { it.isChecked }.keys.toSet()
            if (selected.isEmpty()) { Toast.makeText(this, tr("Выберите хотя бы один раздел"), Toast.LENGTH_LONG).show(); return@button }
            try {
                pendingPdfBytes = buildPaperPdf(year, period.selectedItemPosition, selected, colorMode.selectedItemPosition == 0)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "application/pdf"; putExtra(Intent.EXTRA_TITLE, "Мой_Посевной_" + year + ".pdf") }
                startActivityForResult(intent, 2027)
            } catch (e: Exception) { pendingPdfBytes = null; Toast.makeText(this, tr("Ошибка подготовки PDF: ") + (e.message ?: "неизвестная ошибка"), Toast.LENGTH_LONG).show() }
        }
        card("Что попадёт в документ", "Календарь с датами и фазами Луны, рекомендации по месяцам, задачи с культурой/грядкой и статусом, доступные встроенные культуры и выбранные заметки. Длинные разделы переносятся на следующие страницы.")
    }

    private fun buildPaperPdf(year: Int, periodIndex: Int, selected: Set<String>, colorMode: Boolean): ByteArray {
        val pdfLocale = if (isEnglish()) Locale.ENGLISH else Locale("ru"); val doc = PdfDocument(); val width = 595; val height = 842; val left = 42f; val right = 553f; val bottom = 795f
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 19f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = if (colorMode) android.graphics.Color.rgb(39,91,57) else android.graphics.Color.BLACK }
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = if (colorMode) android.graphics.Color.rgb(39,91,57) else android.graphics.Color.BLACK }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f; color = android.graphics.Color.DKGRAY }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 8f; color = android.graphics.Color.GRAY }
        var page: PdfDocument.Page? = null; var canvas: android.graphics.Canvas? = null; var y = 0f; var pageNo = 0
        fun newPage(title: String? = null) {
            if (page != null) { canvas!!.drawText(if (isEnglish()) "My Sowing Calendar • $year • page $pageNo" else "Мой Посевной • $year • стр. $pageNo", left, 822f, footerPaint); doc.finishPage(page!!) }
            pageNo++; page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create()); canvas = page!!.canvas; canvas!!.drawColor(android.graphics.Color.WHITE); y = 48f
            if (title != null) { canvas!!.drawText(tr(title), left, y, titlePaint); y += 28f; canvas!!.drawLine(left, y, right, y, headPaint); y += 20f }
        }
        fun ensure(h: Float = 14f) { if (y + h > bottom) newPage() }
        fun line(value: String, paint: Paint = textPaint, indent: Float = 0f) {
            val maxWidth = right - left - indent; var row = ""
            for (word in tr(value).split(Regex("\\s+"))) { val candidate = if (row.isEmpty()) word else row + " " + word; if (paint.measureText(candidate) > maxWidth && row.isNotEmpty()) { ensure(paint.textSize + 5f); canvas!!.drawText(row, left + indent, y, paint); y += paint.textSize + 4f; row = word } else row = candidate }
            if (row.isNotEmpty()) { ensure(paint.textSize + 5f); canvas!!.drawText(row, left + indent, y, paint); y += paint.textSize + 4f }
        }
        fun section(value: String) { ensure(25f); y += 5f; line(value, headPaint); y += 3f }
        val months = when (periodIndex) { 1 -> listOf(2,3,4); 2 -> listOf(5,6,7); 3 -> listOf(8,9,10); 4 -> listOf(11,0,1); else -> (0..11).toList() }
        newPage("Мой Посевной — бумажный вариант"); line("План огородника на " + year, headPaint); line("Сформировано: " + SimpleDateFormat("dd.MM.yyyy", pdfLocale).format(Date())); line("Период: " + listOf("весь год","весна","лето","осень","зима")[periodIndex]); line("Лунные обозначения — справочная подсказка. Учитывайте регион, погоду и температуру почвы."); y += 12f
        section("Содержание"); if ("calendar" in selected) line("Посевной календарь"); if ("work" in selected) line("Сезонные рекомендации"); if ("tasks" in selected) line("Мои задачи"); if ("plants" in selected) line("Каталог культур"); if ("notes" in selected) line("Мои заметки"); if ("blank" in selected) line("Страницы для записей")
        if ("calendar" in selected) months.forEach { month ->
            val actualYear = if (periodIndex == 4 && month == 11) year - 1 else year
            val c = Calendar.getInstance().apply { set(actualYear, month, 1, 12, 0, 0) }
            newPage("Календарь — " + SimpleDateFormat("LLLL yyyy", pdfLocale).format(c.time))
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
                    val displayDate = runCatching { SimpleDateFormat("dd.MM.yyyy", pdfLocale).format(SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(rawDate)!!) }.getOrDefault(rawDate)
                    line("$displayDate — $noteValue")
                }
            }
        }
        if ("work" in selected) { newPage("Сезонные рекомендации"); months.forEach { month -> val actualYear = if (periodIndex == 4 && month == 11) year - 1 else year; val c = Calendar.getInstance().apply { set(actualYear, month, 15, 12, 0, 0) }; section(SimpleDateFormat("LLLL", pdfLocale).format(c.time)); line(seasonalAdvice(c)) } }
        if ("tasks" in selected) {
            newPage("Мои задачи"); val tasks = tasksArray(); var count = 0
            for (i in 0 until tasks.length()) { val item = tasks.optJSONObject(i) ?: continue; val date = item.optString("date", ""); if (date.length < 7 || date.substring(0,4).toIntOrNull() != year || (date.substring(5,7).toIntOrNull()?.minus(1) !in months)) continue; count++; section(date + " • " + item.optString("title", "Задача")); line("Тип работы: " + item.optString("type", "Уход")); val crop = item.optString("crop", "").trim(); if (crop.isNotEmpty()) line("Культура/грядка: " + crop); line("Статус: " + if (item.optBoolean("done", false)) "Выполнено" else "Запланировано") }
            if (count == 0) line("На выбранный период задач нет.")
        }
        if ("plants" in selected) { newPage("Каталог культур"); builtInPlants.forEach { p -> section(p.name); line("Группа: " + p.group); line("Срок посева: " + p.sowing); line("Глубина: " + p.depth); line("Всходы: " + p.sprouts); line(p.advice) } }
        if ("notes" in selected) { newPage("Мои заметки"); val notes = prefs.all.filter { it.key.startsWith("note_") && it.value is String && (it.value as String).isNotBlank() }.toSortedMap(); if (notes.isEmpty()) line("Сохранённых заметок нет."); notes.forEach { entry -> section(entry.key.removePrefix("note_")); line(entry.value.toString()) } }
        if ("blank" in selected) repeat(3) { index -> newPage("Свободные записи " + (index + 1)); repeat(22) { canvas!!.drawLine(left, y, right, y, footerPaint); y += 30f } }
        if (page != null) { canvas!!.drawText(if (isEnglish()) "My Sowing Calendar • $year • page $pageNo" else "Мой Посевной • $year • стр. $pageNo", left, 822f, footerPaint); doc.finishPage(page!!) }
        val out = java.io.ByteArrayOutputStream(); doc.writeTo(out); doc.close(); return out.toByteArray()
    }
    @Deprecated("Uses the document picker to support older Android versions")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2401) {
            val bytes = pendingBackupBytes; pendingBackupBytes = null
            if (resultCode != Activity.RESULT_OK || data?.data == null || bytes == null) return
            try { contentResolver.openOutputStream(data.data!!)?.use { it.write(bytes) }; Toast.makeText(this, tr("Резервная копия сохранена"), Toast.LENGTH_LONG).show() }
            catch (e: Exception) { Toast.makeText(this, tr("Ошибка сохранения копии"), Toast.LENGTH_LONG).show() }
            return
        }
        if (requestCode == 2402) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                android.app.AlertDialog.Builder(this)
                    .setTitle(tr("Заменить данные приложения?"))
                    .setMessage(tr("Восстановление заменит текущие настройки, задачи, заметки и каталог культур данными из резервной копии."))
                    .setNegativeButton(tr("Отмена"), null)
                    .setPositiveButton(tr("Восстановить")) { _, _ -> importBackup(data.data!!) }
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
                Toast.makeText(this, tr("PDF сохранён"), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, tr("Ошибка сохранения: ") + (e.message ?: "неизвестная ошибка"), Toast.LENGTH_LONG).show()
            }
            return
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

    // First-stage built-in localization. User data and persisted keys are never translated.
    private fun isEnglish(): Boolean = prefs.getString("app_language", "ru") == "en"

    private fun tr(value: String): String {
        if (!isEnglish()) return value
        val exact = mapOf(
            "Сегодня" to "Today",
            "Календарь" to "Calendar",
            "Культуры" to "Crops",
            "Задачи" to "Tasks",
            "Огород" to "Garden",
            "Раздел: " to "Section: ",
            "О программе" to "About",
            "Каталог культур" to "Crop catalog",
            "Мой огород" to "My garden",
            "Бумажный вариант" to "Printable planner",
            "Открыть погоду" to "Open weather",
            "Посевной календарь" to "Sowing calendar",
            "Мои культуры" to "My crops",
            "Мои посадки" to "My plantings",
            "Мои заметки" to "My notes",
            "Сохранённые посадки" to "Saved plantings",
            "Журнал урожая" to "Harvest log",
            "Добавить свою культуру" to "Add a custom crop",
            "＋ Добавить свою культуру" to "＋ Add a custom crop",
            "Найти культуру или группу" to "Search crops or groups",
            "Подробнее" to "Details",
            "Выбрать" to "Select",
            "Сорт" to "Variety",
            "Дата посева (дд.мм.гггг)" to "Sowing date (dd.mm.yyyy)",
            "Место (теплица, грядка №1, балкон)" to "Location (greenhouse, bed #1, balcony)",
            "Заметки (необязательно)" to "Notes (optional)",
            "Добавить посадку" to "Add planting",
            "Введите название культуры" to "Enter a crop name",
            "Введите дату в формате дд.мм.гггг" to "Enter the date as dd.mm.yyyy",
            "Пока нет подробных посадок. Добавьте первую запись." to "No detailed plantings yet. Add your first entry.",
            "Что собрали?" to "What did you harvest?",
            "Количество (например, 2,5 кг)" to "Amount (e.g. 2.5 kg)",
            "Дата сбора" to "Harvest date",
            "Примечание (необязательно)" to "Comment (optional)",
            "Записать урожай" to "Record harvest",
            "Укажите культуру и количество" to "Enter the crop and amount",
            "Проверьте дату сбора" to "Check the harvest date",
            "Записей об урожае пока нет." to "No harvest records yet.",
            "Очистить дневник посадок" to "Clear planting diary",
            "Очистить журнал урожая" to "Clear harvest log",
            "Очистить посадки?" to "Clear plantings?",
            "Удалятся подробные и старые записи о посадках. Журнал урожая останется." to "Detailed and older planting records will be deleted. The harvest log will remain.",
            "Все записи о собранном урожае будут удалены." to "All harvest records will be deleted.",
            "Отмена" to "Cancel",
            "Очистить" to "Clear",
            "Сохранить" to "Save",
            "Изменить" to "Edit",
            "Закрыть" to "Close",
            "Удалить" to "Delete",
            "Удалить культуру?" to "Delete crop?",
            "Версия " to "Version ",
            "Номер сборки: " to "Build number: ",
            "Что нового в версии" to "What's new",
            "📄 Создать PDF-планировщик" to "📄 Create PDF planner",
            "📦 Создать резервную копию" to "📦 Create backup",
            "♻️ Восстановить резервную копию" to "♻️ Restore backup",
            "Политика конфиденциальности" to "Privacy policy",
            "← На главный экран" to "← Home",
            "Язык / Language" to "Language / Язык",
            "Выберите язык приложения" to "Choose app language",
            "Русский" to "Russian",
            "Английский" to "English",
            "Применить язык можно после перезапуска раздела." to "The language is applied when the screen reloads.",
            "Календарь с индикаторами дней, собственные культуры, PDF с заметками и резервное копирование." to "Calendar with day indicators, custom crops, PDF planner with notes, and backups.",
            "Ваш помощник в саду и огороде" to "Your gardening companion",
            "🌱 Всё для заботы об урожае" to "🌱 Everything for a better harvest",
            "📵 Без рекламы" to "📵 No ads",
            "☕ Поддержать разработку" to "☕ Support development",
            "☕ Поблагодарить разработчика" to "☕ Thank the developer",
            "Не удалось открыть страницу поддержки" to "Could not open the support page",
            "Спасибо, что выбрали «Мой Посевной». Пусть ваши труды дают добрые всходы, а урожай радует каждый год!" to "Thank you for choosing My Sowing Calendar. May your efforts sprout well and bring a great harvest every year!",
            "Томат" to "Tomato",
            "Огурец" to "Cucumber",
            "Перец сладкий" to "Sweet pepper",
            "Перец острый" to "Hot pepper",
            "Баклажан" to "Eggplant",
            "Картофель" to "Potato",
            "Морковь" to "Carrot",
            "Свёкла" to "Beetroot",
            "Редис" to "Radish",
            "Дайкон" to "Daikon radish",
            "Репа" to "Turnip",
            "Брюква" to "Rutabaga",
            "Капуста белокочанная" to "White cabbage",
            "Капуста краснокочанная" to "Red cabbage",
            "Цветная капуста" to "Cauliflower",
            "Брокколи" to "Broccoli",
            "Брюссельская капуста" to "Brussels sprouts",
            "Кольраби" to "Kohlrabi",
            "Кабачок" to "Zucchini",
            "Патиссон" to "Pattypan squash",
            "Тыква" to "Pumpkin",
            "Арбуз" to "Watermelon",
            "Дыня" to "Melon",
            "Горох" to "Peas",
            "Фасоль стручковая" to "Green beans",
            "Бобы" to "Broad beans",
            "Кукуруза сахарная" to "Sweet corn",
            "Лук репчатый" to "Bulb onion",
            "Лук-порей" to "Leek",
            "Чеснок" to "Garlic",
            "Сельдерей корневой" to "Celeriac",
            "Сельдерей черешковый" to "Celery",
            "Спаржа" to "Asparagus",
            "Пастернак" to "Parsnip",
            "Батат" to "Sweet potato",
            "Укроп" to "Dill",
            "Петрушка" to "Parsley",
            "Кинза" to "Cilantro",
            "Базилик" to "Basil",
            "Мята" to "Mint",
            "Мелисса" to "Lemon balm",
            "Шпинат" to "Spinach",
            "Салат листовой" to "Leaf lettuce",
            "Руккола" to "Arugula",
            "Щавель" to "Sorrel",
            "Мангольд" to "Swiss chard",
            "Кресс-салат" to "Watercress",
            "Эстрагон" to "Tarragon",
            "Розмарин" to "Rosemary",
            "Тимьян" to "Thyme",
            "Клубника" to "Garden strawberry",
            "Земляника" to "Wild strawberry",
            "Малина" to "Raspberry",
            "Ежевика" to "Blackberry",
            "Смородина чёрная" to "Blackcurrant",
            "Смородина красная" to "Redcurrant",
            "Смородина белая" to "White currant",
            "Крыжовник" to "Gooseberry",
            "Жимолость съедобная" to "Edible honeysuckle",
            "Голубика" to "Blueberry",
            "Клюква" to "Cranberry",
            "Брусника" to "Lingonberry",
            "Виноград" to "Grapes",
            "Облепиха" to "Sea buckthorn",
            "Актинидия" to "Actinidia",
            "Яблоня" to "Apple tree",
            "Груша" to "Pear tree",
            "Слива" to "Plum",
            "Вишня" to "Sour cherry",
            "Черешня" to "Sweet cherry",
            "Абрикос" to "Apricot",
            "Персик" to "Peach",
            "Алыча" to "Cherry plum",
            "Айва" to "Quince",
            "Рябина" to "Rowan",
            "Ирга" to "Serviceberry",
            "Шиповник" to "Rosehip",
            "Грецкий орех" to "Walnut",
            "Фундук" to "Hazelnut",
            "Миндаль" to "Almond",
            "Калина" to "Viburnum",
            "Боярышник" to "Hawthorn",
            "Шелковица" to "Mulberry",
            "Кизил" to "Cornelian cherry",
            "Лимон" to "Lemon",
            "Бархатцы" to "Marigolds",
            "Настурция" to "Nasturtium",
            "Календула" to "Calendula",
            "Петуния" to "Petunia",
            "Астра" to "Aster",
            "Цинния" to "Zinnia",
            "Космея" to "Cosmos",
            "Львиный зев" to "Snapdragon",
            "Душистый горошек" to "Sweet pea",
            "Ипомея" to "Morning glory",
            "Подсолнечник декоративный" to "Ornamental sunflower",
            "Георгина" to "Dahlia",
            "Гладиолус" to "Gladiolus",
            "Тюльпан" to "Tulip",
            "Нарцисс" to "Daffodil",
            "Горчица белая" to "White mustard",
            "Фацелия" to "Phacelia",
            "Люпин" to "Lupin",
            "Вика" to "Vetch",
            "Клевер белый" to "White clover",
            "Клевер красный" to "Red clover",
            "Люцерна" to "Alfalfa",
            "Рожь" to "Rye",
            "Овёс" to "Oats",
            "Ячмень" to "Barley",
            "Гречиха" to "Buckwheat",
            "Рапс" to "Rapeseed",
            "Редька масличная" to "Oilseed radish",
            "Донник" to "Sweet clover",
            "Эспарцет" to "Sainfoin",
            "Лук-шалот" to "Shallot",
            "Шнитт-лук" to "Chives",
            "Лук-батун" to "Welsh onion",
            "Капуста пекинская" to "Napa cabbage",
            "Кейл" to "Kale",
            "Ревень" to "Rhubarb",
            "Хрен" to "Horseradish",
            "Хмель" to "Hops",
            "Фенхель" to "Fennel",
            "Душица" to "Oregano",
            "Шалфей" to "Sage",
            "Любисток" to "Lovage",
            "Майоран" to "Marjoram",
            "Чабер" to "Savory",
            "Артишок" to "Artichoke",
            "Овощи" to "Vegetables",
            "Корнеплоды" to "Root vegetables",
            "Капустные" to "Brassicas",
            "Тыквенные" to "Cucurbits",
            "Бобовые" to "Legumes",
            "Зерновые и овощи" to "Grains and vegetables",
            "Луковые" to "Alliums",
            "Зелень" to "Leafy greens",
            "Пряные травы" to "Culinary herbs",
            "Ягоды" to "Berries",
            "Ягоды и лианы" to "Berries and vines",
            "Плодовые деревья" to "Fruit trees",
            "Плодовые кустарники" to "Fruit shrubs",
            "Орехоплодные" to "Nut crops",
            "Цитрусовые" to "Citrus",
            "Цветы" to "Flowers",
            "Сидераты" to "Green manure",
            "Многолетние овощи" to "Perennial vegetables",
            "Лианы" to "Vines",
            "Культуры" to "Crops",
            "Погода на 7 дней" to "7-day weather",
            "Прогноз погоды на 7 дней." to "7-day weather forecast.",
            "Источник данных: 7Timer!" to "Data source: 7Timer!",
            "← На главную" to "← Home",
            "Прогноз по дням: температура, облачность, осадки и ветер." to "Daily forecast: temperature, cloud cover, precipitation and wind.",
            "🌦️ Погода на 7 дней" to "🌦️ 7-day weather",
            "🌙 Лунный календарь" to "🌙 Lunar calendar",
            "📅 Выбрать дату" to "📅 Choose a date",
            "🌱 Каталог культур" to "🌱 Crop catalog",
            "🔔 Напоминания" to "🔔 Reminders",
            "🧤 Задачи по уходу" to "🧤 Garden tasks",
            "🌿 План на сегодня" to "🌿 Today's plan",
            "🪴 Полезный принцип" to "🪴 Useful principle",
            "ℹ️ Важно" to "ℹ️ Important",
            "Климатическая зона" to "Climate zone",
            "Нажмите, чтобы изменить место." to "Tap to change location.",
            "Посевные сроки — ориентир, а не гарантия. Сверяйте рекомендации с прогнозом заморозков, температурой почвы и условиями конкретной грядки." to "Sowing dates are guidelines, not guarantees. Check frost forecasts, soil temperature and the conditions of your specific bed.",
            "Откройте календарь, чтобы посмотреть сезонные подсказки и заметки." to "Open the calendar to view seasonal tips and notes.",
            "Овощи, зелень, ягоды и цветы — сроки, глубина посева и советы." to "Vegetables, herbs, berries and flowers — timing, sowing depth and tips.",
            "Выберите дату, чтобы увидеть фазу Луны, сезонные работы, задачи и заметки. Лунные подсказки — традиционный ориентир; учитывайте погоду и состояние почвы." to "Choose a date to view the Moon phase, seasonal work, tasks and notes. Lunar tips are traditional guidance; consider the weather and soil conditions.",
            "Пн" to "Mo",
            "Вт" to "Tu",
            "Ср" to "We",
            "Чт" to "Th",
            "Пт" to "Fr",
            "Сб" to "Sa",
            "Вс" to "Su",
            "Год −" to "Year −",
            "Год +" to "Year +",
            "Задачи по уходу" to "Garden tasks",
            "Добавляйте планы и отмечайте выполненное. Для посева и пересадки показываем срок до даты или возраст посадки. Все даты указаны в формате день.месяц.год." to "Add plans and mark them complete. For sowing and transplanting, the app shows time remaining or planting age. Dates use day.month.year format.",
            "Задача (например, посеять томаты)" to "Task (e.g. sow tomatoes)",
            "Культура или грядка (необязательно)" to "Crop or bed (optional)",
            "Дата (дд.мм.гггг)" to "Date (dd.mm.yyyy)",
            "Уход" to "Care",
            "Посев" to "Sowing",
            "Полив" to "Watering",
            "Подкормка" to "Fertilizing",
            "Прополка" to "Weeding",
            "Пересадка" to "Transplanting",
            "Защита растений" to "Plant protection",
            "Сбор урожая" to "Harvest",
            "Добавить задачу" to "Add task",
            "Введите название задачи" to "Enter a task title",
            "Выберите корректную дату" to "Choose a valid date",
            "Выполнено" to "Completed",
            "Не выполнено" to "Not completed",
            "Нет задач на сегодня." to "No tasks for today.",
            "Напоминания" to "Reminders",
            "Ежедневное напоминание" to "Daily reminder",
            "Разрешить уведомления" to "Allow notifications",
            "Сохранить настройки" to "Save settings",
            "Местоположение" to "Location",
            "Выбрать город вручную" to "Choose city manually",
            "Определить местоположение" to "Detect location",
            "Введите город или регион" to "Enter a city or region",
            "Сохранить место" to "Save location",
            "Не удалось определить местоположение" to "Could not determine location",
            "Нет данных о погоде" to "No weather data available",
            "Обновить прогноз" to "Refresh forecast",
            "Загрузка прогноза..." to "Loading forecast...",
            "Солнечно" to "Sunny",
            "Облачно" to "Cloudy",
            "Дождь" to "Rain",
            "Снег" to "Snow",
            "Ветер" to "Wind",
            "Осадки" to "Precipitation",
            "Температура" to "Temperature",
            "Благоприятный день" to "Favorable day",
            "Неблагоприятный день" to "Unfavorable day",
            "Нейтральный день" to "Neutral day",
            "Лунная фаза" to "Moon phase",
            "Сезонные работы" to "Seasonal work",
            "Заметки на день" to "Daily notes",
            "Добавить заметку" to "Add note",
            "Сохранить заметку" to "Save note",
            "Заметка сохранена" to "Note saved",
            "Нет заметок на эту дату." to "No notes for this date.",
            "Создать PDF-планировщик" to "Create PDF planner",
            "PDF-планировщик" to "PDF planner",
            "Подготовка PDF..." to "Preparing PDF...",
            "PDF успешно создан" to "PDF created successfully",
            "Не удалось создать PDF" to "Could not create PDF",
            "Резервная копия создана" to "Backup created",
            "Не удалось создать резервную копию" to "Could not create backup",
            "Восстановить резервную копию" to "Restore backup",
            "Выберите файл резервной копии" to "Choose a backup file",
            "Резервная копия успешно восстановлена" to "Backup restored successfully",
            "Не удалось восстановить резервную копию" to "Could not restore backup",
            "Вы уверены?" to "Are you sure?",
            "Овощи" to "Vegetables",
            "Корнеплоды" to "Root vegetables",
            "Капустные" to "Brassicas",
            "Тыквенные" to "Cucurbits",
            "Бобовые" to "Legumes",
            "Зерновые и овощи" to "Grains and vegetables",
            "Луковые" to "Alliums",
            "Зелень" to "Leafy greens",
            "Пряные травы" to "Herbs",
            "Ягоды" to "Berries",
            "Плодовые деревья" to "Fruit trees",
            "Цветы" to "Flowers",
            "Другое" to "Other",
            "Томат" to "Tomato",
            "Огурец" to "Cucumber",
            "Перец сладкий" to "Sweet pepper",
            "Перец острый" to "Hot pepper",
            "Баклажан" to "Eggplant",
            "Картофель" to "Potato",
            "Морковь" to "Carrot",
            "Свёкла" to "Beetroot",
            "Редис" to "Radish",
            "Дайкон" to "Daikon",
            "Репа" to "Turnip",
            "Брюква" to "Rutabaga",
            "Капуста белокочанная" to "White cabbage",
            "Капуста краснокочанная" to "Red cabbage",
            "Цветная капуста" to "Cauliflower",
            "Брокколи" to "Broccoli",
            "Брюссельская капуста" to "Brussels sprouts",
            "Кольраби" to "Kohlrabi",
            "Кабачок" to "Zucchini",
            "Патиссон" to "Pattypan squash",
            "Тыква" to "Pumpkin",
            "Арбуз" to "Watermelon",
            "Дыня" to "Melon",
            "Горох" to "Peas",
            "Фасоль стручковая" to "Green beans",
            "Бобы" to "Broad beans",
            "Кукуруза сахарная" to "Sweet corn",
            "Лук репчатый" to "Onion",
            "Лук-порей" to "Leek",
            "Чеснок" to "Garlic",
            "Сельдерей корневой" to "Celeriac",
            "Сельдерей черешковый" to "Celery",
            "Спаржа" to "Asparagus",
            "Пастернак" to "Parsnip",
            "Батат" to "Sweet potato",
            "Укроп" to "Dill",
            "Петрушка" to "Parsley",
            "Кинза" to "Cilantro",
            "Базилик" to "Basil",
            "Мята" to "Mint",
            "Мелисса" to "Lemon balm",
            "Шпинат" to "Spinach",
            "Салат листовой" to "Leaf lettuce",
            "Руккола" to "Arugula",
            "Щавель" to "Sorrel",
            "Мангольд" to "Swiss chard",
            "Клубника" to "Strawberry",
            "Земляника" to "Wild strawberry",
            "Малина" to "Raspberry",
            "Ежевика" to "Blackberry",
            "Голубика" to "Blueberry",
            "Яблоня" to "Apple tree",
            "Груша" to "Pear tree",
            "Слива" to "Plum tree",
            "Вишня" to "Sour cherry",
            "Черешня" to "Sweet cherry",
            "Абрикос" to "Apricot",
            "Персик" to "Peach",
            "Миндаль" to "Almond",
            "Бархатцы" to "Marigold",
            "Настурция" to "Nasturtium",
            "Календула" to "Calendula",
            "Петуния" to "Petunia",
            "Астра" to "Aster",
            "Подсолнечник" to "Sunflower",
            "Горчица" to "Mustard",
            "Фацелия" to "Phacelia",
            "Люпин" to "Lupin",
            "Рожь" to "Rye",
            "Овёс" to "Oats",
            "Ячмень" to "Barley",
            "Гречиха" to "Buckwheat",
            "Хмель" to "Hops",
            "Фенхель" to "Fennel",
            "Душица" to "Oregano",
            "Шалфей" to "Sage",
            "Сроки посева" to "Sowing time",
            "Глубина посева" to "Sowing depth",
            "Сроки всходов" to "Germination time",
            "Советы по выращиванию" to "Growing tips",
            "Цветная иконка культуры" to "Crop icon",
            "Предпросмотр иконки культуры" to "Crop icon preview",
            "Название культуры *" to "Crop name *",
            "Глубина посева" to "Sowing depth",
            "Сроки всходов" to "Germination time",
            "Советы по выращиванию" to "Growing tips",
            "Изменить культуру" to "Edit crop",
            "Введите название" to "Enter a name",
            "Введите название действия" to "Enter an activity name",
            "Культура/грядка" to "Crop/bed",
            "Культура/грядка: " to "Crop/bed: ",
            "На эту дату задач нет." to "No tasks are scheduled for this date.",
            "Задачи:" to "Tasks:",
            "Заметка: " to "Note: ",
            "нет" to "none",
            "На выбранную дату задач нет." to "No tasks are scheduled for the selected date.",
            "Обозначения календаря" to "Calendar legend",
            "Зелёная точка" to "Green dot",
            "Условно благоприятный ориентир" to "Tentatively favorable guideline",
            "Жёлтая точка" to "Yellow dot",
            "Смешанный день" to "Mixed day",
            "Красная точка" to "Red dot",
            "Для осторожного планирования" to "Plan cautiously",
            "Традиционная лунная подсказка, не научный прогноз урожайности. ● с числом — незавершённые задачи; ◆ — есть заметка." to "Traditional lunar guidance, not a scientific yield forecast. ● with a number indicates unfinished tasks; ◆ indicates a note.",
            "Подробности выбранного дня" to "Selected day details",
            "Заметка на выбранную дату" to "Note for selected date",
            "Календарь на день" to "Daily calendar",
            "Фаза вычислена приближённо. Лунные советы — традиционная практика и не заменяют агрономические рекомендации." to "The phase is approximate. Lunar advice is a tradition and does not replace agronomic recommendations.",
            "Выберите город или геолокацию" to "Choose a city or use location",
            "Моё местоположение" to "My location",
            "Выбранное место" to "Selected location",
            "город не выбран" to "No city selected",
            "Определить по GPS" to "Detect via GPS",
            "Регион" to "Region",
            "Зона выращивания" to "Growing zone",
            "Юг" to "South",
            "Центральный" to "Central",
            "Северо-Запад" to "Northwest",
            "Поволжье" to "Volga region",
            "Сибирь" to "Siberia",
            "Север" to "North",
            "Урал" to "Urals",
            "Дальний Восток" to "Far East",
            "Москва" to "Moscow",
            "Санкт-Петербург" to "Saint Petersburg",
            "Минск" to "Minsk",
            "Алматы" to "Almaty",
            "Крым" to "Crimea",
            "Ленинградская область" to "Leningrad Region",
            "Калининградская область" to "Kaliningrad Region",
            "Псковская область" to "Pskov Region",
            "Воронежская область" to "Voronezh Region",
            "Белгородская область" to "Belgorod Region",
            "Краснодарский край" to "Krasnodar Krai",
            "Ростовская область" to "Rostov Region",
            "Ставропольский край" to "Stavropol Krai",
            "Волгоградская область" to "Volgograd Region",
            "Самарская область" to "Samara Region",
            "Республика Татарстан" to "Republic of Tatarstan",
            "Нижегородская область" to "Nizhny Novgorod Region",
            "Свердловская область" to "Sverdlovsk Region",
            "Пермский край" to "Perm Krai",
            "Челябинская область" to "Chelyabinsk Region",
            "Тюменская область" to "Tyumen Region",
            "Новосибирская область" to "Novosibirsk Region",
            "Омская область" to "Omsk Region",
            "Красноярский край" to "Krasnoyarsk Krai",
            "Иркутская область" to "Irkutsk Region",
            "Республика Саха (Якутия)" to "Sakha Republic (Yakutia)",
            "Хабаровский край" to "Khabarovsk Krai",
            "Приморский край" to "Primorsky Krai",
            "Сахалинская область" to "Sakhalin Region",
            "Мурманская область" to "Murmansk Region",
            "Архангельская область" to "Arkhangelsk Region",
            "Беларусь" to "Belarus",
            "Казахстан" to "Kazakhstan",
            "Срок сегодня" to "Due today",
            "Посев сегодня" to "Sowing today",
            "Пересадка сегодня" to "Transplanting today",
            "Просрочено на " to "Overdue by ",
            "После даты посадки прошло " to "Since planting date: ",
            "До посадки: " to "Until planting: ",
            "До задачи: " to "Until task: ",
            "день" to "day",
            "дня" to "days",
            "дней" to "days",
            "Планируйте сорта и закупку семян. Зимний посев требует подходящих условий и досветки." to "Plan varieties and seed purchases. Winter sowing requires suitable conditions and supplemental lighting.",
            "В некоторых регионах начинают посев культур с длинным рассадным периодом." to "In some regions, sowing begins for crops with a long seedling period.",
            "Планируйте рассаду. Обеспечьте свет, умеренный полив и подходящую температуру." to "Plan seedling production. Provide light, moderate watering and a suitable temperature.",
            "Посев холодостойких культур возможен по погоде и состоянию грунта." to "Cold-hardy crops can be sown depending on weather and soil conditions.",
            "Учитывайте регион и возвратные заморозки. Закаливайте рассаду." to "Consider your region and late frosts. Harden off seedlings.",
            "Следите за влажностью почвы, прореживанием всходов и защитой молодых растений." to "Monitor soil moisture, thin seedlings and protect young plants.",
            "Возможны повторные посевы некоторых зеленных и скороспелых культур." to "Some leafy greens and fast-maturing crops can be sown again.",
            "Планируйте осенние посевы с учётом климата." to "Plan autumn sowing according to your climate.",
            "Собирайте урожай, готовьте грядки и учитывайте сроки подзимних посевов." to "Harvest crops, prepare beds and consider winter-sowing dates.",
            "Убирайте участок и готовьте почву. Подзимний посев зависит от местной погоды." to "Clear the plot and prepare the soil. Winter sowing depends on local weather.",
            "Подготовьте участок к зиме и составьте план следующего сезона." to "Prepare the plot for winter and plan the next season.",
            "Сейте в рыхлый грунт. После всходов обеспечьте хорошее освещение; сроки зависят от сорта и региона." to "Sow in loose soil. Provide good light after emergence; timing depends on the variety and region.",
            "Перец любит тепло. Не высаживайте в холодную почву; учитывайте местную погоду." to "Peppers need warmth. Do not plant in cold soil; consider local weather.",
            "Не любит повреждения корней. Рассаду сейте в отдельные ёмкости." to "Roots are sensitive to damage. Sow seedlings in individual containers.",
            "Нужны рыхлая почва и равномерная влажность. Не вносите свежий навоз перед посевом." to "Needs loose soil and even moisture. Do not apply fresh manure before sowing.",
            "В жару может стрелковаться. Поддерживайте равномерный полив." to "May bolt in hot weather. Keep watering even.",
            "Мелкие семена не заглубляйте; почва должна оставаться влажной." to "Do not bury small seeds deeply; keep the soil moist.",
            "Рассаде нужны свет и проветривание. Учитывайте сорт и риск заморозков." to "Seedlings need light and ventilation. Consider the variety and frost risk.",
            "Выберите солнечное место и не засыпайте точку роста." to "Choose a sunny spot and do not cover the crown.",
            "Нужны тепло, место для плетей и регулярный полив в период роста." to "Needs warmth, room for vines and regular watering during growth.",
            "Не загущайте посадки; собирайте молодые плоды регулярно." to "Avoid overcrowding; harvest young fruits regularly.",
            "Прореживайте всходы и поддерживайте равномерную влажность." to "Thin seedlings and maintain even moisture.",
            "Выбирайте здоровые зубки и хорошо дренированное место." to "Choose healthy cloves and a well-drained site.",
            "Предусмотрите опору для высокорослых сортов." to "Provide support for tall varieties.",
            "Теплолюбива; не сейте в холодную мокрую землю." to "Heat-loving; do not sow in cold, wet soil.",
            "Уточняйте по сорту, региону и способу выращивания" to "Check the variety, region and growing method",
            "Зависит от культуры и размера посадочного материала" to "Depends on the crop and planting material size",
            "Зависит от вида, температуры и влажности" to "Depends on the species, temperature and humidity",
            "Это общая карточка без сортовой нормы. Сверяйте сроки, глубину и условия выращивания с упаковкой семян и местными агрономическими рекомендациями." to "This is general guidance, not a variety-specific recommendation. Check sowing time, depth and growing conditions against the seed packet and local agronomic advice.",
            "📘 Бумажный вариант огородного дневника" to "📘 Printable garden planner",
            "Выберите разделы и период. PDF будет многостраничным, с переносом длинного текста и календарём по месяцам." to "Choose sections and a period. The PDF will span multiple pages, wrap long text and include a monthly calendar.",
            "Год календаря" to "Calendar year",
            "Период" to "Period",
            "Оформление" to "Appearance",
            "Сформировать и сохранить PDF" to "Generate and save PDF",
            "Что попадёт в документ" to "What's included in the document",
            "Календарь с датами и фазами Луны, рекомендации по месяцам, задачи с культурой/грядкой и статусом, доступные встроенные культуры и выбранные заметки. Длинные разделы переносятся на следующие страницы." to "Calendar dates and moon phases, monthly recommendations, tasks with crop/bed and status, available built-in crops, and selected notes. Long sections continue on following pages.",
            "Ваш помощник для сада и огорода" to "Your gardening companion",
            "📍 Место вашего огорода" to "📍 Your garden location",
            "Климатическая зона: " to "Climate zone: ",
            "Это стартовая база. Универсальных дат посева для всей России не существует." to "This is a starting guide. There are no universal sowing dates for all of Russia.",
            "Приложение будет готовить локальный план на текущий день. Интернет для календарных рекомендаций не нужен; точное время доставки зависит от ограничений Android." to "The app prepares a local plan for the current day. Calendar recommendations do not require internet access; exact delivery time depends on Android restrictions.",
            "Категории рекомендаций" to "Recommendation categories",
            "Выберите хотя бы одну категорию рекомендаций." to "Select at least one recommendation category.",
            "Выбрать культуры" to "Choose crops",
            "Сколько рекомендаций показывать" to "Number of recommendations to show",
            "Отправить тестовое уведомление" to "Send test notification",
            "← Назад" to "← Back",
            "📍 Сейчас выбрано" to "📍 Current selection",
            "Почему это важно" to "Why this matters",
            "Сроки посева зависят от климата, высоты участка, микроклимата и погоды. Региональные подсказки в приложении ориентировочные — перед высадкой проверяйте прогноз и температуру почвы." to "Sowing times depend on climate, elevation, microclimate and weather. Regional tips are approximate; check the forecast and soil temperature before planting.",
            "Выбрать город или регион вручную" to "Choose a city or region manually",
            "Определить местоположение по GPS" to "Detect location via GPS",
            "Вернуться на главный экран" to "Return to home screen",
            "Загружаю прогноз…" to "Loading forecast…",
            "Не удалось обновить прогноз. Показаны последние сохранённые данные." to "Could not refresh the forecast. Showing the last saved data.",
            "Прогноз погоды" to "Weather forecast",
            "Предупреждение" to "Warning",
            "Не удалось загрузить прогноз. Проверьте интернет-соединение и повторите попытку." to "Could not load the forecast. Check your internet connection and try again.",
            "↻ Повторить загрузку" to "↻ Retry loading",
            "Источник прогноза: 7Timer!" to "Forecast source: 7Timer!",
            "🌡️ Температура" to "🌡️ Temperature",
            "🌤️ Сегодня" to "🌤️ Today",
            "📅 Прогноз" to "📅 Forecast",
            "↻ Обновить прогноз" to "↻ Refresh forecast",
            "Пока задач нет. Добавьте первую задачу выше — она появится здесь." to "No tasks yet. Add your first task above and it will appear here.",
            "Здесь будут отображаться ваши собственные культуры." to "Your custom crops will appear here.",
            "Свои культуры не найдены. Введите название в форме посадки, чтобы выбрать встроенную культуру." to "No custom crops found. Enter a name in the planting form to choose a built-in crop.",
            "Записывайте сорт, место выращивания и заметки. Данные сохраняются на устройстве." to "Record the variety, growing location and notes. Data is saved on your device.",
            "Подходящие культуры" to "Suggested crops",
            "Старые записи" to "Older records",
            "Запись из прежней версии дневника." to "Entry from an older version of the diary.",
            "Отметить выполненной: " to "Mark as completed: ",
            "Удалить из истории: " to "Remove from history: ",
            "Неверное значение: \$key" to "Invalid value: \$key",
            "План огородника на " to "Gardener's plan for ",
            "Сформировано: " to "Generated: ",
            "Период: " to "Period: ",
            "Лунные обозначения — справочная подсказка. Учитывайте регион, погоду и температуру почвы." to "Lunar symbols are for reference only. Consider your region, weather and soil temperature.",
            "Содержание" to "Contents",
            "Сезонные рекомендации" to "Seasonal recommendations",
            "Страницы для записей" to "Blank notes pages",
            "Календарь — " to "Calendar — ",
            "Пн       Вт       Ср       Чт       Пт       Сб       Вс" to "Mo       Tu       We       Th       Fr       Sa       Su",
            "Фазы Луны даны как ориентир; сроки работ корректируйте по местным условиям." to "Moon phases are indicative; adjust work dates to local conditions.",
            "Заметки к календарю" to "Calendar notes",
            "Тип работы: " to "Task type: ",
            "Статус: " to "Status: ",
            "Запланировано" to "Planned",
            "На выбранный период задач нет." to "No tasks for the selected period.",
            "Группа: " to "Group: ",
            "Срок посева: " to "Sowing time: ",
            "Глубина: " to "Depth: ",
            "Всходы: " to "Germination: ",
            "Сохранённых заметок нет." to "No saved notes.",
            "Свободные записи " to "Blank notes ",
            "Весь год" to "Full year",
            "весь год" to "full year",
            "весна" to "spring",
            "лето" to "summer",
            "осень" to "autumn",
            "зима" to "winter",
            "Мой Посевной" to "My Sowing Calendar",
            "Мой Посевной — бумажный вариант" to "My Sowing Calendar — printable planner",
            "Условно благоприятный день" to "Tentatively favorable day",
            "Мои задачи" to "My tasks",
            "Фаза Луны" to "Moon phase",
            "Что делать в этом месяце" to "What to do this month",
            "Заменить данные приложения?" to "Replace app data?",
            "Добро пожаловать в «Мой Посевной»! 🌱" to "Welcome to My Sowing Calendar! 🌱",
            "Культуры для напоминаний" to "Crops for reminders",
            "Выберите город" to "Choose a city",
            "Местоположение пока недоступно" to "Location is not available yet",
            "Конфиденциальность" to "Privacy",
            "Очистить журнал урожая?" to "Clear the harvest log?",
            "Планируйте посевы и садовые работы, ведите заметки, добавляйте культуры, создавайте PDF и сохраняйте резервные копии. Ваши данные хранятся на устройстве." to "Plan sowing and garden work, keep notes, add crops, create PDFs and save backups. Your data is stored on your device.",
            "• Каталог расширен до всех встроенных культур с доступными значками.\n• Исправлены напоминания по личным заметкам и восстановлению резервной копии.\n• Удалён неиспользуемый аудиоплеер и его зависимости.\n• Обновлён источник прогноза погоды: 7Timer!." to "• The catalog now includes all built-in crops with available icons.\n• Reminders for personal notes and backup restoration have been fixed.\n• The unused audio player and its dependencies have been removed.\n• Weather forecast source updated: 7Timer!.",
            "Не удалось получить координаты. Включите геолокацию и попробуйте ещё раз. Если GPS пока не определил место, можно выбрать город вручную." to "Could not obtain coordinates. Enable location services and try again. If GPS has not determined your location, you can choose a city manually.",
            "Приложение не требует учётной записи и не содержит рекламы или аналитических трекеров. Задачи, заметки и выбранный город хранятся локально на устройстве. При запросе прогноза координаты выбранного места передаются сервису 7Timer!. Источник данных указан на экране прогноза.\n\nПолная политика конфиденциальности доступна в репозитории проекта." to "The app does not require an account and contains no ads or analytics trackers. Tasks, notes and the selected city are stored locally on the device. When a forecast is requested, the selected location's coordinates are sent to 7Timer!. The data source is shown on the forecast screen.\n\nThe full privacy policy is available in the project repository.",
            "Начать работу" to "Get started",
            "Понятно" to "Got it",
            "Настройки" to "Settings",
            "Открыть политику" to "Open policy",
            "Выбрать все" to "Select all",
            "Год" to "Year",
            "Поиск города или региона" to "Search city or region",
            "Например, проверить грядки" to "For example, inspect garden beds",
            "Культура (например, томат)" to "Crop (e.g. tomato)",
            "Включить ежедневные уведомления" to "Enable daily notifications",
            "Иконка Мой Посевной" to "My Sowing Calendar icon",
            "Весна (март–май)" to "Spring (March–May)",
            "Лето (июнь–август)" to "Summer (June–August)",
            "Осень (сентябрь–ноябрь)" to "Autumn (September–November)",
            "Зима (декабрь–февраль)" to "Winter (December–February)",
            "Цветной" to "Color",
            "Чёрно-белый, экономный" to "Black and white, ink-saving",
            "Сидераты" to "Green manure",
            "Ягоды и лианы" to "Berries and vines",
            "Плодовые кустарники" to "Fruit bushes",
            "Введите год от 2000 до 2100" to "Enter a year from 2000 to 2100",
            "Выберите хотя бы один раздел" to "Select at least one section",
            "Резервная копия сохранена" to "Backup saved",
            "Ошибка сохранения копии" to "Could not save the backup",
            "Восстановление заменит текущие настройки, задачи, заметки и каталог культур данными из резервной копии." to "Restoring will replace current settings, tasks, notes and crop catalog with data from the backup.",
            "Восстановить" to "Restore",
            "PDF сохранён" to "PDF saved",
            "Ошибка создания копии: " to "Backup creation error: ",
            "Не удалось открыть выбор файла" to "Could not open the file picker",
            "Копия восстановлена. Данные загружены." to "Backup restored. Data loaded.",
            "Тестовое уведомление отправлено, если разрешения Android включены." to "Test notification sent, provided Android notification permission is enabled.",
            "Каталог культур недоступен." to "Crop catalog is unavailable.",
            "Без разрешения на геолокацию выберите город вручную." to "Choose a city manually if you do not grant location permission.",
            "Не удалось получить геолокацию. Выберите город вручную." to "Could not get your location. Choose a city manually.",
            "Ошибка определения местоположения. Выберите город вручную." to "Location detection failed. Choose a city manually.",
            "Выбрать город" to "Choose city",
            "Сначала выберите город или определите местоположение." to "Choose a city or detect your location first.",
            "Задача добавлена" to "Task added",
            "Не удалось открыть браузер." to "Could not open the browser.",
            "• Каталог расширен до всех встроенных культур с доступными значками.\n• Исправлены напоминания по личным заметкам и восстановлению резервной копии.\n• Удалён неиспользуемый аудиоплеер и его зависимости.\n• Обновлён источник прогноза погоды: 7Timer!." to "• The catalog now includes all built-in crops with available icons.\n• Reminders for personal notes and backup restoration have been fixed.\n• The unused audio player and its dependencies have been removed.\n• Weather forecast source updated: 7Timer!.",
            "Приложение не требует учётной записи и не содержит рекламы или аналитических трекеров. Задачи, заметки и выбранный город хранятся локально на устройстве. При запросе прогноза координаты выбранного места передаются сервису 7Timer!. Источник данных указан на экране прогноза.\n\nПолная политика конфиденциальности доступна в репозитории проекта." to "The app does not require an account and contains no ads or analytics trackers. Tasks, notes and the selected city are stored locally on the device. When a forecast is requested, the selected location's coordinates are sent to 7Timer!. The data source is shown on the forecast screen.\n\nThe full privacy policy is available in the project repository.",
            "Да" to "Yes",
            "Нет" to "No"
        )
        exact[value]?.let { return it }
        if (value.contains("Климатическая зона: ") || value.contains("Нажмите, чтобы изменить место.")) {
            return value.replace("Климатическая зона: ", "Climate zone: ").replace("Нажмите, чтобы изменить место.", "Tap to change location.")
        }
        if (value.contains("\nВыполнено")) return value.replace("\nВыполнено", "\nCompleted")
        if (value.startsWith("Спасибо, что выбрали «Мой Посевной».")) {
            val version = value.substringAfter("\n\nВерсия ", "")
            return "Thank you for choosing My Sowing Calendar. May your efforts sprout well and bring a great harvest every year!" + if (version.isBlank()) "" else "\n\nVersion $version"
        }
        if (value.startsWith("Новолуние. Фаза вычислена приближённо.")) return "New moon. The phase is approximate. Lunar advice is traditional and does not replace agronomic recommendations."
        if (value.startsWith("Растущая Луна. Фаза вычислена приближённо.")) return "Waxing moon. The phase is approximate. Lunar advice is traditional and does not replace agronomic recommendations."
        if (value.startsWith("Полнолуние. Фаза вычислена приближённо.")) return "Full moon. The phase is approximate. Lunar advice is traditional and does not replace agronomic recommendations."
        if (value.startsWith("Убывающая Луна. Фаза вычислена приближённо.")) return "Waning moon. The phase is approximate. Lunar advice is traditional and does not replace agronomic recommendations."
        if (value.startsWith("До посадки: ")) return "Until planting: " + translateDayCount(value.removePrefix("До посадки: "))
        if (value.startsWith("До задачи: ")) return "Until task: " + translateDayCount(value.removePrefix("До задачи: "))
        if (value.startsWith("После даты посадки прошло ")) return "Time since planting: " + translateDayCount(value.removePrefix("После даты посадки прошло "))
        if (value.startsWith("Просрочено на ")) return "Overdue by " + translateDayCount(value.removePrefix("Просрочено на "))
        if (value.endsWith(" пункта") && value.removeSuffix(" пункта").toIntOrNull() != null) return value.removeSuffix(" пункта") + " items"
        if (value.startsWith("Ошибка подготовки PDF: ")) return "PDF preparation error: " + value.removePrefix("Ошибка подготовки PDF: ")
        if (value.startsWith("Ошибка сохранения: ")) return "Save error: " + value.removePrefix("Ошибка сохранения: ")
        if (value.startsWith("Ошибка создания копии: ")) return "Backup creation error: " + value.removePrefix("Ошибка создания копии: ")
        if (value.startsWith("Место сохранено: ")) return "Location saved: " + tr(value.removePrefix("Место сохранено: "))
        if (value.startsWith("Что нового · версия ")) return "What's new · version " + value.removePrefix("Что нового · версия ")
        if (value.startsWith("Запланировать на ")) return "Schedule for " + value.removePrefix("Запланировать на ")
        if (value.startsWith("Время: ")) return "Time: " + value.removePrefix("Время: ")
        if (value.startsWith("Иконка культуры ")) return "Crop icon " + tr(value.removePrefix("Иконка культуры "))
        if (value.startsWith("Посевной календарь · ")) return "Sowing calendar · " + value.removePrefix("Посевной календарь · ")
        if (value.startsWith("План огородника на ")) return "Gardener's plan for " + value.removePrefix("План огородника на ")
        if (value.startsWith("Сформировано: ")) return "Generated: " + value.removePrefix("Сформировано: ")
        if (value.startsWith("Период: ")) return "Period: " + tr(value.removePrefix("Период: "))
        if (value.startsWith("Календарь — ")) return "Calendar — " + value.removePrefix("Календарь — ")
        if (value.startsWith("Свободные записи ")) return "Blank notes " + value.removePrefix("Свободные записи ")
        if (value.startsWith("Прогноз на ")) return "Forecast for " + value.removePrefix("Прогноз на ")
        if (value.startsWith("Раздел: ")) return "Section: " + tr(value.removePrefix("Раздел: "))
        if (value.startsWith("Климатическая зона: ")) return "Climate zone: " + tr(value.removePrefix("Климатическая зона: "))
        if (value.startsWith("Регион: ")) return "Region: " + tr(value.removePrefix("Регион: "))
        if (value.startsWith("Группа: ")) return "Group: " + tr(value.removePrefix("Группа: "))
        if (value.startsWith("Сроки посева: ")) return "Sowing time: " + tr(value.removePrefix("Сроки посева: "))
        if (value.startsWith("Глубина: ")) return "Depth: " + tr(value.removePrefix("Глубина: "))
        if (value.startsWith("Всходы: ")) return "Germination: " + tr(value.removePrefix("Всходы: "))
        if (value.startsWith("Советы: ")) return "Tips: " + tr(value.removePrefix("Советы: "))
        if (value.startsWith("Тип работы: ")) return "Task type: " + tr(value.removePrefix("Тип работы: "))
        if (value.startsWith("Статус: ")) return "Status: " + tr(value.removePrefix("Статус: "))
        if (value.startsWith("Культура/грядка: ")) return "Crop/bed: " + tr(value.removePrefix("Культура/грядка: "))
        if (value.startsWith("Отметить выполненной: ")) return "Mark as completed: " + tr(value.removePrefix("Отметить выполненной: "))
        if (value.startsWith("Удалить из истории: ")) return "Remove from history: " + tr(value.removePrefix("Удалить из истории: "))
        if (value.startsWith("Возраст: ")) return "Age: " + value.removePrefix("Возраст: ").replace(" дн.", " days")
        if (value.startsWith("До посева: ")) return "Until sowing: " + value.removePrefix("До посева: ").replace(" дн.", " days")
        if (value.startsWith("Посев: ")) return "Sowing: " + value.removePrefix("Посев: ")
        if (value.startsWith("Сорт: ")) return "Variety: " + value.removePrefix("Сорт: ")
        if (value.startsWith("Место: ")) return "Location: " + value.removePrefix("Место: ")
        if (value.startsWith("Заметки: ")) return "Notes: " + value.removePrefix("Заметки: ")
        if (value.startsWith("Сбор: ")) return "Harvest: " + value.removePrefix("Сбор: ")
        if (value.startsWith("Количество: ")) return "Amount: " + value.removePrefix("Количество: ")
        if (value.startsWith("Примечание: ")) return "Comment: " + value.removePrefix("Примечание: ")
        if (value.startsWith("Удалить посадку: ")) return "Delete planting: " + tr(value.removePrefix("Удалить посадку: "))
        return value
    }

    private fun translateDayCount(value: String): String = value
        .replace(" дней", " days")
        .replace(" дня", " days")
        .replace(" день", " day")

    private fun txt(value:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply {
        text=tr(value); textSize=size; setTextColor(Color.rgb(35,48,38))
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
        setPadding(0,dp(7),0,dp(7))
    }
    private fun button(label:String, action:()->Unit) {
        val b = Button(this).apply {
            text=tr(label)
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
            android.app.AlertDialog.Builder(this).setTitle(tr("Добро пожаловать в «Мой Посевной»! 🌱"))
                .setMessage(tr("Планируйте посевы и садовые работы, ведите заметки, добавляйте культуры, создавайте PDF и сохраняйте резервные копии. Ваши данные хранятся на устройстве."))
                .setPositiveButton(tr("Начать работу"), null).show()
            return
        }
        if (prefs.getString("last_seen_version", "") != BuildConfig.VERSION_NAME) {
            prefs.edit().putString("last_seen_version", BuildConfig.VERSION_NAME).apply()
            showUpdateNotes(false)
        }
    }

    private fun showUpdateNotes(manual: Boolean) {
        android.app.AlertDialog.Builder(this).setTitle(tr("Что нового · версия ") + BuildConfig.VERSION_NAME)
            .setMessage(tr("• Каталог расширен до всех встроенных культур с доступными значками.\n• Исправлены напоминания по личным заметкам и восстановлению резервной копии.\n• Удалён неиспользуемый аудиоплеер и его зависимости.\n• Обновлён источник прогноза погоды: 7Timer!."))
            .setPositiveButton(tr("Понятно"), null).show()
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
        } catch (e: Exception) { Toast.makeText(this, tr("Ошибка создания копии: ") + (e.message ?: "ошибка"), Toast.LENGTH_LONG).show() }
    }
    private fun restoreBackup() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { startActivityForResult(intent, 2402) }
        catch (_: Exception) { Toast.makeText(this, tr("Не удалось открыть выбор файла"), Toast.LENGTH_LONG).show() }
    }

    private fun importBackup(uri: Uri) {
        try {
            val jsonText = contentResolver.openInputStream(uri)?.use { input ->
                java.util.zip.ZipInputStream(input).use { zip ->
                    val entry = zip.nextEntry
                        ?: throw IllegalArgumentException("Архив пуст")
                    // Backups created by the app contain exactly one entry. Reject
                    // unexpected entries instead of decompressing arbitrary ZIP content.
                    if (entry.name != "backup.json" || entry.isDirectory) {
                        throw IllegalArgumentException("Неверная структура резервной копии")
                    }
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
                    if (zip.nextEntry != null) {
                        throw IllegalArgumentException("В архиве есть лишние файлы")
                    }
                    output.toString(Charsets.UTF_8.name())
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
                    "s" -> {
                        if (value !is String) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value))
                    }
                    "b" -> {
                        if (value !is Boolean) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to value))
                    }
                    "i" -> {
                        if (value !is Int && value !is Long) throw IllegalArgumentException("Неверное значение: $key")
                        val number = (value as Number).toLong()
                        if (number !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to number.toInt()))
                    }
                    "l" -> {
                        if (value !is Int && value !is Long) throw IllegalArgumentException("Неверное значение: $key")
                        parsed.add(key to (type to (value as Number).toLong()))
                    }
                    "f" -> {
                        if (value !is Number || !value.toDouble().isFinite() || value.toDouble() !in -Float.MAX_VALUE.toDouble()..Float.MAX_VALUE.toDouble()) {
                            throw IllegalArgumentException("Неверное значение: $key")
                        }
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

            // Snapshot current preferences so a failed write can attempt to restore them.
            val previous = prefs.all.toMap()
            fun writePreferences(valuesToWrite: Map<String, Any?>): Boolean {
                val editor = prefs.edit().clear()
                valuesToWrite.forEach { (key, value) ->
                    when (value) {
                        is String -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Float -> editor.putFloat(key, value)
                        is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                        null -> editor.remove(key)
                        else -> throw IllegalArgumentException("Неподдерживаемый тип данных: $key")
                    }
                }
                return editor.commit()
            }
            val restored = parsed.associate { (key, pair) -> key to pair.second }
            if (!writePreferences(restored)) {
                val rollbackSucceeded = runCatching { writePreferences(previous) }.getOrDefault(false)
                throw IllegalStateException(
                    if (rollbackSucceeded) "Не удалось сохранить копию; исходные данные восстановлены"
                    else "Ошибка сохранения копии и отката. Проверьте данные приложения"
                )
            }
            migrateLegacyNoteKeys()
            scheduleDailyReminder()
            Toast.makeText(this, tr("Копия восстановлена. Данные загружены."), Toast.LENGTH_LONG).show()
            showHome()
        } catch (e: Exception) {
            Toast.makeText(this, if (isEnglish()) "Could not restore backup: ${e.message ?: "invalid archive"}. Check the app data before continuing." else "Не удалось восстановить копию: ${e.message ?: "неверный архив"}. Проверьте данные приложения перед продолжением.", Toast.LENGTH_LONG).show()
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
        card("🌦️ Погода на 7 дней","Прогноз по дням: температура, облачность, осадки и ветер.",{loadWeather()})
        card("🪴 Полезный принцип","Посевные сроки — ориентир, а не гарантия. Сверяйте рекомендации с прогнозом заморозков, температурой почвы и условиями конкретной грядки.")
        card("🌙 Лунный календарь",lunarSummary(Calendar.getInstance()),{showCalendar()})
        card("📅 Выбрать дату","Откройте календарь, чтобы посмотреть сезонные подсказки и заметки.",{showCalendar()})
        card("🌱 Каталог культур","Овощи, зелень, ягоды и цветы — сроки, глубина посева и советы.",{showPlants("")})
        card("🔔 Напоминания",reminderSettingsSummary(),{showReminderSettings()})
        card("ℹ️ Важно","Это стартовая база. Универсальных дат посева для всей России не существует.")
    }


    private val reminderCategoryOptions = listOf(
        "Сезонные работы", "Посев и рассада", "Уход и полив", "Подкормка",
        "Защита растений", "Сбор урожая", "Лунная фаза", "Мои задачи", "Мои заметки"
    )

    private fun defaultReminderCategories(): Set<String> = reminderCategoryOptions.toSet()

    private fun reminderSettingsSummary(): String {
        val enabled = prefs.getBoolean("reminder_enabled", false)
        val hour = prefs.getInt("reminder_hour", 9).coerceIn(0, 23)
        val minute = prefs.getInt("reminder_minute", 0).coerceIn(0, 59)
        val selected = prefs.getStringSet("reminder_categories", null) ?: defaultReminderCategories()
        val count = prefs.getStringSet("reminder_crop_ids", null)?.size ?: cropIconCatalog.length()
        return if (enabled) "Каждый день примерно в %02d:%02d · категорий: %d · культур: %d. Нажмите, чтобы настроить."
            .format(Locale.US, hour, minute, selected.size, count)
        else "Ежедневные уведомления выключены. Нажмите, чтобы настроить."
    }

    private fun showReminderSettings() {
        baseLayout("Ежедневные напоминания")
        content.addView(txt("Приложение будет готовить локальный план на текущий день. Интернет для календарных рекомендаций не нужен; точное время доставки зависит от ограничений Android.", 14f))
        val enabled = CheckBox(this).apply {
            text = tr("Включить ежедневные уведомления")
            isChecked = prefs.getBoolean("reminder_enabled", false)
            textSize = 16f
        }
        content.addView(enabled)
        var hour = prefs.getInt("reminder_hour", 9).coerceIn(0, 23)
        var minute = prefs.getInt("reminder_minute", 0).coerceIn(0, 59)
        val timeButton = Button(this).apply {
            text = tr("Время: %02d:%02d").format(Locale.US, hour, minute)
            isAllCaps = false
            setOnClickListener {
                android.app.TimePickerDialog(this@MainActivity, { _, h, m ->
                    hour = h; minute = m
                    text = tr("Время: %02d:%02d").format(Locale.US, hour, minute)
                }, hour, minute, true).show()
            }
        }
        content.addView(timeButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })

        content.addView(txt("Категории рекомендаций", 17f, true))
        val savedCategories = prefs.getStringSet("reminder_categories", null) ?: defaultReminderCategories()
        val categoryChecks = linkedMapOf<String, CheckBox>()
        reminderCategoryOptions.forEach { category ->
            val check = CheckBox(this).apply {
                text = category
                isChecked = category in savedCategories
            }
            categoryChecks[category] = check
            content.addView(check)
        }

        val cropCount = prefs.getStringSet("reminder_crop_ids", null)?.size ?: cropIconCatalog.length()
        val cropSummary = txt("Выбрано культур: $cropCount из ${cropIconCatalog.length()}", 14f)
        content.addView(cropSummary)
        button("Выбрать культуры") {
            showReminderCropPicker {
                cropSummary.text = if (isEnglish()) "Selected crops: ${prefs.getStringSet("reminder_crop_ids", emptySet())?.size ?: cropIconCatalog.length()} of ${cropIconCatalog.length()}" else "Выбрано культур: ${prefs.getStringSet("reminder_crop_ids", emptySet())?.size ?: cropIconCatalog.length()} из ${cropIconCatalog.length()}"
            }
        }

        content.addView(txt("Сколько рекомендаций показывать", 17f, true))
        val limitSpinner = Spinner(this)
        val limits = listOf(2, 3, 4, 5, 6)
        limitSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, limits.map { "$it пункта" })
        limitSpinner.setSelection((limits.indexOf(prefs.getInt("reminder_limit", 4))).coerceAtLeast(0))
        content.addView(limitSpinner)

        button("Сохранить настройки") {
            val categories = categoryChecks.filterValues { it.isChecked }.keys.toSet()
            val wantsEnabled = enabled.isChecked
            if (wantsEnabled && categories.isEmpty()) {
                Toast.makeText(this, tr("Выберите хотя бы одну категорию рекомендаций."), Toast.LENGTH_LONG).show()
                return@button
            }
            val needsPermission = wantsEnabled && Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            prefs.edit()
                .putBoolean("reminder_enabled", wantsEnabled && !needsPermission)
                .putBoolean("reminder_pending_enable", needsPermission)
                .putInt("reminder_hour", hour)
                .putInt("reminder_minute", minute)
                .putStringSet("reminder_categories", categories)
                .putInt("reminder_limit", limits[limitSpinner.selectedItemPosition])
                .apply()
            scheduleDailyReminder()
            if (needsPermission) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                return@button
            }
            Toast.makeText(this, if (wantsEnabled) "Настройки напоминаний сохранены" else "Напоминания выключены", Toast.LENGTH_SHORT).show()
            showHome()
        }
        button("Отправить тестовое уведомление") {
            sendBroadcast(Intent(this, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_TEST))
            Toast.makeText(this, tr("Тестовое уведомление отправлено, если разрешения Android включены."), Toast.LENGTH_LONG).show()
        }
        button("← Назад") { showHome() }
    }

    private fun showReminderCropPicker(onSaved: () -> Unit) {
        val options = mutableListOf<Pair<String, String>>()
        for (i in 0 until cropIconCatalog.length()) {
            val crop = cropIconCatalog.optJSONObject(i) ?: continue
            if (!crop.optBoolean("available", true)) continue
            val id = crop.optString("id")
            val name = crop.optString("name")
            if (id.isNotBlank() && name.isNotBlank()) options.add(id to name)
        }
        val customCrops = userCrops()
        for (i in 0 until customCrops.length()) {
            val crop = customCrops.optJSONObject(i) ?: continue
            val id = crop.optString("id")
            val name = crop.optString("name")
            if (id.isNotBlank() && name.isNotBlank() && options.none { it.first == id }) options.add(id to name)
        }
        if (options.isEmpty()) {
            Toast.makeText(this, tr("Каталог культур недоступен."), Toast.LENGTH_SHORT).show()
            return
        }
        val stored = prefs.getStringSet("reminder_crop_ids", null)
        val selected = (stored ?: options.map { it.first }.toSet()).toMutableSet()
        val checked = BooleanArray(options.size) { options[it].first in selected }
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Культуры для напоминаний"))
            .setMultiChoiceItems(options.map { it.second }.toTypedArray(), checked) { _, which, isChecked ->
                if (isChecked) selected.add(options[which].first) else selected.remove(options[which].first)
            }
            .setPositiveButton(tr("Сохранить")) { _, _ ->
                prefs.edit().putStringSet("reminder_crop_ids", selected.toSet()).apply()
                onSaved()
            }
            .setNeutralButton(tr("Выбрать все")) { _, _ ->
                prefs.edit().putStringSet("reminder_crop_ids", options.map { it.first }.toSet()).apply()
                onSaved()
            }
            .setNegativeButton(tr("Отмена"), null)
            .show()
    }

    private fun showAbout() {
        baseLayout("О программе")
        content.addView(txt("Версия ${BuildConfig.VERSION_NAME}", 16f, true))
        content.addView(txt("Номер сборки: ${BuildConfig.VERSION_CODE}", 13f))
        content.addView(txt("Календарь с индикаторами дней, собственные культуры, PDF с заметками и резервное копирование.", 14f))
        button("Язык / Language") {
            val labels = arrayOf("Русский", "English")
            android.app.AlertDialog.Builder(this)
                .setTitle(tr("Выберите язык приложения"))
                .setItems(labels) { _, which ->
                    prefs.edit().putString("app_language", if (which == 1) "en" else "ru").apply()
                    showAbout()
                }
                .setNegativeButton(tr("Отмена"), null)
                .show()
        }
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
            contentDescription = tr("Иконка Мой Посевной")
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        hero.addView(logo, LinearLayout.LayoutParams(dp(124), dp(124)))
        hero.addView(TextView(this).apply {
            text = tr("Мой Посевной")
            textSize = 27f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(3))
        })
        hero.addView(TextView(this).apply {
            text = tr("Ваш помощник в саду и огороде")
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
                Toast.makeText(this, tr("Не удалось открыть страницу поддержки"), Toast.LENGTH_LONG).show()
            }
        }
        button("Политика конфиденциальности") { showPrivacyPolicy() }
        button("← На главный экран") { showHome() }
        content.addView(TextView(this).apply {
            text = tr("Спасибо, что выбрали «Мой Посевной». Пусть ваши труды дают добрые всходы, а урожай радует каждый год!\n\nВерсия ${BuildConfig.VERSION_NAME}")
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
        val search = EditText(this).apply { hint=tr("Поиск города или региона"); setSingleLine(true) }
        val list = ListView(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            cities.sortedBy { it.city }.map { it.city + " — " + it.region + " · " + it.zone })
        list.adapter = adapter
        wrap.addView(search)
        wrap.addView(list,LinearLayout.LayoutParams(-1,520))
        val dialog = android.app.AlertDialog.Builder(this).setTitle(tr("Выберите город")).setView(wrap).setNegativeButton(tr("Отмена"),null).create()
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
        Toast.makeText(this,tr("Место сохранено: $city"),Toast.LENGTH_SHORT).show()
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
        if (requestCode == 1001) {
            val granted = grantResults.any { it == PackageManager.PERMISSION_GRANTED }
            val wasPending = prefs.getBoolean("reminder_pending_enable", false)
            prefs.edit().putBoolean("reminder_pending_enable", false)
                .putBoolean("reminder_enabled", granted && wasPending).apply()
            scheduleDailyReminder()
            Toast.makeText(this, if (granted && wasPending) "Ежедневные напоминания включены." else "Уведомления не разрешены. Напоминания оставлены выключенными.", Toast.LENGTH_LONG).show()
        }
        if(requestCode == 2002) {
            if(grantResults.any { it == PackageManager.PERMISSION_GRANTED }) resolveGardenLocation()
            else Toast.makeText(this,tr("Без разрешения на геолокацию выберите город вручную."),Toast.LENGTH_LONG).show()
        }
    }

    private fun resolveGardenLocation() {
        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val lastKnown = manager.getProviders(true).mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val hasFineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val providers = if (hasFineLocation) {
                    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                } else {
                    listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                }
                val provider = providers.firstOrNull { name -> try { manager.isProviderEnabled(name) } catch (_: Exception) { false } }
                if (provider != null) {
                    manager.getCurrentLocation(provider, null, mainExecutor) { fresh ->
                        val location = fresh ?: lastKnown
                        if (location != null) resolveGardenLocationName(location) else showLocationUnavailable()
                    }
                    return
                }
            }
            if (lastKnown != null) resolveGardenLocationName(lastKnown) else showLocationUnavailable()
        } catch (_: SecurityException) {
            Toast.makeText(this,tr("Не удалось получить геолокацию. Выберите город вручную."),Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this,tr("Ошибка определения местоположения. Выберите город вручную."),Toast.LENGTH_LONG).show()
        }
    }

    private fun showLocationUnavailable() {
        runOnUiThread {
            android.app.AlertDialog.Builder(this).setTitle(tr("Местоположение пока недоступно"))
                .setMessage(tr("Не удалось получить координаты. Включите геолокацию и попробуйте ещё раз. Если GPS пока не определил место, можно выбрать город вручную."))
                .setPositiveButton(tr("Настройки")) { _,_ -> startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                .setNegativeButton(tr("Выбрать город")) { _,_ -> showCityPicker() }.show()
        }
    }

    private fun resolveGardenLocationName(location: Location) {
        Thread {
            val address = try {
                if (Geocoder.isPresent()) Geocoder(this, Locale("ru")).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                else null
            } catch (_: Exception) { null }
            runOnUiThread {
                val lat = location.latitude
                val lon = location.longitude
                val isCrimea = lat in 44.2..46.3 && lon in 32.3..36.9
                val nearest = cities.minByOrNull { distanceKm(lat, lon, it.latitude, it.longitude) }
                val nearestDistance = nearest?.let { distanceKm(lat, lon, it.latitude, it.longitude) } ?: Double.MAX_VALUE
                val geocodedCity = listOf(address?.locality, address?.subLocality)
                    .firstOrNull { !it.isNullOrBlank() }?.trim()
                val locationLabel: String
                val region: String
                val zone: String
                if (isCrimea) {
                    val city = geocodedCity
                        ?: nearest?.takeIf { it.region == "Крым" || it.region == "Севастополь" }?.takeIf { nearestDistance <= 45.0 }?.city
                        ?: "Моё местоположение"
                    val isSevastopol = address?.adminArea?.contains("Севастополь", true) == true ||
                        (nearest?.region == "Севастополь" && nearestDistance <= 18.0) ||
                        city.contains("Севастополь", true)
                    locationLabel = city
                    region = if (isSevastopol) "Севастополь" else "Крым"
                    zone = "Юг"
                } else {
                    locationLabel = geocodedCity ?: nearest?.takeIf { nearestDistance <= 180.0 }?.city ?: "Моё местоположение"
                    region = address?.adminArea?.takeIf { it.isNotBlank() }
                        ?: nearest?.takeIf { nearestDistance <= 180.0 }?.region
                        ?: "Определено по геолокации"
                    zone = nearest?.takeIf { nearestDistance <= 180.0 }?.zone ?: when {
                        lat >= 63 -> "Север"
                        lat >= 57 -> "Сибирь"
                        lat <= 48 -> "Юг"
                        else -> "Центральный"
                    }
                }
                saveGardenLocation(locationLabel, region, zone, lat, lon)
                showHome()
            }
        }.start()
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
            Toast.makeText(this, tr("Сначала выберите город или определите местоположение."), Toast.LENGTH_LONG).show()
            showLocationSettings()
            return
        }
        loadWeatherForecast(lat, lon)
    }

    override fun onBackPressed() {
        if (weatherForecastOpen) {
            weatherForecastOpen = false
            showHome()
            return
        }
        super.onBackPressed()
    }

    private fun loadWeatherForecast(lat: Double, lon: Double) {
        weatherForecastOpen = true
        val city = prefs.getString("garden_city", "Выбранное место") ?: "Выбранное место"
        baseLayout("Погода на 7 дней")
        content.addView(txt("📍 " + city, 17f, true))
        content.addView(txt("Прогноз погоды на 7 дней.", 13f))
        button("Источник данных: 7Timer!") { openExternalLink("https://www.7timer.info/doc.php?lang=en") }
        button("← На главную") {
            weatherForecastOpen = false
            showHome()
        }

        val cachedSummary = prefs.getString("weather_summary_7timer", null)
        val cacheExpires = prefs.getLong("weather_summary_7timer_expires", 0L)
        val cachedLat = prefs.getString("weather_summary_7timer_lat", null)?.toDoubleOrNull()
        val cachedLon = prefs.getString("weather_summary_7timer_lon", null)?.toDoubleOrNull()
        if (!cachedSummary.isNullOrBlank() && cachedLat == lat && cachedLon == lon &&
            System.currentTimeMillis() < cacheExpires) {
            renderWeatherForecast(city, cachedSummary, lat, lon)
            return
        }

        content.addView(txt("Загружаю прогноз…", 15f))
        Thread {
            var connection: java.net.HttpURLConnection? = null
            try {
                val url = String.format(
                    Locale.US,
                    "https://www.7timer.info/bin/api.pl?lon=%.3f&lat=%.3f&product=civil&output=json",
                    lon, lat
                )
                connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "MoiPosevnoy/1.3 (https://github.com/goodrawru/myposev)")
                val status = connection.responseCode
                if (status !in 200..299) throw java.io.IOException("HTTP " + status)
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                connection = null

                val root = JSONObject(body)
                val series = root.optJSONArray("dataseries") ?: throw java.io.IOException("Нет данных прогноза")
                val initParser = SimpleDateFormat("yyyyMMddHH", Locale.US).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }
                val initDate = initParser.parse(root.optString("init"))
                    ?: throw java.io.IOException("Не удалось определить время прогноза")
                val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = java.util.TimeZone.getDefault() }
                val displayFormat = SimpleDateFormat("EEE, d MMM", Locale("ru")).apply { timeZone = java.util.TimeZone.getDefault() }
                val grouped = linkedMapOf<String, MutableList<JSONObject>>()
                for (i in 0 until series.length()) {
                    val item = series.optJSONObject(i) ?: continue
                    val timepoint = item.optInt("timepoint", -1)
                    if (timepoint < 0) continue
                    val forecastDate = Date(initDate.time + timepoint * 60L * 60L * 1000L)
                    val key = dayFormat.format(forecastDate)
                    if (!grouped.containsKey(key)) grouped[key] = mutableListOf()
                    grouped[key]!!.add(item)
                }

                val result = mutableListOf<String>()
                var frost = false
                for ((dateText, items) in grouped.entries.take(7)) {
                    var minTemp = Double.POSITIVE_INFINITY
                    var maxTemp = Double.NEGATIVE_INFINITY
                    var maxWindCode = 0
                    var windDirection = ""
                    val weatherCounts = linkedMapOf<String, Int>()
                    val precipTypes = linkedSetOf<String>()
                    var parsedDate: Date? = null

                    for (item in items) {
                        val timepoint = item.optInt("timepoint", 0)
                        if (parsedDate == null) parsedDate = Date(initDate.time + timepoint * 60L * 60L * 1000L)
                        val temp = item.optDouble("temp2m", Double.NaN)
                        if (temp.isFinite()) {
                            minTemp = minOf(minTemp, temp)
                            maxTemp = maxOf(maxTemp, temp)
                        }
                        val wind = item.optJSONObject("wind10m")
                        val windCode = wind?.optInt("speed", 0) ?: 0
                        if (windCode > maxWindCode) {
                            maxWindCode = windCode
                            windDirection = wind?.optString("direction", "").orEmpty()
                        }
                        val weather = item.optString("weather", "cloudy")
                        weatherCounts[weather] = (weatherCounts[weather] ?: 0) + 1
                        when (item.optString("prec_type", "none").lowercase(Locale.US)) {
                            "rain" -> precipTypes.add("дождь")
                            "snow" -> precipTypes.add("снег")
                            "freezing rain" -> precipTypes.add("ледяной дождь")
                        }
                    }

                    if (parsedDate == null || !minTemp.isFinite() || !maxTemp.isFinite()) continue
                    if (minTemp <= 0.0) frost = true
                    val labelDate = try { dayFormat.parse(dateText) } catch (_: Exception) { null }
                    val label = (labelDate?.let { displayFormat.format(it) } ?: dateText)
                        .replaceFirstChar { it.uppercase(Locale("ru")) }
                    val mainWeather = weatherCounts.maxByOrNull { it.value }?.key ?: "cloudy"
                    val windText = if (maxWindCode > 0) {
                        "💨 ветер " + windDirectionRu(windDirection) + ", " + windSpeedRange(maxWindCode)
                    } else "💨 ветер: нет данных"
                    val precipText = if (precipTypes.isEmpty()) "Осадки: не ожидаются" else "Осадки: " + precipTypes.joinToString(", ")
                    result.add(label + "\n" + sevenTimerWeatherRu(mainWeather) + "\n" +
                        "🌡 " + minTemp.toInt() + "…"+ maxTemp.toInt() + " °C\n" +
                        windText + "\n" + precipText)
                }
                if (result.isEmpty()) throw java.io.IOException("Пустой прогноз")
                val summary = result.joinToString("\n\n") + if (frost) {
                    "\n\n⚠️ В один или несколько дней возможна температура 0 °C или ниже."
                } else ""
                val cacheUntil = System.currentTimeMillis() + 60 * 60 * 1000L
                prefs.edit()
                    .putString("weather_summary_7timer", summary)
                    .putLong("weather_summary_7timer_expires", cacheUntil)
                    .putLong("weather_summary_7timer_fetched_at", System.currentTimeMillis())
                    .putString("weather_summary_7timer_lat", lat.toString())
                    .putString("weather_summary_7timer_lon", lon.toString())
                    .apply()
                runOnUiThread {
                    if (!isFinishing && weatherForecastOpen) renderWeatherForecast(city, summary, lat, lon)
                }
            } catch (e: Exception) {
                connection?.disconnect()
                runOnUiThread {
                    if (!isFinishing && weatherForecastOpen) {
                        content.removeAllViews()
                        val savedForecast = prefs.getString("weather_summary_7timer", null)
                        val savedLat = prefs.getString("weather_summary_7timer_lat", null)?.toDoubleOrNull()
                        val savedLon = prefs.getString("weather_summary_7timer_lon", null)?.toDoubleOrNull()
                        if (!savedForecast.isNullOrBlank() && savedLat == lat && savedLon == lon) {
                            content.addView(txt("Не удалось обновить прогноз. Показаны последние сохранённые данные.", 15f, true))
                            savedForecast.split("\n\n").forEachIndexed { index, part ->
                                card(if (index < 7) "Прогноз погоды" else "Предупреждение", part)
                            }
                        } else {
                            content.addView(txt("Не удалось загрузить прогноз. Проверьте интернет-соединение и повторите попытку.", 15f))
                        }
                        button("↻ Повторить загрузку") { loadWeatherForecast(lat, lon) }
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
        baseLayout("Погода на 7 дней")
        content.addView(txt("📍 " + city, 17f, true))
        content.addView(txt("Источник прогноза: 7Timer!", 12f))
        val fetchedAt = prefs.getLong("weather_summary_7timer_fetched_at", 0L)
        if (fetchedAt > 0L) {
            val fetchedText = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(fetchedAt))
            content.addView(txt("Обновлено: $fetchedText", 12f))
        }
        button("Источник данных: 7Timer!") { openExternalLink("https://www.7timer.info/doc.php?lang=en") }
        val parts = summary.split("\n\n")
        parts.forEachIndexed { index, part ->
            if (part.startsWith("⚠️")) {
                card("🌡️ Температура", part)
            } else {
                card(if (index == 0) "🌤️ Сегодня" else "📅 Прогноз", part)
            }
        }
        button("↻ Обновить прогноз") { loadWeatherForecast(lat, lon) }
        button("← На главную") {
            weatherForecastOpen = false
            showHome()
        }
    }

    private fun sevenTimerWeatherRu(code: String): String {
        val c = code.lowercase(Locale.US)
        return when {
            c.startsWith("ts") || c.contains("tstorm") || c.contains("thunder") -> "⛈️ Гроза"
            c.contains("rainsnow") -> "🌨️ Мокрый снег"
            c.contains("snow") -> "🌨️ Снег"
            c.contains("rain") || c.contains("shower") -> "🌧️ Дождь"
            c.contains("fog") -> "🌫️ Туман"
            c.contains("clear") -> "☀️ Ясно"
            c.contains("pcloudy") -> "⛅ Переменная облачность"
            c.contains("mcloudy") -> "🌥️ Облачно с прояснениями"
            c.contains("cloudy") -> "☁️ Облачно"
            c.contains("humid") -> "💧 Высокая влажность"
            else -> "🌥️ Переменная облачность"
        }
    }

    private fun windDirectionRu(direction: String): String = when (direction.uppercase(Locale.US)) {
        "N" -> "северный"; "NNE" -> "северо-северо-восточный"; "NE" -> "северо-восточный"
        "ENE" -> "восточно-северо-восточный"; "E" -> "восточный"; "ESE" -> "восточно-юго-восточный"
        "SE" -> "юго-восточный"; "SSE" -> "юго-юго-восточный"; "S" -> "южный"
        "SSW" -> "юго-юго-западный"; "SW" -> "юго-западный"; "WSW" -> "западно-юго-западный"
        "W" -> "западный"; "WNW" -> "западно-северо-западный"; "NW" -> "северо-западный"
        "NNW" -> "северо-северо-западный"; else -> "направление не определено"
    }

    private fun windSpeedRange(code: Int): String = when (code.coerceIn(1, 8)) {
        1 -> "до 0,3 м/с"; 2 -> "0,3–3,4 м/с"; 3 -> "3,4–8 м/с"
        4 -> "8–10,8 м/с"; 5 -> "10,8–17,2 м/с"; 6 -> "17,2–24,5 м/с"
        7 -> "24,5–32,6 м/с"; else -> "свыше 32,6 м/с"
    }

    private fun showPrivacyPolicy() {
        android.app.AlertDialog.Builder(this)
            .setTitle(tr("Конфиденциальность"))
            .setMessage(tr("Приложение не требует учётной записи и не содержит рекламы или аналитических трекеров. Задачи, заметки и выбранный город хранятся локально на устройстве. При запросе прогноза координаты выбранного места передаются сервису 7Timer!. Источник данных указан на экране прогноза.\n\nПолная политика конфиденциальности доступна в репозитории проекта."))
            .setPositiveButton(tr("Открыть политику")) { _, _ ->
                openExternalLink("https://github.com/goodrawru/myposev/blob/rustore/PRIVACY_POLICY.md")
            }
            .setNegativeButton(tr("Закрыть"), null)
            .show()
    }

    private fun showTasks() {
        baseLayout("Задачи по уходу")
        content.addView(txt("Добавляйте планы и отмечайте выполненное. Для посева и пересадки показываем срок до даты или возраст посадки. Все даты указаны в формате день.месяц.год.",14f))
        val titleInput=EditText(this).apply { hint=tr("Задача (например, посеять томаты)"); setSingleLine(true) }
        val cropInput=EditText(this).apply { hint=tr("Культура или грядка (необязательно)"); setSingleLine(true) }
        val dateInput=EditText(this).apply {
            hint=tr("Дата (дд.мм.гггг)"); setSingleLine(true); isFocusable=false; isClickable=true
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
        val taskTypes = listOf("Уход","Посев","Полив","Подкормка","Прополка","Пересадка","Защита растений","Сбор урожая")
        val type=Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,taskTypes.map { tr(it) })
        }
        content.addView(titleInput); content.addView(cropInput); content.addView(dateInput); content.addView(type)
        button("Добавить задачу") {
            val title=titleInput.text.toString().trim()
            val date=parseTaskDate(dateInput.text.toString().trim())
            if(title.isBlank()) { Toast.makeText(this,tr("Введите название задачи"),Toast.LENGTH_SHORT).show(); return@button }
            if(date==null) { Toast.makeText(this,tr("Выберите корректную дату"),Toast.LENGTH_SHORT).show(); return@button }
            val storedDate=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.time)
            val all=tasksArray()
            all.put(JSONObject().put("id",System.currentTimeMillis()).put("title",title).put("crop",cropInput.text.toString().trim())
                .put("type",taskTypes[type.selectedItemPosition]).put("date",storedDate).put("done",false))
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
        val calendarLocale = if (isEnglish()) Locale.ENGLISH else Locale("ru")
        val monthTitle = SimpleDateFormat("LLLL yyyy", calendarLocale).format(selectedDate.time).replaceFirstChar { it.uppercase(calendarLocale) }
        content.addView(txt("Посевной календарь · $monthTitle", 21f, true))
        content.addView(txt("Выберите дату, чтобы увидеть фазу Луны, сезонные работы, задачи и заметки. Лунные подсказки — традиционный ориентир; учитывайте погоду и состояние почвы.", 14f))
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("‹" to -1, "Сегодня" to 0, "›" to 1).forEach { (label, delta) ->
            controls.addView(Button(this).apply {
                text = tr(label); isAllCaps = false
                setOnClickListener {
                    if (delta == 0) selectedDate = Calendar.getInstance() else shiftSelectedMonth(delta)
                    showCalendar()
                }
            }, LinearLayout.LayoutParams(0, -2, if (delta == 0) 2f else 1f))
        }
        controls.addView(Button(this).apply { text = tr("Год −"); isAllCaps = false; setOnClickListener { shiftSelectedYear(-1); showCalendar() } }, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(Button(this).apply { text = tr("Год +"); isAllCaps = false; setOnClickListener { shiftSelectedYear(1); showCalendar() } }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(controls)
        val weekdays = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
        val header = GridLayout(this).apply { columnCount = 7; rowCount = 1 }
        weekdays.forEach { day -> header.addView(TextView(this).apply { text = tr(day); gravity = Gravity.CENTER; textSize = 12f; setTextColor(if (day == "Сб" || day == "Вс") Color.rgb(180,75,55) else green); setTypeface(null, android.graphics.Typeface.BOLD); setPadding(0, dp(6), 0, dp(6)) }, GridLayout.LayoutParams(GridLayout.spec(0), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply { width = 0 }) }
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
            val note = noteForDate(day).isNotBlank()
            var count = 0
            for (i in 0 until tasks.length()) { val task = tasks.optJSONObject(i) ?: continue; if (task.optString("date") == key && !task.optBoolean("done", false)) count++ }
            val cellFill = when { isSelected -> Color.rgb(232,234,232); count > 0 || note -> Color.rgb(226,241,219); else -> Color.WHITE }
            val cellStroke = when { isSelected -> Color.rgb(91,119,94); isToday -> green; else -> Color.rgb(216,228,211) }
            val cellView = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(1), dp(2), dp(1), dp(2)); background = roundedBackground(cellFill, 8f, cellStroke); isClickable = true; isFocusable = true; setOnClickListener { selectedDate = day.clone() as Calendar; showCalendar() } }
            val dayHeader = FrameLayout(this)
            dayHeader.addView(TextView(this).apply { text = lunarGlyph(day) + " " + number.toString(); textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.rgb(35,48,38)); setTypeface(null, if (isToday || isSelected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL) }, FrameLayout.LayoutParams(-1, -1))
            dayHeader.addView(TextView(this).apply { text = tr("●"); textSize = 10f; gravity = Gravity.TOP or Gravity.END; setTextColor(dayIndicatorColor(day)); setPadding(0, 0, dp(2), 0); contentDescription = when (dayIndicatorColor(day)) { Color.rgb(46,139,87) -> "Условно благоприятный день"; Color.rgb(232,174,35) -> "Смешанный день"; else -> "День для осторожного планирования" } }, FrameLayout.LayoutParams(-1, -1))
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
            row.addView(TextView(this).apply { text = tr("●"); textSize = 20f; setTextColor(item.first); setPadding(0, 0, dp(10), 0) })
            row.addView(txt(item.second + " — " + item.third, 13f))
            legend.addView(row)
        }
        legend.addView(txt("Традиционная лунная подсказка, не научный прогноз урожайности. ● с числом — незавершённые задачи; ◆ — есть заметка.", 12f))
        content.addView(legend)
        card("Фаза Луны", lunarSummary(selectedDate))
        card("Что делать в этом месяце", seasonalAdvice(selectedDate))
        button("Подробности выбранного дня") { showCalendarDay(selectedDate) }
        button("Добавить заметку") {
            val displayDate = dateString(selectedDate)
            val key = noteKey(selectedDate)
            val input = EditText(this).apply { hint = tr("Заметка на выбранную дату"); setText(noteForDate(selectedDate)) }
            android.app.AlertDialog.Builder(this).setTitle(displayDate).setView(input)
                .setNegativeButton(tr("Отмена"), null)
                .setPositiveButton(tr("Сохранить")) { _, _ ->
                    val value = input.text.toString().trim()
                    val editor = prefs.edit()
                    if (value.isBlank()) editor.remove(key) else editor.putString(key, value)
                    editor.remove("note_" + displayDate).apply()
                    showCalendar()
                }.show()
        }
    }

    private fun showCalendarDay(date: Calendar) {
        val day = date.clone() as Calendar
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(day.time)
        val tasks = tasksArray(); val lines = mutableListOf<String>()
        for (i in 0 until tasks.length()) { val item = tasks.optJSONObject(i) ?: continue; if (item.optString("date") == key) { val crop = item.optString("crop", "").trim(); val taskLine = (if (item.optBoolean("done", false)) "✓ " else "• ") + item.optString("title", "Задача") + " — " + item.optString("type", "Уход"); lines.add(if (crop.isBlank()) taskLine else "$taskLine\n  Культура/грядка: $crop") } }
        val note = noteForDate(day)
        val body = dateString(day) + "\n\n" + lunarSummary(day) + "\n\n" + seasonalAdvice(day) + "\n\nЗадачи:\n" + (if (lines.isEmpty()) "На эту дату задач нет." else lines.joinToString("\n")) + "\n\nЗаметка: " + if (note.isBlank()) "нет" else note
        android.app.AlertDialog.Builder(this).setTitle(tr("Календарь на день")).setMessage(body).setPositiveButton(tr("Добавить задачу")) { _, _ -> addTaskForDate(day) }.setNegativeButton(tr("Закрыть"), null).show()
    }

    private fun addTaskForDate(date:Calendar) {
        val wrap=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,8,24,0) }
        val title=EditText(this).apply { hint=tr("Например, проверить грядки"); setSingleLine(true) }
        val crop=EditText(this).apply { hint=tr("Культура или грядка (необязательно)") }
        val types=listOf("Уход","Посев","Полив","Подкормка","Прополка","Пересадка","Защита растений","Сбор урожая")
        val type=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,types.map { tr(it) }) }
        val dateText=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(date.time)
        wrap.addView(title); wrap.addView(crop); wrap.addView(type)
        android.app.AlertDialog.Builder(this).setTitle(tr("Запланировать на ")+dateString(date)).setView(wrap)
            .setNegativeButton(tr("Отмена"),null).setPositiveButton(tr("Сохранить")) { _,_ ->
                val name=title.text.toString().trim()
                if(name.isBlank()) { Toast.makeText(this,tr("Введите название действия"),Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val all=tasksArray()
                all.put(JSONObject().put("id",System.currentTimeMillis()).put("title",name).put("crop",crop.text.toString().trim())
                    .put("type",types[type.selectedItemPosition]).put("date",dateText).put("done",false))
                prefs.edit().putString("care_tasks",all.toString()).apply()
                Toast.makeText(this,tr("Задача добавлена"),Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun openExternalLink(url:String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
        catch (_:Exception) { Toast.makeText(this,tr("Не удалось открыть браузер."),Toast.LENGTH_LONG).show() }
    }
    private fun dateString(c:Calendar)=SimpleDateFormat("d MMMM yyyy",Locale("ru")).format(c.time)
    private fun noteKey(c: Calendar): String = "note_" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)

    private fun noteForDate(c: Calendar): String {
        prefs.getString(noteKey(c), null)?.let { return it }
        // Compatibility with notes saved by older versions using a localized date in the key.
        return prefs.getString("note_" + dateString(c), "").orEmpty()
    }

    private fun migrateLegacyNoteKeys() {
        val editor = prefs.edit()
        var changed = false
        val parser = SimpleDateFormat("d MMMM yyyy", Locale("ru")).apply { isLenient = false }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith("note_") || value !is String) return@forEach
            val rawDate = key.removePrefix("note_")
            if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(rawDate)) return@forEach
            val parsedDate = runCatching { parser.parse(rawDate) }.getOrNull() ?: return@forEach
            val targetKey = "note_" + isoFormat.format(parsedDate)
            if (prefs.getString(targetKey, null).isNullOrBlank() && value.isNotBlank()) editor.putString(targetKey, value)
            editor.remove(key)
            changed = true
        }
        if (changed) editor.apply()
    }

    private fun shiftSelectedMonth(delta: Int) {
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
        selectedDate.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate.add(Calendar.MONTH, delta)
        selectedDate.set(Calendar.DAY_OF_MONTH, minOf(day, selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }

    private fun shiftSelectedYear(delta: Int) {
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)
        selectedDate.set(Calendar.DAY_OF_MONTH, 1)
        selectedDate.add(Calendar.YEAR, delta)
        selectedDate.set(Calendar.DAY_OF_MONTH, minOf(day, selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)))
    }

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
        val knownNewMoon=Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
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
        val knownNewMoon = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
        val synodicMonth = 29.530588853 * 24 * 60 * 60 * 1000
        val age = ((((c.timeInMillis - knownNewMoon) % synodicMonth) + synodicMonth) % synodicMonth) / synodicMonth
        return when {
            age < 0.035 || age in 0.22..0.28 || age in 0.47..0.535 || age in 0.72..0.78 || age >= 0.965 -> Color.rgb(232,174,35)
            age < 0.5 -> Color.rgb(46,139,87)
            else -> Color.rgb(210,65,65)
        }
    }

    private fun lunarGlyph(c: Calendar): String {
        val knownNewMoon = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
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

    // All catalog entries with a bundled icon are visible in the built-in catalog.
    // Detailed agronomic records override conservative, clearly qualified fallback guidance.
    private val builtInPlants: List<Plant> by lazy {
        val detailedByName = plants.associateBy { it.name.lowercase(Locale.ROOT) }
        val result = mutableListOf<Plant>()
        for (i in 0 until cropIconCatalog.length()) {
            val crop = cropIconCatalog.optJSONObject(i) ?: continue
            if (!crop.optBoolean("available", true)) continue
            val name = crop.optString("name").trim()
            if (name.isBlank()) continue
            val category = crop.optString("category").ifBlank { "Культуры" }
            val detailed = detailedByName[name.lowercase(Locale.ROOT)]
            result.add(detailed ?: Plant(
                name, category,
                "Уточняйте по сорту, региону и способу выращивания",
                "Зависит от культуры и размера посадочного материала",
                "Зависит от вида, температуры и влажности",
                "Это общая карточка без сортовой нормы. Сверяйте сроки, глубину и условия выращивания с упаковкой семян и местными агрономическими рекомендациями."
            ))
        }
        result
    }

    private fun cropIconChoices(): List<Pair<String, String>> {
        // Translate only the picker labels. Keep stable icon IDs unchanged for storage and backups.
        val choices = mutableListOf("generic" to tr("Росток"))
        for (i in 0 until cropIconCatalog.length()) {
            val crop = cropIconCatalog.optJSONObject(i) ?: continue
            if (!crop.optBoolean("available", true)) continue
            val id = crop.optString("id")
            val name = crop.optString("name")
            if (id.isNotBlank() && name.isNotBlank()) choices.add(id to tr(name))
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
        "rhubarb" -> R.drawable.plant_generic
        "generic" -> R.drawable.plant_generic
        else -> R.drawable.plant_generic
    }

    private fun cropIconKeyForName(name: String): String {
        val n = name.trim().lowercase(Locale.ROOT).replace('ё', 'е').replace(Regex("\\s+"), " ")
        // Prefer an exact catalog-name match before applying aliases.
        for (i in 0 until cropIconCatalog.length()) {
            val crop = cropIconCatalog.optJSONObject(i) ?: continue
            val catalogName = crop.optString("name").trim().lowercase(Locale.ROOT).replace('ё', 'е').replace(Regex("\\s+"), " ")
            if (catalogName == n) return crop.optString("id", "generic")
        }
        val aliases = listOf(
            "томат" to "tomato", "огур" to "cucumber", "ревень" to "rhubarb", "любисток" to "lovage", "перец слад" to "sweet_pepper",
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
        val name = EditText(this).apply { hint = tr("Название культуры *"); setText(existing?.optString("name", "") ?: "") }
        val cropCategories = listOf("Овощи","Зелень","Ягоды","Цветы","Другое")
        val category = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, cropCategories.map { tr(it) }); setSelection(cropCategories.indexOf(existing?.optString("category")).coerceAtLeast(0)) }
        val sowing = EditText(this).apply { hint = tr("Сроки посева"); setText(existing?.optString("sowing", "") ?: "") }
        val depth = EditText(this).apply { hint = tr("Глубина посева"); setText(existing?.optString("depth", "") ?: "") }
        val sprouts = EditText(this).apply { hint = tr("Сроки всходов"); setText(existing?.optString("sprouts", "") ?: "") }
        val advice = EditText(this).apply { hint = tr("Советы по выращиванию"); setText(existing?.optString("advice", "") ?: ""); minLines = 2 }
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
            val initialKey = if (existing == null) cropIconKeyForName(name.text.toString()) else legacyIconKey
            val index = iconChoices.indexOfFirst { it.first == initialKey }.let { if (it >= 0) it else 0 }
            setSelection(index)
        }
        // Keep a compact live icon beside the crop name so it remains visible above the keyboard.
        val iconPreview = ImageView(this).apply {
            run { val key = iconChoices[iconPicker.selectedItemPosition].first; setCropIcon(this, key) }
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = tr("Предпросмотр иконки культуры")
        }
        val nameRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(name, LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(8) })
            addView(iconPreview, LinearLayout.LayoutParams(dp(40), dp(40)))
        }
        iconPicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                setCropIcon(iconPreview, iconChoices[position].first)
            }
        }
        // Update the small preview immediately while typing; unknown names use the sprout.
        name.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val key = cropIconKeyForName(s?.toString().orEmpty())
                val index = iconChoices.indexOfFirst { it.first == key }
                val selectedIndex = if (index >= 0) index else 0
                setCropIcon(iconPreview, iconChoices[selectedIndex].first)
                if (iconPicker.selectedItemPosition != selectedIndex) {
                    iconPicker.setSelection(selectedIndex)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        val form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(4), dp(18), dp(4)) }
        form.addView(nameRow)
        listOf<View>(category, sowing, depth, sprouts, advice, txt("Цветная иконка культуры",14f,true), iconPicker).forEach { form.addView(it) }
        val dialog = android.app.AlertDialog.Builder(this).setTitle(if(existing == null) "Добавить свою культуру" else "Изменить культуру")
            .setView(ScrollView(this).apply { addView(form) }).setNegativeButton(tr("Отмена"), null).setPositiveButton(tr("Сохранить"), null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val cropName = name.text.toString().trim()
                if (cropName.isBlank()) { name.error = "Введите название"; return@setOnClickListener }
                val all = userCrops(); val id = existing?.optString("id") ?: java.util.UUID.randomUUID().toString()
                val item = JSONObject().put("id",id).put("name",cropName).put("category",cropCategories[category.selectedItemPosition]).put("sowing",sowing.text.toString()).put("depth",depth.text.toString()).put("sprouts",sprouts.text.toString()).put("advice",advice.text.toString()).put("icon",iconChoices[iconPicker.selectedItemPosition].first).put("emoji",plantEmoji(cropName))
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
        val search = EditText(this).apply { hint = tr("Найти культуру или группу"); setSingleLine(true); setText(query) }
        content.addView(search)
        content.addView(Button(this).apply { text = tr("＋ Добавить свою культуру"); isAllCaps = false; setOnClickListener { addUserCrop() } })
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
                .filter { it.optString("name").contains(filter,true) || tr(it.optString("name")).contains(filter,true) || it.optString("category").contains(filter,true) || tr(it.optString("category")).contains(filter,true) }
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
                        contentDescription = tr("Иконка культуры ") + crop.optString("name")
                    }, LinearLayout.LayoutParams(dp(76),dp(76)).apply { bottomMargin = dp(4) })
                    tile.addView(txt(tr(crop.optString("name")),15f,true).apply { gravity = Gravity.CENTER; maxLines = 2 })
                    tile.addView(txt(tr(crop.optString("category")),12f).apply { gravity = Gravity.CENTER })
                    tile.addView(Button(this).apply {
                        text = tr("Подробнее"); textSize = 11f; isAllCaps = false
                        setOnClickListener {
                            val details = listOf("Сроки посева" to "sowing","Глубина" to "depth","Всходы" to "sprouts","Советы" to "advice")
                                .mapNotNull { (label,key) -> crop.optString(key).takeIf { it.isNotBlank() }?.let { tr("$label: $it") } }
                                .joinToString("\n\n")
                            android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle(tr(crop.optString("name")))
                                .setMessage(tr(crop.optString("category")) + if(details.isBlank()) "" else "\n\n$details")
                                .setNeutralButton(tr("Изменить")) { _,_ -> addUserCrop(crop) }
                                .setNegativeButton(tr("Закрыть"),null)
                                .setPositiveButton(tr("Удалить")) { _,_ ->
                                    android.app.AlertDialog.Builder(this@MainActivity).setTitle(tr("Удалить культуру?"))
                                        .setMessage(tr(crop.optString("name"))).setNegativeButton(tr("Отмена"),null)
                                        .setPositiveButton(tr("Удалить")) { _,_ ->
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

            if (results.childCount == 0) results.addView(txt(if (filter.isBlank()) "Здесь будут отображаться ваши собственные культуры." else "Свои культуры не найдены. Введите название в форме посадки, чтобы выбрать встроенную культуру."))
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
        val name=EditText(this@MainActivity).apply { hint=tr("Культура (например, томат)"); setSingleLine(true) }
        val cropSuggestions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        content.addView(name)
        content.addView(cropSuggestions)
        fun updateCropSuggestions(query: String) {
            cropSuggestions.removeAllViews()
            val q = query.trim()
            if (q.isBlank()) {
                cropSuggestions.visibility = View.GONE
                return
            }
            val mine = userCrops()
            val customMatches = (0 until mine.length()).mapNotNull { mine.optJSONObject(it) }
                .filter { it.optString("name").contains(q, true) }
            val customNames = customMatches.map { it.optString("name").lowercase(Locale.ROOT) }.toSet()
            val builtMatches = builtInPlants.filter {
                it.name.contains(q, true) && it.name.lowercase(Locale.ROOT) !in customNames
            }
            val suggestions = customMatches.map { crop ->
                Triple(crop.optString("name"), crop.optString("icon", "generic"), true)
            } + builtMatches.map { plant ->
                Triple(plant.name, cropIconKeyForName(plant.name), false)
            }
            if (suggestions.isEmpty()) {
                cropSuggestions.visibility = View.GONE
                return
            }
            cropSuggestions.visibility = View.VISIBLE
            cropSuggestions.addView(txt("Подходящие культуры", 13f, true))
            suggestions.take(8).forEach { (cropName, iconKey, isCustom) ->
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(8), dp(5), dp(8), dp(5))
                    background = roundedBackground(Color.WHITE, 10f, Color.rgb(220,230,216))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        name.setText(cropName)
                        name.setSelection(name.text.length)
                        cropSuggestions.removeAllViews()
                        cropSuggestions.visibility = View.GONE
                    }
                }
                row.addView(ImageView(this@MainActivity).apply {
                    if (isCustom) setCropIcon(this, iconKey) else setCropIcon(this, iconKey)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = tr("Иконка культуры $cropName")
                }, LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(10) })
                row.addView(txt(cropName, 15f, true), LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(txt("Выбрать", 12f))
                cropSuggestions.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) })
            }
        }
        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCropSuggestions(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        val variety=EditText(this@MainActivity).apply { hint=tr("Сорт"); setSingleLine(true) }
        val sowing=EditText(this@MainActivity).apply { hint=tr("Дата посева (дд.мм.гггг)"); setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date())); setSingleLine(true) }
        val place=EditText(this@MainActivity).apply { hint=tr("Место (теплица, грядка №1, балкон)"); setSingleLine(true) }
        val notes=EditText(this@MainActivity).apply { hint=tr("Заметки (необязательно)"); minLines=2; maxLines=4 }
        content.addView(variety); content.addView(sowing); content.addView(place); content.addView(notes)
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
        val harvestCrop=EditText(this@MainActivity).apply { hint=tr("Что собрали?"); setSingleLine(true) }
        val harvestAmount=EditText(this@MainActivity).apply { hint=tr("Количество (например, 2,5 кг)"); setSingleLine(true) }
        val harvestDate=EditText(this@MainActivity).apply { hint=tr("Дата сбора"); setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date())); setSingleLine(true) }
        val harvestNotes=EditText(this@MainActivity).apply { hint=tr("Примечание (необязательно)"); setSingleLine(true) }
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
            android.app.AlertDialog.Builder(this@MainActivity).setTitle(tr("Очистить посадки?"))
                .setMessage(tr("Удалятся подробные и старые записи о посадках. Журнал урожая останется."))
                .setNegativeButton(tr("Отмена"),null)
                .setPositiveButton(tr("Очистить")) { _,_ -> prefs.edit().remove("plantings").remove("planting_records").apply(); showDiary() }.show()
        }
        button("Очистить журнал урожая") {
            android.app.AlertDialog.Builder(this@MainActivity).setTitle(tr("Очистить журнал урожая?"))
                .setMessage(tr("Все записи о собранном урожае будут удалены."))
                .setNegativeButton(tr("Отмена"),null)
                .setPositiveButton(tr("Очистить")) { _,_ -> prefs.edit().remove("harvest_records").apply(); showDiary() }.show()
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
        val alarm = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(this, 91, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (!prefs.getBoolean("reminder_enabled", false)) {
            alarm.cancel(pending)
            return
        }
        val hour = prefs.getInt("reminder_hour", 9).coerceIn(0, 23)
        val minute = prefs.getInt("reminder_minute", 0).coerceIn(0, 59)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }
}
