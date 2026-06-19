package top.yuameshi.sms.cleaner.receiver

import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yuameshi.sms.cleaner.ui.theme.SMSCleanerTheme
import top.yuameshi.sms.cleaner.util.PermissionUtils

class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if app is default SMS app
        if (!PermissionUtils.isDefaultSmsApp(this)) {
            Toast.makeText(this, "需要设置为默认短信App才能发送短信", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val address = intent?.data?.schemeSpecificPart ?: ""
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""

        setContent {
            SMSCleanerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ComposeSmsScreen(
                        address = address,
                        body = body,
                        onSend = { sendAddress, sendBody ->
                            sendSms(sendAddress, sendBody)
                        },
                        onCancel = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun sendSms(address: String, body: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(address, null, body, null, null)
            Toast.makeText(this, "短信已发送", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "发送失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeSmsScreen(
    address: String,
    body: String,
    onSend: (address: String, body: String) -> Unit,
    onCancel: () -> Unit
) {
    var sendAddress by remember { mutableStateOf(address) }
    var sendBody by remember { mutableStateOf(body) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发送短信") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Text("取消")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onSend(sendAddress, sendBody) },
                        enabled = sendAddress.isNotEmpty() && sendBody.isNotEmpty()
                    ) {
                        Text("发送")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = sendAddress,
                onValueChange = { sendAddress = it },
                label = { Text("收件人") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = sendBody,
                onValueChange = { sendBody = it },
                label = { Text("短信内容") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE
            )
        }
    }
}
