package de.xikolo.states.base

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class LiveDataState<T>(initialState: T? = null) : LiveData<T>() {

    init {
        initialState?.let {
            applyState(initialState)
        }
    }

    private fun applyState(state: T) {
        GlobalScope.launch(Dispatchers.Main) {
            value = state
        }
    }

    open fun state(state: T) {
        applyState(state)
    }
}
