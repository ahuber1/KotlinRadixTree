package com.ahuber.test.utils

/**
 * An enumeration containing values that represent the direction that goes from left to right and from right to left
 */
enum class Direction {
    /**
     * Represents a direction that starts from the left and goes to the right.
     */
    LR,

    /**
     * Represents a direction that starts from the right and goes to the left.
     */
    RL
}

/**
 * Returns a [Direction] that goes in the opposite direction of this [Direction]
 */
val Direction.oppositeDirection: Direction
    get() = when (this) {
        Direction.LR -> Direction.RL
        Direction.RL -> Direction.LR
    }