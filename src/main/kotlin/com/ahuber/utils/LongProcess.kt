package com.ahuber.utils

data class LongProcess(val totalStepCount: Long, val increment: Double = 0.1, val description: String? = null) {
    private var checkpoint = 0.0

    init {
        require(increment in 0.0..1.0) { "'increment' must be in the range [0.0, 1.0]" }
    }

    @Synchronized
    fun update(completedStepCount: Long) {
        val percentage = completedStepCount.toDouble() / totalStepCount.toDouble()

        if (percentage < checkpoint) {
            return
        }

        if (description != null) {
            print(description)
            print(" --> ")
        }

        println("${checkpoint * 100}%")
        checkpoint += increment
    }
}

inline fun <R> LongProcess.start(block: LongProcess.() -> R): R = this.block()