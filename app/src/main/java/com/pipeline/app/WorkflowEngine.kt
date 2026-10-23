package com.pipeline.app

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

enum class RunStage { IDLE, PAUSED_LOCKED, OPENING_FLOW, WAITING_FOR_NEW, WAITING_FOR_PROMPT, WAITING_FOR_IMAGE, WAITING_FOR_DOWNLOAD, COMPLETE, FAILED }
data class WorkflowConfig(val prompt: String, val waitSeconds: Int, val hour: Int, val minute: Int)

object WorkflowEngine {
    private const val P = "pipeline"; private const val CHANNEL = "pipeline_runs"
    fun config(c: Context): WorkflowConfig { val p = c.getSharedPreferences(P, 0); return WorkflowConfig(p.getString("prompt", "a small child smiles") ?: "a small child smiles", p.getInt("wait",30).coerceIn(5,300), p.getInt("hour",9).coerceIn(0,23), p.getInt("minute",0).coerceIn(0,59)) }
    fun saveConfig(c: Context, v: WorkflowConfig) { c.getSharedPreferences(P,0).edit().putString("prompt",v.prompt.trim()).putInt("wait",v.waitSeconds.coerceIn(5,300)).putInt("hour",v.hour.coerceIn(0,23)).putInt("minute",v.minute.coerceIn(0,59)).apply(); schedule(c) }
    fun stage(c: Context): RunStage = runCatching { RunStage.valueOf(c.getSharedPreferences(P,0).getString("stage",RunStage.IDLE.name) ?: RunStage.IDLE.name) }.getOrDefault(RunStage.IDLE)
    fun log(c: Context) = c.getSharedPreferences(P,0).getString("log","No runs yet.") ?: "No runs yet."
    fun setStage(c: Context, s: RunStage, text: String) { val entry = java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.US).format(java.util.Date()) + " — " + text; val old = log(c).takeLast(5000); c.getSharedPreferences(P,0).edit().putString("stage",s.name).putString("log",if(old=="No runs yet.") entry else old+"\n"+entry).apply() }
    fun start(c: Context) { if (c.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { setStage(c,RunStage.PAUSED_LOCKED,"Phone is locked. Run paused until it is unlocked."); notify(c,"Pipeline paused","Unlock the phone to continue the Flow run.",true); return }; setStage(c,RunStage.OPENING_FLOW,"Starting Flow image workflow."); openFlow(c) }
    fun resumeAfterUnlock(c: Context) { if(stage(c)==RunStage.PAUSED_LOCKED && !c.getSystemService(KeyguardManager::class.java).isKeyguardLocked) { setStage(c,RunStage.OPENING_FLOW,"Phone unlocked. Resuming automatically."); openFlow(c) } }
    private fun openFlow(c: Context) { val pm=c.packageManager; val app=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).firstOrNull { it.loadLabel(pm).toString().trim().equals("Flow",true) }; if(app==null) { fail(c,"Flow could not be found. Install the Flow app and keep its name as Flow."); return }; runCatching { c.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(app.activityInfo.packageName,app.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { fail(c,"Flow could not be opened."); return }; setStage(c,RunStage.WAITING_FOR_NEW,"Flow opened. Looking for New.") }
    fun waitForImage(c: Context) = setStage(c,RunStage.WAITING_FOR_IMAGE,"Prompt submitted. Waiting "+config(c).waitSeconds+" seconds for the image.")
    fun complete(c: Context) { setStage(c,RunStage.COMPLETE,"Download pressed. Image should now be visible in Gallery."); notify(c,"Pipeline complete","The Flow image was downloaded to your Gallery.",true) }
    fun fail(c: Context, reason: String) { setStage(c,RunStage.FAILED,reason); notify(c,"Pipeline needs attention",reason,true) }
    fun schedule(c: Context) { val v=config(c); val whenRun=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,v.hour); set(Calendar.MINUTE,v.minute); set(Calendar.SECOND,0); if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }; val pending=PendingIntent.getBroadcast(c,91,Intent(c,ScheduleReceiver::class.java).setAction("com.pipeline.app.RUN_FLOW"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE); val alarm=c.getSystemService(AlarmManager::class.java); if(Build.VERSION.SDK_INT<Build.VERSION_CODES.S||alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenRun.timeInMillis,pending) else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,whenRun.timeInMillis,pending) }
    private fun notify(c: Context,title:String,text:String,alert:Boolean) { val m=c.getSystemService(NotificationManager::class.java); if(Build.VERSION.SDK_INT>=26)m.createNotificationChannel(NotificationChannel(CHANNEL,"Pipeline workflow",NotificationManager.IMPORTANCE_DEFAULT)); m.notify(7,NotificationCompat.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setOnlyAlertOnce(!alert).build()) }
}
