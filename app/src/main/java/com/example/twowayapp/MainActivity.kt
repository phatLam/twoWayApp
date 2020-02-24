package com.example.twowayapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.example.twowayapp.data.IntervalTimerViewModel
import com.example.twowayapp.data.IntervalTimerViewModelFactory
import com.example.twowayapp.databinding.ActivityMainBinding
const val SHARED_PREFS_KEY = "timer"
class MainActivity : AppCompatActivity() {

    private val viewModel: IntervalTimerViewModel by lazy {
        ViewModelProviders.of(this, IntervalTimerViewModelFactory).get(IntervalTimerViewModel::class.java)

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.viewModel = viewModel
    }
}
