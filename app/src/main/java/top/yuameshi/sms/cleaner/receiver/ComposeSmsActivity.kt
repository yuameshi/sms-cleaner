package top.yuameshi.sms.cleaner.receiver

import android.content.Intent
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yuameshi.sms.cleaner.ui.theme.SMSCleanerTheme

class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            // TODO: Implement SMS sending
                            finish()
                        },
                        onCancel = {
                            finish()
                        }
                    )
                }
            }
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
