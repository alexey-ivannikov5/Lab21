package ru.alexeyivannikov.lab21

sealed class TimerState {
    data object Stopped : TimerState()
    data class InProgress(
        val secondsLeft: Int
    ) : TimerState()

}