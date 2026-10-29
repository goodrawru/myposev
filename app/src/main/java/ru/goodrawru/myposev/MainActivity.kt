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
import android.os.Build
import android.os.Bundle
import android.view.Gravity
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
        Plant("Бархатцы","Цветы","Весной на рассаду или после заморозков","0,5–1 см","5–10 дней","Теплолюбивы; высаживайте после угрозы заморозков.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        scheduleDailyReminder()
        connectMusicController()
        showHome()
    }

    private fun baseLayout(title:String) {
        val root = LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(pale)
            // Respect Android 15 edge-to-edge status-bar inset.
            setOnApplyWindowInsetsListener { view, insets ->
                val topInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
                val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else 0
                view.setPadding(0, topInset, 0, bottomInset)
                insets
            }
        }
        root.addView(TextView(this).apply {
            text = "🌱  " + title; textSize=25f; setTextColor(Color.WHITE); setBackgroundColor(green)
            setPadding(20,22,20,22); gravity=Gravity.CENTER_VERTICAL
        })
        // Section description row with an offline, original ambient sound toggle.
        val sectionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(3), dp(6), dp(3))
            setBackgroundColor(Color.WHITE)
        }
        sectionRow.addView(TextView(this).apply {
            text = "Раздел: $title"
            textSize = 12f
            setTextColor(Color.rgb(65, 78, 67))
            maxLines = 2
        }, LinearLayout.LayoutParams(0, -2, 1f))
        sectionRow.addView(Button(this).apply {
            text = "🎧 Погружение"
            textSize = 12f
            isAllCaps = false
            minWidth = 0
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { showImmersionPlayer() }
        })
        root.addView(sectionRow)
        val nav = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE) }
        listOf("Сегодня","Календарь","Культуры","Задачи","Огород").forEach { label ->
            nav.addView(Button(this).apply {
                text=label; textSize=9f; isAllCaps=false; minWidth=0; setPadding(2,4,2,4)
                setOnClickListener { when(label) {
                    "Сегодня" -> showHome()
                    "Календарь" -> showCalendar()
                    "Культуры" -> showPlants("")
                    "Задачи" -> showTasks()
                    else -> showDiary()
                } }
            }, LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(nav)
        val scroll=ScrollView(this)
        content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,28) }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun showImmersionPlayer() {
        baseLayout("Музыка и радио")
        content.addView(txt("Музыкальный плеер", 23f, true))
        content.addView(txt("Слушайте файлы с телефона или добавьте прямую ссылку на аудиопоток. Плеер работает в фоне и показывает системные кнопки управления.", 14f))
        val nowPlaying = txt("Подключение к плееру…", 18f, true)
        musicStatusView = nowPlaying
        card("🎵 Сейчас играет", "Название трека и состояние воспроизведения появятся здесь.")
        content.addView(nowPlaying)
        val progress = SeekBar(this).apply {
            max = 1000
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) userSeekingMusic = true
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) { userSeekingMusic = true }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val player = musicController
                    if (player != null && player.duration > 0 && player.duration != androidx.media3.common.C.TIME_UNSET) {
                        player.seekTo((player.duration * (seekBar?.progress ?: 0) / 1000).toLong())
                    }
                    userSeekingMusic = false
                }
            })
        }
        musicProgress = progress
        content.addView(progress)
        content.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
            addView(Button(this@MainActivity).apply { text = "⏮"; contentDescription = "Предыдущий трек"; setOnClickListener { musicController?.seekToPreviousMediaItem() } })
            addView(Button(this@MainActivity).apply { text = "▶ / ⏸"; contentDescription = "Воспроизвести или поставить на паузу"; setOnClickListener {
                val player = musicController
                if (player == null) Toast.makeText(this@MainActivity, "Плеер ещё подключается", Toast.LENGTH_SHORT).show()
                else if (player.isPlaying) player.pause() else { if (player.mediaItemCount == 0) chooseLocalAudio() else player.play() }
            } })
            addView(Button(this@MainActivity).apply { text = "⏭"; contentDescription = "Следующий трек"; setOnClickListener { musicController?.seekToNextMediaItem() } })
        })
        button("＋ Выбрать музыку на телефоне") { chooseLocalAudio() }
        button("＋ Добавить прямую ссылку на аудио") { showStreamUrlDialog() }
        button("Очистить очередь") {
            musicController?.clearMediaItems()
            updateMusicStatus()
        }
        content.addView(txt("Онлайн-источники", 20f, true))
        card("📻 Радио Дача", "Открыть официальный онлайн-плеер радиостанции. Поток не извлекается и не ретранслируется приложением.") {
            openMusicSource("https://www.radiodacha.ru/player/", "Радио Дача")
        }
        button("Открыть Радио Дача в браузере") { openMusicSource("https://www.radiodacha.ru/player/", "Радио Дача") }
        card("🎶 Zvukogram", "Каталог музыки. Проверяйте лицензию и условия конкретной записи перед скачиванием или использованием.") {
            openMusicSource("https://zvukogram.com/category/veselaya-muzyika/", "Zvukogram")
        }
        button("Открыть Zvukogram") { openMusicSource("https://zvukogram.com/category/veselaya-muzyika/", "Zvukogram") }
        card("☀️ Chosic", "Каталог бесплатной музыки. Условия и требования к указанию автора зависят от трека.") {
            openMusicSource("https://www.chosic.com/free-music/happy/", "Chosic")
        }
        button("Открыть Chosic") { openMusicSource("https://www.chosic.com/free-music/happy/", "Chosic") }
        button("На главный экран") { showHome() }
        updateMusicStatus()
        musicHandler.removeCallbacks(musicProgressTask)
        musicHandler.post(musicProgressTask)
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

    private fun showStreamUrlDialog() {
        val input = EditText(this).apply {
            hint = "https://…/stream.mp3"
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Прямая ссылка на аудио")
            .setMessage("Вставьте прямую ссылку на разрешённый аудиофайл или поток (MP3/AAC/HLS). Обычная ссылка на страницу сайта не подойдёт.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Добавить") { _, _ ->
                val raw = input.text.toString().trim()
                val uri = try { Uri.parse(raw) } catch (_: Exception) { null }
                if (uri == null || (uri.scheme != "https" && uri.scheme != "http") || uri.host.isNullOrBlank()) {
                    Toast.makeText(this, "Введите корректную ссылку http:// или https://", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val player = musicController
                if (player == null) {
                    Toast.makeText(this, "Плеер ещё подключается", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val title = uri.host ?: "Интернет-аудио"
                val item = MediaItem.Builder().setUri(uri).setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
                if (player.mediaItemCount == 0) {
                    player.setMediaItem(item)
                    player.prepare()
                    player.play()
                } else player.addMediaItem(item)
                updateMusicStatus()
            }.show()
    }

    private fun updateMusicStatus() {
        val player = musicController ?: run {
            musicStatusView?.text = "Подключение к плееру…"
            return
        }
        val title = player.currentMediaItem?.mediaMetadata?.title?.toString()?.takeIf { it.isNotBlank() }
            ?: if (player.mediaItemCount == 0) "Очередь пуста — добавьте аудиофайл или поток" else "Аудио"
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

    private fun txt(value:String,size:Float=16f,bold:Boolean=false)=TextView(this).apply {
        text=value; textSize=size; setTextColor(Color.rgb(35,48,38))
        if(bold) setTypeface(null,android.graphics.Typeface.BOLD)
        setPadding(0,7,0,7)
    }
    private fun button(label:String, action:()->Unit) {
        content.addView(Button(this).apply { text=label; isAllCaps=false; setOnClickListener { action() } })
    }
    private fun card(title:String, body:String, action:(()->Unit)?=null) {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,10,14,12); setBackgroundColor(Color.WHITE) }
        box.addView(txt(title,18f,true)); box.addView(txt(body,14f))
        if(action!=null) box.setOnClickListener { action() }
        content.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=12 })
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
        card("🌦️ Погода на 3 дня",prefs.getString("weather_summary","Нажмите, чтобы получить прогноз для выбранного места.") ?: "Прогноз пока не загружен.",{loadWeather()})
        card("🪴 Полезный принцип","Посевные сроки — ориентир, а не гарантия. Сверяйте рекомендации с прогнозом заморозков, температурой почвы и условиями конкретной грядки.")
        card("🌙 Лунный календарь",lunarSummary(Calendar.getInstance()),{showCalendar()})
        card("📅 Выбрать дату","Откройте календарь, чтобы посмотреть сезонные подсказки и заметки.",{showCalendar()})
        card("🌱 Каталог культур","Овощи, зелень, ягоды и цветы — сроки, глубина посева и советы.",{showPlants("")})
        card("🔔 Напоминания","Ежедневное локальное напоминание запланировано на 09:00.")
        card("ℹ️ Важно","Это стартовая база. Универсальных дат посева для всей России не существует.")
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
        val lat=prefs.getString("garden_lat",null)?.toDoubleOrNull()
        val lon=prefs.getString("garden_lon",null)?.toDoubleOrNull()
        if(lat==null || lon==null) {
            Toast.makeText(this,"Сначала выберите город или определите местоположение.",Toast.LENGTH_LONG).show()
            showLocationSettings()
            return
        }
        Toast.makeText(this,"Загружаю прогноз погоды…",Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val url="https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=temperature_2m_min,temperature_2m_max,precipitation_probability_max&forecast_days=3&timezone=auto"
                val connection=java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout=9000
                connection.readTimeout=9000
                connection.requestMethod="GET"
                val body=connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val daily=JSONObject(body).getJSONObject("daily")
                val dates=daily.getJSONArray("time")
                val mins=daily.getJSONArray("temperature_2m_min")
                val maxs=daily.getJSONArray("temperature_2m_max")
                val rain=daily.getJSONArray("precipitation_probability_max")
                val lines=mutableListOf<String>()
                var frost=false
                for(i in 0 until dates.length()) {
                    val min=mins.optDouble(i,Double.NaN)
                    val max=maxs.optDouble(i,Double.NaN)
                    val rainChance=rain.optInt(i,-1)
                    val date=dates.optString(i).takeLast(5).replace("-",".")
                    if(min.isFinite() && min<=0.0) frost=true
                    val minText=if(min.isFinite()) String.format(Locale.US,"%.0f",min) else "?"
                    val maxText=if(max.isFinite()) String.format(Locale.US,"%.0f",max) else "?"
                    val rainText=if(rainChance>=0) "; осадки до $rainChance%" else ""
                    lines.add("$date: $minText…$maxText °C$rainText")
                }
                val warning=if(frost) "\n⚠️ Возможен заморозок: защитите теплолюбивую рассаду." else "\nСверяйте прогноз с условиями именно вашего участка."
                val summary=lines.joinToString("\n")+warning+"\nИсточник: Open-Meteo. Координаты передаются сервису только при запросе прогноза."
                prefs.edit().putString("weather_summary",summary).apply()
                runOnUiThread { showHome() }
            } catch (_:Exception) {
                runOnUiThread {
                    Toast.makeText(this,"Не удалось загрузить прогноз. Проверьте интернет.",Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun showTasks() {
        baseLayout("Задачи по уходу")
        content.addView(txt("Планируйте полив, посев, подкормку, прополку и сбор урожая. Отмечайте выполненное — оно останется в истории.",14f))
        val titleInput=EditText(this).apply { hint="Задача (например, полить томаты)" }
        val cropInput=EditText(this).apply { hint="Культура или грядка (необязательно)" }
        val dateInput=EditText(this).apply { hint="Дата ГГГГ-ММ-ДД"; setSingleLine(true); setText(SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())) }
        val type=Spinner(this).apply {
            adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,
                listOf("Уход","Посев","Полив","Подкормка","Прополка","Пересадка","Защита растений","Сбор урожая"))
        }
        content.addView(titleInput); content.addView(cropInput); content.addView(dateInput); content.addView(type)
        button("Добавить задачу") {
            val title=titleInput.text.toString().trim()
            val date=dateInput.text.toString().trim()
            if(title.isBlank()) { Toast.makeText(this,"Введите название задачи",Toast.LENGTH_SHORT).show(); return@button }
            if(!date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) { Toast.makeText(this,"Дата должна быть в формате ГГГГ-ММ-ДД",Toast.LENGTH_SHORT).show(); return@button }
            val all=tasksArray()
            all.put(JSONObject().put("id",System.currentTimeMillis()).put("title",title).put("crop",cropInput.text.toString().trim())
                .put("type",type.selectedItem.toString()).put("date",date).put("done",false))
            prefs.edit().putString("care_tasks",all.toString()).apply()
            showTasks()
        }
        content.addView(txt("Мои задачи",19f,true))
        val all=tasksArray()
        val entries=(0 until all.length()).mapNotNull { all.optJSONObject(it) }.sortedWith(compareBy<JSONObject> { it.optBoolean("done",false) }.thenBy { it.optString("date") })
        if(entries.isEmpty()) content.addView(txt("Список пуст. Создайте первую задачу выше."))
        entries.forEach { item ->
            val done=item.optBoolean("done",false)
            val name=item.optString("title","Задача")
            val crop=item.optString("crop","")
            val details=item.optString("date")+" · "+item.optString("type")+(if(crop.isNotBlank()) " · $crop" else "")
            card((if(done) "✅ " else "🟢 ")+name,details+(if(done) "\nВыполнено" else ""))
            if(!done) button("Отметить выполненной: $name") {
                val current=tasksArray()
                for(i in 0 until current.length()) {
                    val row=current.optJSONObject(i) ?: continue
                    if(row.optLong("id")==item.optLong("id")) { row.put("done",true); break }
                }
                prefs.edit().putString("care_tasks",current.toString()).apply()
                showTasks()
            } else button("Удалить из истории: $name") {
                val current=tasksArray()
                val filtered=JSONArray()
                for(i in 0 until current.length()) {
                    val row=current.optJSONObject(i) ?: continue
                    if(row.optLong("id")!=item.optLong("id")) filtered.put(row)
                }
                prefs.edit().putString("care_tasks",filtered.toString()).apply()
                showTasks()
            }
        }
    }

    private fun showCalendar() {
        baseLayout("Календарь")
        val monthTitle = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(selectedDate.time)
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(selectedDate.time)
        content.addView(txt("Календарь Ортон · " + monthTitle.replaceFirstChar { it.uppercase(Locale("ru")) },19f,true))
        content.addView(txt("После обновления нажимайте на день, чтобы посмотреть отметку календаря и запланировать работу.",14f))
        val monthNav=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        monthNav.addView(Button(this).apply { text="‹"; contentDescription="Предыдущий месяц"; setOnClickListener { selectedDate.add(Calendar.MONTH,-1); showCalendar() } },
            LinearLayout.LayoutParams(0,-2,1f))
        monthNav.addView(Button(this).apply { text="Сегодня"; isAllCaps=false; setOnClickListener { selectedDate=Calendar.getInstance(); showCalendar() } },
            LinearLayout.LayoutParams(0,-2,2f))
        monthNav.addView(Button(this).apply { text="›"; contentDescription="Следующий месяц"; setOnClickListener { selectedDate.add(Calendar.MONTH,1); showCalendar() } },
            LinearLayout.LayoutParams(0,-2,1f))
        content.addView(monthNav)
        button("🌱 Открыть статью Ортон за этот месяц") { openCalendarSource(ortonUrl(selectedDate)) }
        button("🔄 Обновить календарь Ортон") { refreshOrtonCalendar(monthKey) }
        val cached = getOrtonMonth(monthKey)
        val status = if(cached == null) "Календарь ещё не загружен. Нажмите «Обновить календарь Ортон»." else
            "Данные получены: " + cached.optString("checkedAt","дата неизвестна") + "\nИсточник: Ортон. Проверьте условия участка и прогноз погоды."
        card("Состояние календаря",status)
        val weekdays = listOf("Пн","Вт","Ср","Чт","Пт","Сб","Вс")
        val header = GridLayout(this).apply { columnCount=7; rowCount=1 }
        weekdays.forEach { day ->
            header.addView(TextView(this).apply { text=day; gravity=Gravity.CENTER; textSize=12f; setTextColor(green); setPadding(0,6,0,6) },
                GridLayout.LayoutParams(GridLayout.spec(0),GridLayout.spec(GridLayout.UNDEFINED,1f)).apply { width=0 })
        }
        content.addView(header,LinearLayout.LayoutParams(-1,-2))
        val grid = GridLayout(this).apply { columnCount=7; rowCount=6 }
        val first = selectedDate.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH,1)
        val offset = (first.get(Calendar.DAY_OF_WEEK)+5)%7
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        for(cell in 0 until 42) {
            val dayNumber = cell-offset+1
            val lp = GridLayout.LayoutParams(GridLayout.spec(cell/7),GridLayout.spec(cell%7,1f)).apply {
                width=0; height=dp(48); setMargins(dp(2),dp(2),dp(2),dp(2))
            }
            if(dayNumber !in 1..daysInMonth) {
                grid.addView(TextView(this),lp)
            } else {
                val rating = cached?.optJSONObject("ratings")?.optString(dayNumber.toString(),"") ?: ""
                val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH)==dayNumber
                val label = when(rating) {
                    "Лучший день" -> "★"; "Благоприятный" -> "•"
                    "Нежелательный" -> "!"; "Неблагоприятный" -> "×"; else -> ""
                }
                val b = Button(this).apply {
                    text = dayNumber.toString() + if(label.isNotBlank()) "\n"+label else ""
                    textSize=12f; isAllCaps=false; minWidth=0; minHeight=0; setPadding(0,0,0,0)
                    setTextColor(if(isSelected) Color.WHITE else Color.rgb(35,48,38))
                    setBackgroundColor(if(isSelected) green else when(rating) {
                        "Лучший день" -> Color.rgb(190,225,181)
                        "Благоприятный" -> Color.rgb(222,239,215)
                        "Нежелательный" -> Color.rgb(255,237,196)
                        "Неблагоприятный" -> Color.rgb(250,215,210)
                        else -> Color.WHITE
                    })
                    setOnClickListener { selectedDate.set(Calendar.DAY_OF_MONTH,dayNumber); showOrtonDay(monthKey,dayNumber) }
                }
                grid.addView(b,lp)
            }
        }
        content.addView(grid,LinearLayout.LayoutParams(-1,-2))
        card("Обозначения","★ самые благоприятные · • благоприятные · ! нежелательные · × самые неблагоприятные. Пустая отметка означает, что источник не указал отдельную категорию для этого дня.")
        val d=dateString(selectedDate)
        card("🌙 Лунная фаза",lunarSummary(selectedDate))
        card("🌱 Сезонные подсказки",seasonalAdvice(selectedDate))
        card("📝 Заметка на дату",prefs.getString("note_"+d,"")?.ifBlank{"Заметок пока нет."} ?: "Заметок пока нет.")
        button("Добавить заметку") {
            val input=EditText(this).apply { hint="Например: посеял томаты" }
            android.app.AlertDialog.Builder(this).setTitle("Заметка на "+d).setView(input)
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Сохранить") { _,_ -> prefs.edit().putString("note_"+d,input.text.toString()).apply(); showCalendar() }.show()
        }
    }

    private fun ortonUrl(date:Calendar):String {
        val slug = when(date.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "yanvar"; Calendar.FEBRUARY -> "fevral"; Calendar.MARCH -> "mart"
            Calendar.APRIL -> "aprel"; Calendar.MAY -> "may"; Calendar.JUNE -> "iyun"
            Calendar.JULY -> "iyul"; Calendar.AUGUST -> "avgust"; Calendar.SEPTEMBER -> "sentyabr"
            Calendar.OCTOBER -> "oktyabr"; Calendar.NOVEMBER -> "noyabr"; else -> "dekabr"
        }
        return "https://orton.ru/kalendar-ogorodnika/" + slug + "/"
    }

    private fun getOrtonMonth(key:String):JSONObject? = try {
        val all=JSONObject(prefs.getString("orton_calendar_cache","{}") ?: "{}")
        all.optJSONObject(key)
    } catch (_:Exception) { null }

    private fun refreshOrtonCalendar(monthKey:String) {
        Toast.makeText(this,"Загружаю календарь Ортон…",Toast.LENGTH_SHORT).show()
        val target = selectedDate.clone() as Calendar
        Thread {
            try {
                val connection=java.net.URL(ortonUrl(target)).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout=12000; connection.readTimeout=12000
                connection.setRequestProperty("User-Agent","Mozilla/5.0 (Android) MoiPosevnoy/1.0")
                val html=connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                connection.disconnect()
                val plain=html.replace(Regex("(?is)<script.*?</script>|<style.*?</style>")," ")
                    .replace(Regex("(?s)<[^>]+>")," ")
                    .replace("&nbsp;"," ").replace("&#8211;","–").replace("&ndash;","–")
                    .replace("&#8212;","—").replace("&mdash;","—").replace("&amp;","&")
                    .replace("&#39;","'").replace("&quot;","\"").replace(Regex("\\s+")," ")
                val ratings=JSONObject()
                fun collect(label:String, rating:String) {
                    val pattern=Regex("(?i)(?<!Самые )"+Regex.escape(label)+"\\s*[-–—:]\\s*([0-9,\\s]+?)(?=\\s+(?:Самые благоприятные дни|Благоприятные дни|Нейтральные дни|Нежелательные дни|Самые неблагоприятные дни|Благоприятные посадочные дни|Благоприятные дни для|Неблагоприятные дни|Фазы луны|$))")
                    val match=pattern.find(plain) ?: return
                    Regex("\\d{1,2}").findAll(match.groupValues[1]).forEach { token ->
                        val n=token.value.toIntOrNull() ?: return@forEach
                        if(n in 1..31 && !ratings.has(n.toString())) ratings.put(n.toString(),rating)
                    }
                }
                collect("Самые благоприятные дни","Лучший день")
                collect("Благоприятные дни","Благоприятный")
                collect("Нейтральные дни","Нейтральный")
                collect("Нежелательные дни","Нежелательный")
                collect("Самые неблагоприятные дни","Неблагоприятный")
                val checked=SimpleDateFormat("d MMM yyyy, HH:mm",Locale("ru")).format(Date())
                val result=JSONObject().put("checkedAt",checked).put("source","Ортон").put("ratings",ratings)
                    .put("month",monthKey).put("sourceUrl",ortonUrl(target))
                val all=JSONObject(prefs.getString("orton_calendar_cache","{}") ?: "{}")
                all.put(monthKey,result)
                prefs.edit().putString("orton_calendar_cache",all.toString()).putString("calendar_sources_checked",checked).apply()
                runOnUiThread {
                    Toast.makeText(this,"Календарь обновлён. Дней с отметками: "+ratings.length(),Toast.LENGTH_LONG).show()
                    showCalendar()
                }
            } catch (_:Exception) {
                runOnUiThread { Toast.makeText(this,"Не удалось загрузить календарь. Проверьте интернет; можно открыть статью Ортон вручную.",Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    private fun showOrtonDay(monthKey:String,dayNumber:Int) {
        val cached=getOrtonMonth(monthKey)
        val rating=cached?.optJSONObject("ratings")?.optString(dayNumber.toString(),"").orEmpty()
        val selected=selectedDate.clone() as Calendar
        selected.set(Calendar.DAY_OF_MONTH,dayNumber)
        val lunar=lunarSummary(selected)
        val ratingText=if(rating.isBlank()) "Ортон не указал отдельную категорию для этого дня в общей сводке." else "Отметка Ортон: $rating."
        val body=dateString(selected)+"\n"+ratingText+"\n"+lunar+"\nСверяйте лунный календарь с погодой и состоянием почвы."
        android.app.AlertDialog.Builder(this).setTitle("Рекомендации на день").setMessage(body)
            .setPositiveButton("Запланировать действие") { _,_ -> addTaskForDate(selected) }
            .setNeutralButton("Открыть Ортон") { _,_ -> openCalendarSource(ortonUrl(selected)) }
            .setNegativeButton("Закрыть",null).show()
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

    private fun showCalendarSources() {
        baseLayout("Календарь: источники")
        val month = SimpleDateFormat("LLLL yyyy", Locale("ru")).format(selectedDate.time)
        content.addView(txt("Материалы для сверки · " + month,18f,true))
        card("🌙 Яндекс Погода","Лунный посевной календарь и сезонные рекомендации. Откроется статья в браузере; ссылка на октябрь 2026 года приведена как пример.")
        button("Открыть календарь Яндекс Погоды") { openCalendarSource("https://yandex.ru/pogoda/ru/blog/lunnyj-posevnoj-kalendar-oktyabr-2026") }
        card("🌱 Ортон","Календарь огородника по месяцам. Выберите нужный месяц на сайте и сравните рекомендации с погодой своего региона.")
        button("Открыть календарь Ортон · " + month.substringBefore(" ").lowercase(Locale("ru"))) {
            val slug = when (selectedDate.get(Calendar.MONTH)) {
                Calendar.JANUARY -> "yanvar"
                Calendar.FEBRUARY -> "fevral"
                Calendar.MARCH -> "mart"
                Calendar.APRIL -> "aprel"
                Calendar.MAY -> "may"
                Calendar.JUNE -> "iyun"
                Calendar.JULY -> "iyul"
                Calendar.AUGUST -> "avgust"
                Calendar.SEPTEMBER -> "sentyabr"
                Calendar.OCTOBER -> "oktyabr"
                Calendar.NOVEMBER -> "noyabr"
                else -> "dekabr"
            }
            openCalendarSource("https://orton.ru/kalendar-ogorodnika/" + slug + "/")
        }
        card("Как обновляются данные","Приложение пока не копирует статьи автоматически: структура сайтов может меняться, а даты разных источников иногда расходятся. Кнопки открывают первоисточники для проверки. Можно сохранить дату проверки; личные заметки и задачи не затрагиваются.")
        button("Отметить, что источники проверены") {
            val stamp = SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(Date())
            prefs.edit().putString("calendar_sources_checked",stamp).apply()
            Toast.makeText(this,"Дата проверки сохранена",Toast.LENGTH_SHORT).show()
            showCalendarSources()
        }
        button("Назад к календарю") { showCalendar() }
    }

    private fun openCalendarSource(url:String) {
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

    private fun showPlants(query:String) {
        baseLayout("Каталог культур")
        val search=EditText(this).apply { hint="Найти культуру или группу"; setSingleLine(true); setText(query) }
        content.addView(search)
        val results=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        content.addView(results)
        fun fill(filter:String) {
            results.removeAllViews()
            plants.filter { it.name.contains(filter,true) || it.group.contains(filter,true) }.forEach { p ->
                val box=LinearLayout(this).apply {
                    orientation=LinearLayout.VERTICAL; setPadding(12,8,12,10); setBackgroundColor(Color.WHITE)
                    addView(txt(p.name,18f,true)); addView(txt(p.group,13f))
                    addView(txt("Посев: "+p.sowing,14f))
                    addView(txt("Глубина: "+p.depth+" · Всходы: "+p.sprouts,13f))
                    addView(txt(p.advice,14f))
                }
                results.addView(box,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=12 })
            }
            if(results.childCount==0) results.addView(txt("Ничего не найдено. Попробуйте другое название."))
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
        val input=EditText(this).apply { hint="Например: томаты, сорт Де Барао" }
        content.addView(input)
        val dateInput=EditText(this).apply { hint="Дата посева (например, 15.03.2027)"; setText(SimpleDateFormat("dd.MM.yyyy",Locale("ru")).format(Date())) }
        content.addView(dateInput)
        button("Добавить посадку") {
            val name=input.text.toString().trim(); val date=dateInput.text.toString().trim()
            if(name.isBlank()) Toast.makeText(this,"Введите название культуры",Toast.LENGTH_SHORT).show()
            else {
                val old=prefs.getString("plantings","").orEmpty()
                val updated=(old.split("\n").filter{it.isNotBlank()}+"$date — $name").joinToString("\n")
                prefs.edit().putString("plantings",updated).apply()
                showDiary()
            }
        }
        content.addView(txt("Сохранённые записи",18f,true))
        val entries=prefs.getString("plantings","").orEmpty()
        if(entries.isBlank()) content.addView(txt("Пока пусто. Добавьте первую посадку."))
        else entries.split("\n").forEach { card("🌱 "+it,"Запись хранится на этом устройстве.") }
        button("Очистить дневник") {
            android.app.AlertDialog.Builder(this).setTitle("Очистить дневник?")
                .setMessage("Все записи о посадках будут удалены.")
                .setNegativeButton("Отмена",null)
                .setPositiveButton("Очистить") { _,_ -> prefs.edit().remove("plantings").apply(); showDiary() }.show()
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
