package ru.goodrawru.myposev

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE)
        val isTest = intent?.action == ACTION_TEST
        if (!isTest && !prefs.getBoolean("reminder_enabled", true)) return
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val categories = prefs.getStringSet("reminder_categories", null) ?: DEFAULT_CATEGORIES
        val allItems = buildDailyPlan(context, prefs, categories)
        val limit = prefs.getInt("reminder_limit", 4).coerceIn(1, 6)
        val items = allItems.distinct().take(limit)
        val body = if (items.isEmpty()) {
            if (isTest) "Тест: уведомления включены. Выберите категории рекомендаций в настройках."
            else return
        } else items.joinToString("\n") { "• $it" }

        val openApp = PendingIntent.getActivity(
            context, 92, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "posev_reminders")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val date = SimpleDateFormat("d MMMM", Locale("ru")).format(Date())
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Мой Посевной · План на сегодня")
            .setContentText(items.firstOrNull() ?: "Проверка ежедневных уведомлений")
            .setStyle(Notification.BigTextStyle().bigText("$date\n$body"))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(9001, notification)
    }

    private fun buildDailyPlan(
        context: Context,
        prefs: android.content.SharedPreferences,
        categories: Set<String>
    ): List<String> {
        val result = mutableListOf<String>()
        val now = Calendar.getInstance()
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        val todayNoteKey = todayIso

        if ("Мои задачи" in categories) {
            val tasks = try { JSONArray(prefs.getString("care_tasks", "[]")) } catch (_: Exception) { JSONArray() }
            for (i in 0 until tasks.length()) {
                val task = tasks.optJSONObject(i) ?: continue
                if (!task.optBoolean("done", false) && task.optString("date") <= todayIso) {
                    val crop = task.optString("crop").trim()
                    val suffix = if (crop.isNotBlank()) " · $crop" else ""
                    result.add("Задача: ${task.optString("title", "Запланированная работа")}$suffix")
                }
            }
        }

        if ("Мои заметки" in categories) {
            val note = prefs.getString("note_$todayNoteKey", "").orEmpty().trim()
            if (note.isNotBlank()) result.add("Заметка на сегодня: $note")
        }

        val selectedIds = prefs.getStringSet("reminder_crop_ids", null)
        val crops = readCrops(context).filter { selectedIds == null || it.first in selectedIds }
        val selectedNames = crops.map { it.second }
        val zone = prefs.getString("garden_zone", "Центральный") ?: "Центральный"
        val month = adjustedMonth(now.get(Calendar.MONTH), zone)
        val cropExamples = when (month) {
            Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL -> crops
                .filter { it.third in setOf("Овощи", "Корнеплоды", "Капустные", "Тыквенные", "Бобовые", "Луковые", "Зерновые и овощи", "Зелень", "Пряные травы", "Цветы", "Сидераты", "Другое") }
                .take(3).map { it.second }
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER -> crops
                .filter { it.third in setOf("Овощи", "Корнеплоды", "Капустные", "Тыквенные", "Бобовые", "Луковые", "Зерновые и овощи", "Зелень", "Пряные травы", "Цветы", "Сидераты", "Другое", "Ягоды", "Ягоды и лианы", "Плодовые деревья", "Плодовые кустарники") }
                .take(3).map { it.second }
            else -> crops.take(3).map { it.second }
        }
        val cropText = cropExamples.joinToString(", ").ifBlank { selectedNames.take(3).joinToString(", ") }

        if ("Сезонные работы" in categories) result.add(seasonalAdvice(month))
        if ("Посев и рассада" in categories) {
            result.add(
                if (month in listOf(Calendar.DECEMBER, Calendar.JANUARY, Calendar.NOVEMBER)) {
                    "Планируйте семена и сроки посева; ранний посев нужен только культурам с длинным рассадным периодом."
                } else if (cropText.isNotBlank()) {
                    "Проверьте сроки посева и высадки для культур: $cropText. Учитывайте сорт, температуру почвы и прогноз заморозков."
                } else {
                    "Сверьте сроки посева с регионом, сортом, температурой почвы и прогнозом заморозков."
                }
            )
        }
        if ("Уход и полив" in categories) {
            result.add(if (cropText.isNotBlank()) {
                "Осмотрите $cropText и проверьте влажность почвы; полив корректируйте по осадкам и состоянию растений."
            } else "Проверьте влажность почвы и состояние растений; поливайте с учётом осадков.")
        }
        if ("Подкормка" in categories) {
            result.add("Осмотрите растения перед подкормкой. Используйте удобрения только по потребности и строго по инструкции.")
        }
        if ("Защита растений" in categories) {
            result.add("Осмотрите листья с обеих сторон и молодые побеги. При обнаружении вредителей сначала уточните вид проблемы.")
        }
        if ("Сбор урожая" in categories) {
            result.add(if (month in listOf(Calendar.JUNE, Calendar.JULY, Calendar.AUGUST, Calendar.SEPTEMBER, Calendar.OCTOBER)) {
                if (cropText.isNotBlank()) "Проверьте зрелость и готовность к сбору: $cropText. Учитывайте признаки зрелости конкретного сорта."
                else "Проверьте зрелость плодов и ягод; ориентируйтесь на признаки зрелости конкретного сорта."
            } else "Проверьте сроки созревания выбранных культур и заранее подготовьте тару для сбора.")
        }
        if ("Лунная фаза" in categories) result.add(lunarPhase(now) + ". Лунная подсказка традиционная и не заменяет агротехнические сроки.")
        return result
    }

    private fun readCrops(context: Context): List<Triple<String, String, String>> {
        return try {
            val text = context.assets.open("crop-icons/catalog.json").bufferedReader().use { it.readText() }
            val array = JSONObject(text).optJSONArray("crops") ?: JSONArray()
            val builtIns = (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                if (!item.optBoolean("available", true)) return@mapNotNull null
                val id = item.optString("id")
                val name = item.optString("name")
                val category = item.optString("category")
                if (id.isBlank() || name.isBlank()) null else Triple(id, name, category)
            }
            val prefs = context.getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE)
            val customJson = try { JSONArray(prefs.getString("user_crops", "[]")) } catch (_: Exception) { JSONArray() }
            val custom = (0 until customJson.length()).mapNotNull { i ->
                val item = customJson.optJSONObject(i) ?: return@mapNotNull null
                val id = item.optString("id")
                val name = item.optString("name")
                val category = item.optString("category", "Другое")
                if (id.isBlank() || name.isBlank()) null else Triple(id, name, category)
            }
            builtIns + custom
        } catch (_: Exception) { emptyList() }
    }

    private fun adjustedMonth(actualMonth: Int, zone: String): Int {
        if (actualMonth !in Calendar.FEBRUARY..Calendar.SEPTEMBER) return actualMonth
        val offset = when (zone) {
            "Юг" -> 1
            "Северо-Запад", "Урал", "Дальний Восток" -> -1
            "Сибирь", "Север" -> -2
            else -> 0
        }
        return (actualMonth + offset + 12) % 12
    }

    private fun seasonalAdvice(month: Int): String = when (month) {
        Calendar.DECEMBER, Calendar.JANUARY -> "Планируйте сорта и закупку семян. Зимний посев требует подходящих условий и досветки."
        Calendar.FEBRUARY -> "Проверьте сроки посева культур с длинным рассадным периодом и подготовьте ёмкости."
        Calendar.MARCH -> "Планируйте рассаду, обеспечьте достаточное освещение и умеренный полив."
        Calendar.APRIL -> "Подготовьте грядки; посев холодостойких культур сверяйте с состоянием почвы."
        Calendar.MAY -> "Закаливайте рассаду и учитывайте риск возвратных заморозков перед высадкой."
        Calendar.JUNE -> "Следите за влажностью почвы, прореживанием всходов и защитой молодых растений."
        Calendar.JULY -> "Проверяйте полив, собирайте созревшие плоды и планируйте повторные посевы скороспелых культур."
        Calendar.AUGUST -> "Планируйте осенние посевы и регулярно собирайте урожай по мере созревания."
        Calendar.SEPTEMBER -> "Собирайте урожай, готовьте грядки и учитывайте сроки подзимних посевов."
        Calendar.OCTOBER -> "Убирайте участок и готовьте почву; подзимний посев зависит от местной погоды."
        Calendar.NOVEMBER -> "Завершайте уборку участка и планируйте следующий сезон; не сейте в неподходящую почву."
        else -> "Подготовьте участок к зиме и составьте план следующего сезона."
    }

    private fun lunarPhase(date: Calendar): String {
        val knownNewMoon = Calendar.getInstance().apply { set(2000, Calendar.JANUARY, 6, 18, 14, 0) }.timeInMillis
        val synodicMonth = 29.530588853 * 24 * 60 * 60 * 1000
        val age = ((((date.timeInMillis - knownNewMoon) % synodicMonth) + synodicMonth) % synodicMonth) / synodicMonth
        return when {
            age < 0.035 || age >= 0.965 -> "Фаза Луны: новолуние"
            age < 0.285 -> "Фаза Луны: растущая Луна"
            age < 0.535 -> "Фаза Луны: полнолуние"
            else -> "Фаза Луны: убывающая Луна"
        }
    }

    companion object {
        const val ACTION_TEST = "ru.goodrawru.myposev.ACTION_TEST_REMINDER"
        private val DEFAULT_CATEGORIES = setOf(
            "Сезонные работы", "Посев и рассада", "Уход и полив", "Подкормка",
            "Защита растений", "Сбор урожая", "Лунная фаза", "Мои задачи", "Мои заметки"
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderIntent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, 91, reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prefs = context.getSharedPreferences("moi_posevnoy", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("reminder_enabled", true)) {
            alarm.cancel(pending)
            return
        }
        val hour = prefs.getInt("reminder_hour", 9).coerceIn(0, 23)
        val minute = prefs.getInt("reminder_minute", 0).coerceIn(0, 59)
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }
}
