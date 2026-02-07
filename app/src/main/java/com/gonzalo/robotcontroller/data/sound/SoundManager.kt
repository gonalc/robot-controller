package com.gonzalo.robotcontroller.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.gonzalo.robotcontroller.R

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val sounds: MutableMap<Sound, Int> = mutableMapOf()
    private val loadedSounds: MutableSet<Sound> = mutableSetOf()

    private var runningStreamId: Int? = null
    private var isRunningLooping = false

    enum class Sound {
        ENGINE_STARTING,
        ENGINE_RUNNING,
        ENGINE_SPEEDING_UP
    }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                sounds.entries.find { it.value == sampleId }?.key?.let { sound ->
                    loadedSounds.add(sound)
                }
            }
        }

        // Load all sounds
        sounds[Sound.ENGINE_STARTING] = soundPool.load(context, R.raw.engine_starting, 1)
        sounds[Sound.ENGINE_RUNNING] = soundPool.load(context, R.raw.engine_running, 1)
        sounds[Sound.ENGINE_SPEEDING_UP] = soundPool.load(context, R.raw.engine_speeding_up, 1)
    }

    fun playEngineStart() {
        play(Sound.ENGINE_STARTING)
    }

    fun playSpeedingUp() {
        play(Sound.ENGINE_SPEEDING_UP)
    }

    fun startEngineRunning() {
        if (isRunningLooping) return

        val soundId = sounds[Sound.ENGINE_RUNNING] ?: return
        if (Sound.ENGINE_RUNNING !in loadedSounds) return

        runningStreamId = soundPool.play(soundId, 1f, 1f, 1, -1, 1f)
        isRunningLooping = true
    }

    fun stopEngineRunning() {
        runningStreamId?.let { streamId ->
            soundPool.stop(streamId)
        }
        runningStreamId = null
        isRunningLooping = false
    }

    private fun play(sound: Sound, loop: Int = 0) {
        val soundId = sounds[sound] ?: return
        if (sound !in loadedSounds) return

        soundPool.play(soundId, 1f, 1f, 1, loop, 1f)
    }

    fun release() {
        stopEngineRunning()
        soundPool.release()
    }
}
