    // ─── اعمال تنظیمات دستیار AI ───
    fun applyAiTuning(result: AiTuningAssistant.TuningResult) {
        // ۱. سرعت
        _currentSpeed.value = result.speed
        _isSlowed.value = result.speed < 1.0f

        // ۲. گام
        _currentPitch.value = result.pitch
        applyPlaybackParameters()

        // ۳. Reverb
        if (result.reverb && result.reverbPreset != null) {
            _isReverbEnabled.value = true
            _currentReverbPreset.value = result.reverbPreset
            effectsManager?.setReverbEnabled(true, result.reverbPreset)
        } else {
            _isReverbEnabled.value = false
            effectsManager?.setReverbEnabled(false)
        }

        // ۴. Bass Boost
        _isBassBoostEnabled.value = result.bassBoost
        effectsManager?.setBassBoostEnabled(result.bassBoost)

        // ۵. 8D
        _is8DEnabled.value = result.virtualizer
        effectsManager?.setVirtualizerEnabled(result.virtualizer)

        // ۶. Dynamics
        _isDynamicsEnabled.value = result.dynamics
        dynamicsManager?.setEnabled(result.dynamics)

        // ۷. افکت محیطی
        ambientManager.stop()
        if (result.ambientSound != null) {
            _ambientVolume.value = result.ambientVolume
            ambientManager.play(result.ambientSound, result.ambientVolume)
            _activeAmbient.value = result.ambientSound
        } else {
            _activeAmbient.value = null
        }
    }
