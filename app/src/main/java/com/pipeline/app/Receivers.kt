package com.pipeline.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
class ScheduleReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) { WorkflowEngine.start(context); WorkflowEngine.schedule(context) } }
class UnlockReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) = WorkflowEngine.resumeAfterUnlock(context) }
class BootReceiver : BroadcastReceiver() { override fun onReceive(context: Context, intent: Intent) = WorkflowEngine.schedule(context) }
