package com.dev.mytranslatorsunda.activities

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

class VoiceRecorder(private val mCallback: Callback) {
    abstract class Callback {
        //Called when the recorder starts hearing voice.

        open fun onVoiceStart() {}

        // Called when the recorder is hearing voice.
        open fun onVoice(data: ByteArray?, size: Int) {}

        /**
         * Called when the recorder stops hearing voice.
         */
        open fun onVoiceEnd() {}
    }

    private var mAudioRecord: AudioRecord? = null
    private var mThread: Thread? = null
    private var mBuffer: ByteArray? = null
    private val mLock = Any()

    /** The timestamp of the last time that voice is heard.  */
    private var mLastVoiceHeardMillis = Long.MAX_VALUE

    /** The timestamp when the current voice is started.  */
    private var mVoiceStartedMillis: Long = 0

    /**
     * Starts recording audio.
     *
     *
     * The caller is responsible for calling [.stop] later.
     */
    fun start() {
        // Stop recording if it is currently ongoing.
        stop()
        // Try to create a new recording session.
        mAudioRecord = createAudioRecord()
        if (mAudioRecord == null) {
            throw RuntimeException("Cannot instantiate VoiceRecorder")
        }
        // Start recording.
        mAudioRecord!!.startRecording()
        // Start processing the captured audio.
        mThread = Thread(ProcessVoice())
        mThread!!.start()
    }

    /**
     * Stops recording audio.
     */
    fun stop() {
        synchronized(mLock) {
            dismiss()
            if (mThread != null) {
                mThread!!.interrupt()
                mThread = null
            }
            if (mAudioRecord != null) {
                mAudioRecord!!.stop()
                mAudioRecord!!.release()
                mAudioRecord = null
            }
            mBuffer = null
        }
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    fun dismiss() {
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE
            mCallback.onVoiceEnd()
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    val sampleRate: Int
        get() = if (mAudioRecord != null) {
            mAudioRecord!!.sampleRate
        } else 0

    /**
     * Creates a new [AudioRecord].
     *
     * @return A newly created [AudioRecord], or null if it cannot be created (missing
     * permissions?).
     */
    private fun createAudioRecord(): AudioRecord? {
        for (sampleRate in SAMPLE_RATE_CANDIDATES) {
            val sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING)
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue
            }
            val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, CHANNEL, ENCODING, sizeInBytes)
            if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                mBuffer = ByteArray(sizeInBytes)
                return audioRecord
            } else {
                audioRecord.release()
            }
        }
        return null
    }

    /**
     * Continuously processes the captured audio and notifies [.mCallback] of corresponding
     * events.
     */
    private inner class ProcessVoice : Runnable {
        override fun run() {
            while (true) {
                synchronized(mLock) {
                    if (Thread.currentThread().isInterrupted) {
                        return
                    }
                    val size = mAudioRecord!!.read(mBuffer!!, 0, mBuffer!!.size)
                    val now = System.currentTimeMillis()
                    if (isHearingVoice(mBuffer, size)) {
                        if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                            mVoiceStartedMillis = now
                            mCallback.onVoiceStart()
                        }
                        mCallback.onVoice(mBuffer, size)
                        mLastVoiceHeardMillis = now
                        if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                            end()
                        }
                    } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                        mCallback.onVoice(mBuffer, size)
                        if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                            end()
                        }
                    }
                }
            }
        }

        private fun end() {
            mLastVoiceHeardMillis = Long.MAX_VALUE
            mCallback.onVoiceEnd()
        }

        private fun isHearingVoice(buffer: ByteArray?, size: Int): Boolean {
            var i = 0
            while (i < size - 1) {

                // The buffer has LINEAR16 in little endian.
                var s = buffer!![i + 1].toInt()
                if (s < 0) s *= -1
                s = s shl 8
                s += Math.abs(buffer[i].toInt())
                if (s > AMPLITUDE_THRESHOLD) {
                    return true
                }
                i += 2
            }
            return false
        }
    }

    companion object {
        private val SAMPLE_RATE_CANDIDATES = intArrayOf(16000, 11025, 22050, 44100)
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val AMPLITUDE_THRESHOLD = 1500
        private const val SPEECH_TIMEOUT_MILLIS = 2000
        private const val MAX_SPEECH_LENGTH_MILLIS = 30 * 1000
    }
}