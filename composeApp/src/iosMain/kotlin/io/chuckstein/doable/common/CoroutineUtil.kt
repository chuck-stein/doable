package io.chuckstein.doable.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class ViewModelScope : CoroutineScope {
    override val coroutineContext = Dispatchers.Main + SupervisorJob()

    fun cancel() {
        coroutineContext.cancel()
    }
}

