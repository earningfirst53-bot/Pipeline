package com.pipeline.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(state: Bundle?) { super.onCreate(state); if(android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS); setContent{Screen()} }
    @Composable private fun Screen() {
        var config by remember { mutableStateOf(WorkflowEngine.config(this)) }
        var prompt by remember { mutableStateOf(config.prompt) }; var wait by remember { mutableStateOf(config.waitSeconds.toString()) }
        var hour by remember { mutableStateOf(config.hour.toString().padStart(2,'0')) }; var minute by remember { mutableStateOf(config.minute.toString().padStart(2,'0')) }
        var status by remember { mutableStateOf(WorkflowEngine.stage(this)) }; var log by remember { mutableStateOf(WorkflowEngine.log(this)) }
        fun refresh(){status=WorkflowEngine.stage(this);log=WorkflowEngine.log(this)}
        MaterialTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.padding(20.dp).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Pipeline",style=MaterialTheme.typography.headlineMedium); Text("Daily Flow image workflow",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(prompt,{prompt=it},Modifier.fillMaxWidth(),label={Text("Flow prompt")})
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(wait,{wait=it.filter(Char::isDigit)},Modifier.weight(1f),label={Text("Wait seconds")});OutlinedTextField(hour,{hour=it.filter(Char::isDigit).take(2)},Modifier.weight(1f),label={Text("Hour")});OutlinedTextField(minute,{minute=it.filter(Char::isDigit).take(2)},Modifier.weight(1f),label={Text("Minute")})}
            Button({config=WorkflowConfig(prompt,wait.toIntOrNull()?:30,hour.toIntOrNull()?:9,minute.toIntOrNull()?:0);WorkflowEngine.saveConfig(this@MainActivity,config);refresh()},Modifier.fillMaxWidth()){Text("Save daily workflow")}
            Button({WorkflowEngine.start(this@MainActivity);refresh()},Modifier.fillMaxWidth()){Text("Run now")}
            OutlinedButton({startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))},Modifier.fillMaxWidth()){Text("Enable Pipeline accessibility")}
            OutlinedButton(::refresh,Modifier.fillMaxWidth()){Text("Refresh status")}
            Text("Status: "+status.name.replace('_',' '),style=MaterialTheme.typography.titleSmall); Text(log,style=MaterialTheme.typography.bodySmall)
        } } }
    }
}
