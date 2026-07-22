package com.requena.supportdesk.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

open class BaseViewModel {
    protected val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    protected fun launch(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    // Canonical double-submit guard. Mutex.tryLock() is non-suspending and runs
    // synchronously on the caller's thread, before `block` is dispatched onto
    // viewModelScope - so the check-and-lock is atomic even though viewModelScope runs on
    // Dispatchers.Default, unlike a `if (state.value.isX) return` check followed by a
    // `_state.update { isX = true }` inside the launched block (which has a real TOCTOU
    // window on a background dispatcher). Declare the Mutex as an instance field on the
    // subclass, not here - this class has no feature-specific state to guard.
    protected fun guardedLaunch(mutex: Mutex, block: suspend CoroutineScope.() -> Unit) {
        if (!mutex.tryLock()) return
        launch {
            try {
                block()
            } finally {
                mutex.unlock()
            }
        }
    }

    open fun clear() {
        viewModelScope.cancel()
    }
}
