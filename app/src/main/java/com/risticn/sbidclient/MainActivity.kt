package com.risticn.sbidclient

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.risticn.sbidclient.ui.theme.SBIDClientTheme
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {

    // CHANGE THIS TO YOUR WINDOWS IP
    private val serverUrl =
     //   "http://10.40.102.29:8080/api/v1/register"
        "http://10.100.1.141:8080/api/v1/register"

    private val validateUrl =
    //    "http://10.40.102.29:8080/api/v1/validate"
        "http://10.100.1.141:8080/api/v1/validate"

    private val heartbeatUrl =
    //    "http://10.40.102.29:8080/api/v1/heartbeat"
        "http://10.100.1.141:8080/api/v1/heartbeat"
    private fun validateToken(
        token: String
    ): Boolean {

        return try {

            val connection =
                URL(validateUrl)
                    .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.doOutput = true

            val payload = JSONObject()

            payload.put(
                "token",
                token
            )

            OutputStreamWriter(
                connection.outputStream
            ).use {
                it.write(payload.toString())
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .readText()

            val json =
                JSONObject(response)

            json.getBoolean("valid")

        } catch (e: Exception) {

            false
        }
    }

    private fun sendHeartbeat(
        token: String
    ): Boolean {

        return try {

            val connection =
                URL(heartbeatUrl)
                    .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            connection.doOutput = true

            val payload = JSONObject()

            payload.put(
                "token",
                token
            )

            OutputStreamWriter(
                connection.outputStream
            ).use {
                it.write(payload.toString())
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .readText()

            val json =
                JSONObject(response)

            val success =
                json.getBoolean("success")

            println("HEARTBEAT RESULT = $success")

            success

        } catch (e: Exception) {

            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SBIDClientTheme {

                var statusText by remember {
                    mutableStateOf("Checking registration...")
                }

                LaunchedEffect(Unit) {

                    val prefs =
                        getSharedPreferences(
                            "sbid",
                            MODE_PRIVATE
                        )

                    val token =
                        prefs.getString(
                            "device_token",
                            null
                        )

                    if (token != null) {

                        Thread {

                            val valid =
                                validateToken(token)

                            if (valid) {

                                sendHeartbeat(token)

                                val deviceId =
                                    prefs.getInt(
                                        "device_id",
                                        -1
                                    )

                                runOnUiThread {

                                    statusText =
                                        "REGISTERED\n\nDevice ID: $deviceId"
                                }

                            } else {

                            prefs.edit()
                                .remove("device_token")
                                .remove("device_id")
                                .apply()

                            statusText =
                                "TOKEN INVALID - PLEASE REGISTER"
                            }

                        }.start()

                    } else {

                        statusText =
                            "NOT REGISTERED"
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Button(
                            onClick = {

                                Thread {

                                    try {

                                        val androidId =
                                            Settings.Secure.getString(
                                                contentResolver,
                                                Settings.Secure.ANDROID_ID
                                            )

                                        val connection =
                                            URL(serverUrl)
                                                .openConnection() as HttpURLConnection

                                        connection.requestMethod = "POST"
                                        connection.setRequestProperty(
                                            "Content-Type",
                                            "application/json"
                                        )

                                        connection.doOutput = true

                                        val payload = JSONObject()

                                        payload.put(
                                            "android_id",
                                            androidId
                                        )

                                        payload.put(
                                            "device_name",
                                            android.os.Build.MODEL
                                        )

                                        payload.put(
                                            "app_version",
                                            "1.0"
                                        )

                                        OutputStreamWriter(
                                            connection.outputStream
                                        ).use {
                                            it.write(payload.toString())
                                        }

                                        val response =
                                            connection.inputStream
                                                .bufferedReader()
                                                .readText()

                                        val json =
                                            JSONObject(response)

                                        val token =
                                            json.getString("token")

                                        val deviceId =
                                            json.getInt("device_id")

                                        val prefs =
                                            getSharedPreferences(
                                                "sbid",
                                                MODE_PRIVATE
                                            )

                                        prefs.edit()
                                            .putString(
                                                "device_token",
                                                token
                                            )
                                            .putInt(
                                                "device_id",
                                                deviceId
                                            )
                                            .apply()

                                        runOnUiThread {
                                            statusText =
                                                "SUCCESS\n\nToken:\n$token"
                                        }

                                    } catch (e: Exception) {

                                        runOnUiThread {
                                            statusText =
                                                "ERROR:\n${e.message}"
                                        }
                                    }

                                }.start()
                            }
                        ) {
                            Text("Register Device")
                        }

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Text(
                            text = statusText,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}