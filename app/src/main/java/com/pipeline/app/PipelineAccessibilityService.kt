package com.pipeline.app

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class PipelineAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var queued = false
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        when (WorkflowEngine.stage(this)) {
            RunStage.WAITING_FOR_NEW -> find(root,"new")?.let { if(click(it)) WorkflowEngine.setStage(this,RunStage.WAITING_FOR_PROMPT,"New pressed. Looking for the prompt field.") }
            RunStage.WAITING_FOR_PROMPT -> prompt(root)
            RunStage.WAITING_FOR_IMAGE -> find(root,"image")?.let { if(click(it)) WorkflowEngine.setStage(this,RunStage.WAITING_FOR_DOWNLOAD,"Generated image opened. Looking for Download.") }
            RunStage.WAITING_FOR_DOWNLOAD -> find(root,"download")?.let { if(click(it)) WorkflowEngine.complete(this) }
            else -> Unit
        }
    }
    override fun onInterrupt() = Unit
    private fun prompt(root: AccessibilityNodeInfo) {
        val field=find(root,"write prompt") ?: find(root,"prompt") ?: return
        field.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val a=Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,WorkflowEngine.config(this@PipelineAccessibilityService).prompt) }
        if(!field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,a)) return
        val send=find(root,"arrow") ?: find(root,"generate") ?: find(root,"send") ?: return
        if(click(send)) { WorkflowEngine.waitForImage(this); if(!queued) { queued=true; handler.postDelayed({queued=false; onAccessibilityEvent(null)},WorkflowEngine.config(this).waitSeconds*1000L) } }
    }
    private fun find(root: AccessibilityNodeInfo, wanted: String): AccessibilityNodeInfo? {
        val q=ArrayDeque<AccessibilityNodeInfo>(); q.add(root)
        while(q.isNotEmpty()) { val n=q.removeFirst(); val value=listOf(n.text,n.contentDescription,n.hintText).filterNotNull().joinToString(" ").lowercase(); if(value.contains(wanted.lowercase())) return n; repeat(n.childCount) { n.getChild(it)?.let(q::add) } }
        return null
    }
    private fun click(node: AccessibilityNodeInfo): Boolean { var current:AccessibilityNodeInfo?=node; repeat(6) { val n=current ?: return false; if(n.isClickable&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true; current=n.parent }; return false }
}
