package com.ttechsoft.okhttp_callable

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth

    private var user: FirebaseUser? = null
    private var idToken: String? = null
    private var instanceId: String? = null

    private lateinit var callableButton: Button
    private lateinit var okHttpButton: Button

    private lateinit var loginText: TextView
    private lateinit var idTokenText: TextView
    private lateinit var callableResultText: TextView
    private lateinit var okHttpResultText: TextView
    private lateinit var instanceIdText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupView()
        setupListeners()

        functions = FirebaseFunctions.getInstance()
        auth = FirebaseAuth.getInstance()

        enableButtons()

        user = auth.currentUser
        if (user == null) {
            logInAsAnonymousUser()
        } else {
            loginText.text = "OK"
            fetchIdToken()
        }
        fetchInstanceId()
    }

    private fun setupView() {
        callableButton = findViewById(R.id.buttonCallable)
        okHttpButton = findViewById(R.id.buttonCallable)

        loginText = findViewById(R.id.textLogin)
        idTokenText = findViewById(R.id.textUserIdToken)
        callableResultText = findViewById(R.id.textCallableResult)
        okHttpResultText = findViewById(R.id.textOkHttpResult)
        instanceIdText = findViewById(R.id.textInstanceId)
    }

    private fun setupListeners() {
        buttonCallable.setOnClickListener {
            callWithCallable(FUNCTION_NAME)
        }

        buttonOkHttp.setOnClickListener {
            callWithOkHttp(FUNCTION_NAME)
        }
    }

    private fun logInAsAnonymousUser() {
        auth.signInAnonymously()
            .addOnCompleteListener { task ->
                runOnUiThread {
                    loginText.text = if (task.isSuccessful) "OK" else "NG"
                }

                if (task.isSuccessful) {
                    user = task.result?.user
                    enableButtons()
                    fetchIdToken()

                }
                else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to sign in.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun fetchIdToken() {
        val user = user ?: return

        user.getIdToken(false)
            .addOnCompleteListener {task ->
                runOnUiThread {
                    idTokenText.text = if (task.isSuccessful) "OK" else "NG"
                }

                if (task.isSuccessful) {
                    idToken = task.result?.token
                    enableButtons()
                }
                else {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to sign in.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun fetchInstanceId() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            runOnUiThread {
                instanceIdText.text = if (task.isSuccessful) "OK" else "NG"
            }

            if (task.isSuccessful) {
                instanceId = task.result?.token
                enableButtons()
            }
            else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to sign in.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun enableButtons() {
        buttonCallable.isEnabled = (user != null)

        buttonOkHttp.isEnabled = (user != null) && (idToken != null) && (instanceId != null)
    }

    private fun callWithCallable(functionName: String) {
        textCallableResult.text = ""
        buttonCallable.isEnabled = false
        Log.i(TAG, "Start callable")
        functions
            .getHttpsCallable(functionName)
            .call(mapOf("text" to "inputText"))
            .addOnCompleteListener {task->
                enableButtons()
                if (!task.isSuccessful) {
                    val e = task.exception

                    runOnUiThread {
                        textCallableResult.text = e?.message
                    }

                    if (e is FirebaseFunctionsException) {
                        val code = e.code
                        val details = e.details

                        Log.e(TAG, "Functions error: $code, $details")
                    } else {
                        Log.e(TAG, "Error: ${Log.getStackTraceString(e)}")
                    }
                    return@addOnCompleteListener
                }
                runOnUiThread {
                    textCallableResult.text = "OK"
                }
                val result = task.result?.data.toString()
                Log.i(TAG, result)

            }
    }

    private fun callWithOkHttp(functionName: String) {
        val idToken = idToken ?: return
        val instanceId = instanceId ?: return
        val projectId = FirebaseApp.getInstance()?.options?.projectId ?: return
        val url = "https://us-central1-$projectId.cloudfunctions.net/$functionName"

        val jsonData = JSONObject()
        jsonData.put("text", "inputText")

        val json = JSONObject()
        json.put("data", jsonData)

        val requestBody = RequestBody.create(JSON, json.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("Firebase-Instance-ID-Token", instanceId)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(1 , TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .writeTimeout(1, TimeUnit.MINUTES)
            .build()

        Log.i(TAG, "Start Okhttp")
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val message = response.body()?.string() ?: "Network Error"
                    runOnUiThread {
                        textOkHttpResult.text = message
                    }

                    return
                }

                runOnUiThread {
                    textOkHttpResult.text = "OK"
                }

                val responseBody = response.body()
                Log.i(TAG, responseBody?.string())
            }

            override fun onFailure(call: Call, e: IOException) {
                val message = e.message ?: "Unknown Network error"
                runOnUiThread {
                    textOkHttpResult.text = message
                }
            }
        })

    }

    companion object {
        private const val TAG = "OkHttpCallable"
        private const val FUNCTION_NAME = "helloWorldOnCall"
        private val JSON = MediaType.parse("application/json; charset=utf-8")
    }
}
