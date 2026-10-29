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
        val notification=builder.setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Мой Посевной")
            .setContentText("Проверьте план работ в саду и огороде на сегодня.")
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
