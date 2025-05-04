package com.example.chovypilled.amadeus_kt

import android.Manifest
import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    val voiceLines = VoiceLine.Line.getLines()
    private lateinit var speak: (line: VoiceLine, activity: MainActivity) -> Unit
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var sr: SpeechRecognizer
    private lateinit var srl: RecognitionListener
    private lateinit var settings: SharedPreferences
    private lateinit var recogLang: String
    private lateinit var contextLang: List<String>

    private fun chooseSpeakFunc(granted: Boolean) {
        speak = if (granted) {
            { line, activity ->
                Amadeus.speakVisualizer(line, activity)
            }
        } else {
            { line, activity ->
                Amadeus.speak(line, activity)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val kurisu: ImageView = findViewById(R.id.imageView_kurisu)
        val subBackground: ImageView = findViewById(R.id.imageView_subtitles)
        val logo: ImageView = findViewById(R.id.imageView_logo_small)
        settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
        recogLang = settings.getString("recognition_lang", "ja-JP").toString()
        contextLang = recogLang.split("-")

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            chooseSpeakFunc(isGranted)
            if (isGranted) {
                recreate()
            } else {
                Log.d("MainActivity", "Permission not granted")
                Amadeus.speak(voiceLines[VoiceLine.Line.SORRY]!!, this)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            chooseSpeakFunc(true)
            sr = SpeechRecognizer.createSpeechRecognizer(this)!!
            srl = AmadeusListener()
            sr.setRecognitionListener(srl)

        }

        // if settings value is changed
        settings.registerOnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == "show_subtitles") {
                if (sharedPreferences.getBoolean(key, true)) {
                    Log.d("MainActivity", "Subtitles enabled")
                    subBackground.visibility = View.VISIBLE
                    recreate()
                } else {
                    Log.d("MainActivity", "Subtitles disabled")
                    subBackground.visibility = View.INVISIBLE
                    recreate()

                }
            }
        }

        val rng = Random(System.currentTimeMillis())
        Amadeus.speak(voiceLines[VoiceLine.Line.HELLO]!!, this)

        val handler = Handler(Looper.getMainLooper())
        val loop: Runnable = object : Runnable {
            override fun run() {
                if (Amadeus.isLooping) {
                    Log.d("MainActivity", "Looping")
                        speak(voiceLines[rng.nextInt(voiceLines.size)]!!, this@MainActivity)
                        handler.postDelayed(this, ((5000 + rng.nextInt(5) * 1000).toLong()))
                }
            }
        }

        kurisu.setOnLongClickListener {
            Log.d("MainActivity", "Long click on Kurisu")
            if (!Amadeus.isLooping && !Amadeus.isSpeaking) {
                handler.post(loop)
                Amadeus.isLooping = true
                Log.d("MainActivity", "Looping started")
            } else {
                Log.d("MainActivity", "Looping stopped")
                handler.removeCallbacks(loop)
                Amadeus.isLooping = false
            }
            true
        }

        kurisu.setOnClickListener { view ->
            Log.d("MainActivity", "Click on Kurisu")
            val host = view.context as MainActivity
            val permissionCheck = ContextCompat.checkSelfPermission(
                host,
                Manifest.permission.RECORD_AUDIO
            )

            if (!Amadeus.isLooping && !Amadeus.isSpeaking) {
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Permission granted")
                    promptSpeech()
                } else {
                    Log.d("MainActivity", "Permission not granted")
                    speak(voiceLines[VoiceLine.Line.DAGA_KOTOWARU]!!, this)
                }
            }
        }

        logo.setOnClickListener {
            Log.d("MainActivity", "Click on logo")
        }

        logo.setOnLongClickListener {
            Log.d("MainActivity", "Long click on logo")
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }

        subBackground.setOnClickListener {
            Log.d("MainActivity", "Click on subtitles")
        }

        subBackground.setOnLongClickListener {
            Log.d("MainActivity", "Long click on subtitles")
            true
        }
    }

    private fun promptSpeech() {
        if (!::sr.isInitialized && !::srl.isInitialized) {
            sr = SpeechRecognizer.createSpeechRecognizer(this)
            srl = AmadeusListener()
            sr.setRecognitionListener(srl)
        }

        speak(voiceLines[VoiceLine.Line.ASK_ME]!!, this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recogLang)
        sr.startListening(intent)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK && data != null) {
                val context = LanguageContext.load(applicationContext, contextLang[0])
                val input = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                Amadeus.respond(input!![0], context, this)
            }
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageContext.wrap(it) })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sr.isInitialized) {
            sr.destroy()
        }
        Amadeus.player.release()
    }

    override fun onStop() {
        super.onStop()
        Amadeus.isLooping = false
    }

    override fun onPause() {
        super.onPause()
        Amadeus.isLooping = false
    }

    override fun onResume() {
        super.onResume()
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())

        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val subBackground:ImageView = findViewById(R.id.imageView_subtitles)
        if (settings.getBoolean("show_subtitles", true)) {
            subBackground.visibility = View.VISIBLE
        } else {
            subBackground.visibility = View.INVISIBLE
        }
    }

    inner class AmadeusListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("AmadeusListener", "Speech recognition ready")
        }

        override fun onBeginningOfSpeech() {
            Log.d("AmadeusListener", "Listening to speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            Log.d("AmadeusListener", "RMS changed: $rmsdB")
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.d("AmadeusListener", "Buffer received")
        }

        override fun onEndOfSpeech() {
            Log.d("AmadeusListener", "End of speech")
        }

        override fun onError(error: Int) {
            Log.wtf("AmadeusListener", "Error: $error")
            sr.cancel()
            speak(voiceLines[VoiceLine.Line.SORRY]!!, this@MainActivity)
        }

        override fun onResults(results: Bundle?) {
            var input = ""
            var debug = ""

            val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (data != null) {
                for (word: String in data) {
                    debug += word + "\n"
                }

                Log.d("AmadeusListener", "Results: $debug")
                input = data[0]

                var split: List<String> = input.split(" ")
                val context = LanguageContext.load(applicationContext, contextLang[0])
                if (split.size > 2 && split[0].lowercase() == (context.getString(R.string.assistant))) {
                    val cmd = split[1].lowercase()
                    val query = arrayOfNulls<String>(split.size - 2)
                    System.arraycopy(split, 2, query, 0, split.size - 2)

                    if (cmd.contains(context.getString(R.string.open))) {
                        Amadeus.openApp(query, this@MainActivity)
                    }
                } else {
                    Amadeus.respond(input, context, this@MainActivity)
                }
            } else {
                Log.d("AmadeusListener", "No results found")
            }
            Log.d("AmadeusListener", "Results received")
        }

        override fun onPartialResults(partialResults: Bundle?) {
            Log.d("AmadeusListener", "Partial results received")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d("AmadeusListener", "Event received: $eventType")
        }

    }
}

