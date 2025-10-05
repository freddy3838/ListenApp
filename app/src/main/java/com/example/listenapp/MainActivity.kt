import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private val PERMISSION_REQUEST_CODE = 100
    private val folderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/MyImages"
    private var imageNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }
        val textView = TextView(this).apply {
            text = "Écoute en cours..."
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(textView)
        setContentView(layout)

        if (checkPermissions()) {
            loadImageNames()
            startListening()
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadImageNames()
                startListening()
            } else {
                Toast.makeText(this, "Permissions refusées", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun loadImageNames() {
        val folder = File(folderPath)
        if (folder.exists()) {
            imageNames.clear()
            folder.listFiles { _, name -> name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) }?.forEach {
                imageNames.add(it.name)
            }
            if (imageNames.isEmpty()) {
                Toast.makeText(this, "Aucune image trouvée dans $folderPath", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Dossier non trouvé : $folderPath. Créez-le et ajoutez des images.", Toast.LENGTH_LONG).show()
        }
    }

    private fun startListening() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { startListening() }
            override fun onError(error: Int) { startListening() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    checkForImageName(matches[0].lowercase())
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partialMatches.isNullOrEmpty()) {
                    checkForImageName(partialMatches[0].lowercase())
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer.startListening(intent)
    }

    private fun checkForImageName(text: String) {
        for (name in imageNames) {
            val cleanName = name.lowercase().replace(".jpg", "").replace(".png", "").replace(".jpeg", "")
            if (text.contains(cleanName)) {
                displayImage(name)
                speechRecognizer.destroy()
                return
            }
        }
    }

    private fun displayImage(imageName: String) {
        val imageFile = File(folderPath, imageName)
        if (imageFile.exists()) {
            val uri = Uri.fromFile(imageFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } else {
            Toast.makeText(this, "Image non trouvée", Toast.LENGTH_SHORT).show()
        }
    }
}
