package ru.goodrawru.myposev

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent?) {
        val openApp=PendingIntent.getActivity(context,92,Intent(context,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) Notification.Builder(context,"posev_reminders") else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val prefs=context.getSharedPreferences("moi_posevnoy",Context.MODE_PRIVATE)
        val today=java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.US).format(java.util.Date())
        val tasks=try { org.json.JSONArray(prefs.getString("care_tasks","[]")) } catch (_:Exception) { org.json.JSONArray() }
        val due=mutableListOf<String>()
        for(i in 0 until tasks.length()) {
            val item=tasks.optJSONObject(i) ?: continue
            if(!item.optBoolean("done",false) && item.optString("date") <= today) {
                due.add(item.optString("title","Задача"))
            }
        }
        val message=when {
            due.isEmpty() -> "Проверьте план работ в саду и огороде на сегодня."
            due.size==1 -> "На сегодня: " + due.first()
            else -> "На сегодня и ранее: " + due.first() + " и ещё " + (due.size-1)
        }
        val notification=builder.setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Мой Посевной")
            .setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(if(due.isEmpty()) message else due.joinToString("\n")))
            .setContentIntent(openApp).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(9001,notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent?) {
        if(intent?.action!=Intent.ACTION_BOOT_COMPLETED) return
        val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderIntent=Intent(context,ReminderReceiver::class.java)
        val pending=PendingIntent.getBroadcast(context,91,reminderIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val c=java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY,9); set(java.util.Calendar.MINUTE,0); set(java.util.Calendar.SECOND,0)
            if(before(java.util.Calendar.getInstance())) add(java.util.Calendar.DAY_OF_YEAR,1)
        }
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,c.timeInMillis,AlarmManager.INTERVAL_DAY,pending)
    }
}
