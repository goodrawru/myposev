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
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Plant(val name:String, val group:String, val sowing:String, val depth:String, val sprouts:String, val advice:String)
data class GardenCity(val city:String, val region:String, val zone:String, val latitude:Double, val longitude:Double)


class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var selectedDate = Calendar.getInstance()
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
        showHome()
    }

    private fun baseLayout(title:String) {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(pale) }
        root.addView(TextView(this).apply {
            text = "🌱  " + title; textSize=25f; setTextColor(Color.WHITE); setBackgroundColor(green)
            setPadding(20,22,20,22); gravity=Gravity.CENTER_VERTICAL
        })
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
                val warning=if(frost) "\\n⚠️ Возможен заморозок: защитите теплолюбивую рассаду." else "\\nСверяйте прогноз с условиями именно вашего участка."
                val summary=lines.joinToString("\\n")+warning+"\\nИсточник: Open-Meteo. Координаты передаются сервису только при запросе прогноза."
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
        val dateText=txt("Выбранная дата: "+dateString(selectedDate),18f,true)
        content.addView(dateText)
        button("Выбрать день") {
            DatePickerDialog(this,{_,year,month,day ->
                selectedDate.set(year,month,day)
                showCalendar()
            },selectedDate.get(Calendar.YEAR),selectedDate.get(Calendar.MONTH),selectedDate.get(Calendar.DAY_OF_MONTH)).show()
        }
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
