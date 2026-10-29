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

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE) }
    private lateinit var content: LinearLayout
    private var selectedDate = Calendar.getInstance()
    private val green = Color.rgb(39, 91, 57)
    private val pale = Color.rgb(239, 246, 235)
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
        listOf("Сегодня","Календарь","Культуры","Мой огород").forEach { label ->
            nav.addView(Button(this).apply {
                text=label; textSize=10f; isAllCaps=false
                setOnClickListener { when(label) {
                    "Сегодня" -> showHome()
                    "Календарь" -> showCalendar()
                    "Культуры" -> showPlants("")
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
        card("🌿 План на сегодня","Выбирайте культуру и регион, чтобы уточнить сроки. Учитывайте местный климат, прогноз и состояние почвы.")
        card("🌙 Лунный календарь",lunarSummary(Calendar.getInstance()),{showCalendar()})
        card("📅 Выбрать дату","Откройте календарь, чтобы посмотреть сезонные подсказки и заметки.",{showCalendar()})
        card("🌱 Каталог культур","Овощи, зелень, ягоды и цветы — сроки, глубина посева и советы.",{showPlants("")})
        card("🔔 Напоминания","Ежедневное локальное напоминание запланировано на 09:00.")
        card("ℹ️ Важно","Это стартовая база. Универсальных дат посева для всей России не существует.")
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
    private fun seasonalAdvice(c:Calendar):String = when(c.get(Calendar.MONTH)) {
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
