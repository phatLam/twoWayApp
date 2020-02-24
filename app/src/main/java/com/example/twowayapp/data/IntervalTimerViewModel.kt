package com.example.twowayapp.data

import android.util.Log
import androidx.databinding.Bindable
import androidx.databinding.ObservableInt
import androidx.databinding.library.baseAdapters.BR
import com.example.twowayapp.util.ObservableViewModel
import com.example.twowayapp.util.Timer
import com.example.twowayapp.util.cleanSecondsString
import java.lang.NumberFormatException
import java.util.*
import kotlin.math.round

const val INITIAL_SECONDS_PER_WORK_SET = 5 // Seconds
const val INITIAL_SECONDS_PER_REST_SET = 2 // Seconds
const val INITIAL_NUMBER_OF_SETS = 5
class IntervalTimerViewModel(private val timer: Timer): ObservableViewModel() {
    val TAG = IntervalTimerViewModel::class.java.name
    val timePerWorkSet = ObservableInt(INITIAL_SECONDS_PER_WORK_SET * 10) // tenths
    val timePerRestSet = ObservableInt(INITIAL_SECONDS_PER_REST_SET * 10) // tenths
    val workTimeLeft = ObservableInt(timePerWorkSet.get()) // tenths
    val restTimeLeft = ObservableInt(timePerRestSet.get()) // tenths
    private var state = TimerStates.STOPPED
    private var stage = StartedStages.WORKING
    var timerRunning: Boolean
    @Bindable get() {
        return state == TimerStates.STARTED
    }
    set(value) {
        Log.i(TAG, "timerRunning = "+ timerRunning)
        if (value) startButtonClicked() else pauseButtonClicked()
    }

    private var numberOfSetsTotal = INITIAL_NUMBER_OF_SETS
    private var numberOfSetsElapsed = 0
    var numberOfSets: Array<Int> = emptyArray()
        @Bindable get() {
            return arrayOf(numberOfSetsElapsed, numberOfSetsTotal)
        }
        set(value: Array<Int>) {
            // Only the second Int is being set
            val newTotal = value[1]
            if (newTotal == numberOfSets[1]) return // Break loop if there's no change
            // Only update if it doesn't affect the current exercise
            if (newTotal != 0 && newTotal > numberOfSetsElapsed) {
                field = value
                numberOfSetsTotal = newTotal
            }
            // Even if the input is empty, force a refresh of the view
            notifyPropertyChanged(BR.numberOfSets)
        }
    private fun startButtonClicked(){
        when (state){
            TimerStates.STARTED -> {

            }
            TimerStates.PAUSED -> {
                pausedToStarted()
            }
            TimerStates.STOPPED -> {
                stoppedToStarted()
            }
        }
        val task = object : TimerTask() {
            override fun run() {
                if (state == TimerStates.STARTED) {
                    updateCountdowns()
                }
            }
        }
        // Schedule timer every 100ms to update the counters.
        timer.start(task)
    }
    private fun updateCountdowns() {
        if (state == TimerStates.STOPPED) {
            resetTimers()
            return
        }

        val elapsed = if (state == TimerStates.PAUSED) {
            timer.getPausedTime()
        } else {
            timer.getElapsedTime()
        }

        if (stage == StartedStages.RESTING) {
            updateRestCountdowns(elapsed)
        } else {
            // work
            updateWorkCountdowns(elapsed)
        }
    }

    private fun updateWorkCountdowns(elapsed: Long) {
        stage = StartedStages.WORKING
        val newTimeLeft = timePerWorkSet.get() - (elapsed / 100).toInt()
        if (newTimeLeft <= 0) {
            workoutFinished()
        }
        workTimeLeft.set(newTimeLeft.coerceAtLeast(0))
    }

    private fun updateRestCountdowns(elapsed: Long) {
        // Calculate the countdown time with the start time
        val newRestTimeLeft = timePerRestSet.get() - (elapsed / 100).toInt()
        restTimeLeft.set(newRestTimeLeft.coerceAtLeast(0))

        if (newRestTimeLeft <= 0) { // Rest time is up
            numberOfSetsElapsed += 1
            resetTimers()
            if (numberOfSetsElapsed >= numberOfSetsTotal) { // End
                timerFinished()
            } else { // End of set
                setFinished()
            }
        }
    }
    /* WORKING -> RESTING */
    private fun workoutFinished() {
        timer.resetStartTime()
        stage = StartedStages.RESTING
        notifyPropertyChanged(BR.inWorkingStage)
    }

    /* RESTING -> WORKING */
    private fun setFinished() {
        timer.resetStartTime()
        stage = StartedStages.WORKING

        notifyPropertyChanged(BR.inWorkingStage)
        notifyPropertyChanged(BR.numberOfSets)
    }

    /* RESTING -> STOPPED */
    private fun timerFinished() {
        state = TimerStates.STOPPED
        stage = StartedStages.WORKING // Reset for the next set
        timer.reset()
        notifyPropertyChanged(BR.timerRunning)
        numberOfSetsElapsed = 0

        notifyPropertyChanged(BR.inWorkingStage)
        notifyPropertyChanged(BR.numberOfSets)
    }

    private fun resetTimers() {
        // Reset counters
        workTimeLeft.set(timePerWorkSet.get())

        // Set the start time
        restTimeLeft.set(timePerRestSet.get())
    }
    /**
     * Pause the timer
     */
    private fun pauseButtonClicked() {
        if (state == TimerStates.STARTED) {
            startedToPaused()
        }
        notifyPropertyChanged(BR.timerRunning)
    }

    /* STOPPED -> STARTED */
    private fun stoppedToStarted() {
        // Set the start time
        timer.resetStartTime()
        state = TimerStates.STARTED
        stage = StartedStages.WORKING

        notifyPropertyChanged(BR.inWorkingStage)
        notifyPropertyChanged(BR.timerRunning)
    }
    /* PAUSED -> STARTED */
    private fun pausedToStarted() {
        // We're measuring the time we're paused, so just add it to the start time
        timer.updatePausedTime()
        state = TimerStates.STARTED

        notifyPropertyChanged(BR.timerRunning)
    }


    /* STARTED -> PAUSED */
    private fun startedToPaused() {
        state = TimerStates.PAUSED
        timer.resetPauseTime()
    }
    /**
    * Used to control some animations.
    */
    val inWorkingStage: Boolean
        @Bindable get() {
            return stage == StartedStages.WORKING
        }

    /**
     * Resets timers and state. Called from the UI.
     */
    fun stopButtonClicked() {
        resetTimers()
        numberOfSetsElapsed = 0
        state = TimerStates.STOPPED
        stage = StartedStages.WORKING // Reset for the next set
        timer.reset()

        notifyPropertyChanged(BR.timerRunning)
        notifyPropertyChanged(BR.inWorkingStage)
        notifyPropertyChanged(BR.numberOfSets)
    }

    fun timePerRestSetChanged(newValue: CharSequence){
        try {
            timePerRestSet.set(cleanSecondsString(newValue.toString()))
        }catch (e: NumberFormatException){
            return
        }
        if (!timerRunning){
            workTimeLeft.set(timePerWorkSet.get())
        }
    }
    fun restTimeIncrease() = timePerSetIncrease(timePerRestSet, 1)

    fun workTimeIncrease() = timePerSetIncrease(timePerWorkSet, 1)

    fun setsIncrease() {
        numberOfSetsTotal += 1
        notifyPropertyChanged(BR.numberOfSets)
    }

    fun restTimeDecrease() = timePerSetIncrease(timePerRestSet, -1)

    fun workTimeDecrease() = timePerSetIncrease(timePerWorkSet, -1, 10)

    fun setsDecrease() { if (numberOfSetsTotal > numberOfSetsElapsed + 1) {
        numberOfSetsTotal -= 1
        notifyPropertyChanged(BR.numberOfSets)
    }
    }
    /**
     * Increases or decreases the work or rest time by a set value, depending on the sign of
     * the parameter.
     *
     * @param timePerSet the value holder to be updated
     * @param sign  1 to increase, -1 to decrease.
     */
    private fun timePerSetIncrease(timePerSet: ObservableInt, sign: Int = 1, min: Int = 0) {
        if (timePerSet.get() < 10 && sign < 0) return
        // Increase the time in chunks
        roundTimeIncrease(timePerSet, sign, min)
        if (state == TimerStates.STOPPED) {
            // If stopped, update the timers right away
            workTimeLeft.set(timePerWorkSet.get())
            restTimeLeft.set(timePerRestSet.get())
        } else {
            // If running or paused, the timers need to be calculated
            updateCountdowns()
        }
    }
    /**
     * Make increasing and decreasing times a bit nicer by adding chunks.
     */
    private fun roundTimeIncrease(timePerSet: ObservableInt, sign: Int, min: Int) {
        val currentValue = timePerSet.get()
        val newValue =
            when {
                // <10 Seconds - increase 1
                currentValue < 100 -> timePerSet.get() + sign * 10
                // >10 seconds, 5-second increase
                currentValue < 600 -> (round(currentValue / 50.0) * 50 + (50 * sign)).toInt()
                // >60 seconds, 10-second increase
                else -> (round(currentValue / 100.0) * 100 + (100 * sign)).toInt()
            }
        timePerSet.set(newValue.coerceAtLeast(min))
    }

}



enum class TimerStates {STOPPED, STARTED, PAUSED}

enum class StartedStages {WORKING, RESTING}

/**
 * Extensions to allow += and -= on an ObservableInt.
 */
private operator fun ObservableInt.plusAssign(value: Int) {
    set(get() + value)
}

private operator fun ObservableInt.minusAssign(amount: Int) {
    plusAssign(- amount)
}
