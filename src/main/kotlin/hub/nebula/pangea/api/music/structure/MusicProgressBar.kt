package hub.nebula.pangea.api.music.structure

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicProgressBar(private val totalDuration: Long) {
    private var job: Job? = null
    var currentPosition = 0L
    private set

    fun start() {
        job = GlobalScope.launch {
            while (currentPosition < totalDuration) {
                currentPosition += 1000
                delay(1000)
            }
        }
    }

    fun stop() {
        job?.cancel()
        currentPosition = 0L
    }
}