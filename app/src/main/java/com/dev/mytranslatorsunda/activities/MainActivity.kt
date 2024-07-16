package com.dev.mytranslatorsunda.activities

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechRecognitionAlternative
import com.google.cloud.speech.v1.SpeechRecognitionResult
import com.google.cloud.speech.v1.SpeechSettings
import com.dev.mytranslatorsunda.R
import com.dev.mytranslatorsunda.model.ModelLanguage
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.cloud.translate.Translation

import com.google.protobuf.ByteString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.util.Locale

class MainActivity : ComponentActivity() {
    companion object {
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 123
    }

    //Variabel Speech to Text
    private lateinit var speechClient: SpeechClient
    private lateinit var btnSpeech: Button

    //Variabel Translate text
    private lateinit var teksInput: EditText
    private lateinit var btnTerjemahkan: Button
    private lateinit var teksTerjemahan: TextView

    //Variabel Switch Bahasa
    private lateinit var switchBahasa: Switch
    private lateinit var bahasaAsal: Button
    private lateinit var bahasaTujuan: Button

    //variabel Button Tingkatan
    private lateinit var btnKasar: Button
    private lateinit var btnBiasa: Button
    private lateinit var btnHalus: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Switch Bahasa
        bahasaAsal  = findViewById(R.id.bahasaAsal)
        bahasaTujuan = findViewById(R.id.bahasaTujuan)
        switchBahasa = findViewById(R.id.switchBahasa)

        //Button Tingkatan
        btnKasar = findViewById(R.id.btnKasar)
        btnBiasa = findViewById(R.id.btnBiasa)
        btnHalus = findViewById(R.id.btnHalus)

        switchBahasa.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                bahasaAsal.text = "Bahasa Sunda"
                bahasaTujuan.text = "Bahasa Indonesia"
                btnKasar.alpha = 0.0f
                btnBiasa.alpha = 0.0f
                btnHalus.alpha = 0.0f
                btnKasar.setClickable(false)
                btnBiasa.setClickable(false)
                btnHalus.setClickable(false)
            } else {
                bahasaAsal.text = "Bahasa Indonesia"
                bahasaTujuan.text = "Bahasa Sunda"
                btnKasar.alpha = 1.0f
                btnBiasa.alpha = 1.0f
                btnHalus.alpha = 1.0f
                btnKasar.setClickable(true)
                btnBiasa.setClickable(true)
                btnHalus.setClickable(true)
            }
        }

        //Translate
        teksInput = findViewById(R.id.teksInput)
        teksTerjemahan = findViewById(R.id.teksTerjemahan)
        btnTerjemahkan = findViewById(R.id.btnTerjemahkan)

        btnTerjemahkan.setOnClickListener {
            translateText()
        }

        //Speech to Text
        teksInput = findViewById(R.id.teksInput)
        btnSpeech = findViewById(R.id.btnSpeech)

        btnSpeech.setOnClickListener {
        }
    }

    private fun translateText() {
        // Set up Google Cloud Translation API credentials
        val credentials = GoogleCredentials.fromStream(FileInputStream("credentials.json"))
        val translate = TranslateOptions.newBuilder()
            .setCredentials(credentials)
            .build()
            .service

        val defaultLanguage = "id"

        val sourceLanguage = when (bahasaAsal.text) {
            "Bahasa Sunda" -> "su"
            "Bahasa Indonesia" -> "id"
            else -> defaultLanguage
        }
        val targetLanguage = when (bahasaTujuan.text) {
            "Bahasa Sunda" -> "su"
            "Bahasa Indonesia" -> "id"
            else -> defaultLanguage
        }

        val text = teksInput.text.toString()
        val translation = translate.translate(text, Translate.TranslateOption.sourceLanguage(sourceLanguage),
            Translate.TranslateOption.targetLanguage(targetLanguage))
        teksTerjemahan.text = translation.translatedText
    }
}

