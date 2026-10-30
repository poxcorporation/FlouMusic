package com.flou.music.ui.equalizer

import android.content.Intent
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flou.music.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial

class EqualizerActivity : AppCompatActivity() {
    
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private lateinit var layout: LinearLayout
    private lateinit var switchEqualizer: SwitchMaterial
    private lateinit var seekBassBoost: SeekBar
    private lateinit var seekVirtualizer: SeekBar
    private lateinit var seekBalance: SeekBar
    private lateinit var chipGroupPresets: ChipGroup
    private lateinit var toggleAudioMode: MaterialButtonToggleGroup
    
    private var audioSessionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equalizer)
        
        audioSessionId = intent.getIntExtra("AUDIO_SESSION_ID", 0)
        
        layout = findViewById(R.id.equalizerLayout)
        switchEqualizer = findViewById(R.id.switchEqualizer)
        seekBassBoost = findViewById(R.id.seekBassBoost)
        seekVirtualizer = findViewById(R.id.seekVirtualizer)
        seekBalance = findViewById(R.id.seekBalance)
        chipGroupPresets = findViewById(R.id.chipGroupPresets)
        toggleAudioMode = findViewById(R.id.toggleAudioMode)
        
        initAudioEffects()
        setupEqualizerControls()
        setupBassBoost()
        setupVirtualizer()
        setupBalance()
        setupPresets()
        setupAudioMode()
    }

    private fun initAudioEffects() {
        try {
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)
        } catch (e: Exception) {
            Toast.makeText(this, "Ecualizador no disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEqualizerControls() {
        // CORRECCIÓN: Convertir numberOfBands a Int
        val bands = equalizer?.numberOfBands?.toInt() ?: return
        val minLevel = equalizer?.bandLevelRange?.get(0)?.toInt() ?: -1500
        val maxLevel = equalizer?.bandLevelRange?.get(1)?.toInt() ?: 1500

        layout.removeAllViews()

        for (i in 0 until bands) {
            val bandLevel = equalizer?.getBandLevel(i.toShort())?.toInt() ?: 0
            val centerFreq = equalizer?.getCenterFreq(i.toShort()) ?: 1000

            val bandLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val textView = TextView(this).apply {
                text = "${centerFreq / 1000}Hz"
                textSize = 12f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }

            val seekBar = SeekBar(this).apply {
                max = maxLevel - minLevel
                progress = bandLevel - minLevel
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    200
                )
                rotation = 270f // Vertical

                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            try {
                                equalizer?.setBandLevel(i.toShort(), (progress + minLevel).toShort())
                            } catch (e: Exception) {
                                // Ignorar errores
                            }
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val valueText = TextView(this).apply {
                text = "${bandLevel / 100}dB"
                textSize = 10f
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            }

            bandLayout.addView(textView)
            bandLayout.addView(seekBar)
            bandLayout.addView(valueText)
            layout.addView(bandLayout)
        }
    }

    private fun setupBassBoost() {
        val strength = bassBoost?.roundedStrength ?: 0
        seekBassBoost.progress = strength.toInt()
        
        seekBassBoost.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    try {
                        bassBoost?.setStrength(progress.toShort())
                    } catch (e: Exception) {
                        // Ignorar
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupVirtualizer() {
        val strength = virtualizer?.roundedStrength ?: 0
        seekVirtualizer.progress = strength.toInt()
        
        seekVirtualizer.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    try {
                        virtualizer?.setStrength(progress.toShort())
                    } catch (e: Exception) {
                        // Ignorar
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupBalance() {
        seekBalance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
                    val balance = (progress - 1000) / 1000f
                    val leftVolume = if (balance < 0) 1f else 1f - balance
                    val rightVolume = if (balance > 0) 1f else 1f + balance
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPresets() {
        val presets = mapOf(
            "Flat" to shortArrayOf(0, 0, 0, 0, 0),
            "Rock" to shortArrayOf(400, 200, -200, 200, 400),
            "Bass" to shortArrayOf(600, 400, 0, 0, 0),
            "Clásica" to shortArrayOf(0, 0, 0, 200, 400),
            "Pop" to shortArrayOf(200, 400, 200, 0, -200),
            "Jazz" to shortArrayOf(200, 0, -200, 200, 400)
        )

        chipGroupPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = findViewById<Chip>(checkedIds[0])
                val presetName = chip.text.toString()
                val presetValues = presets[presetName]
                
                if (presetValues != null) {
                    applyPreset(presetValues)
                }
            }
        }
    }

    private fun applyPreset(values: ShortArray) {
        // CORRECCIÓN: Convertir numberOfBands a Int para evitar el error de Short vs Int
        val bands = equalizer?.numberOfBands?.toInt() ?: return
        val minLevel = equalizer?.bandLevelRange?.get(0)?.toInt() ?: -1500

        for (i in 0 until bands.coerceAtMost(values.size)) {
            try {
                equalizer?.setBandLevel(i.toShort(), values[i])
                // Actualizar SeekBar correspondiente
                val seekBar = layout.getChildAt(i)?.let {
                    (it as? LinearLayout)?.getChildAt(1) as? SeekBar
                }
                seekBar?.progress = values[i].toInt() - minLevel
            } catch (e: Exception) {
                // Ignorar
            }
        }
    }

    private fun setupAudioMode() {
        toggleAudioMode.check(R.id.btnStereo)
        
        toggleAudioMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnMono -> {
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            startActivity(intent)
                            Toast.makeText(this, "Activa 'Audio Mono' en Accesibilidad", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this, "No se pudo abrir ajustes", Toast.LENGTH_SHORT).show()
                        }
                    }
                    R.id.btnStereo -> {
                        Toast.makeText(this, "Modo Stereo activado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun enableEqualizer(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        switchEqualizer.setOnCheckedChangeListener { _, isChecked ->
            enableEqualizer(isChecked)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            // Ignorar
        }
    }
}