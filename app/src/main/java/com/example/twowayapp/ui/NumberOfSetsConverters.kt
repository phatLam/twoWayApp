package com.example.twowayapp.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseMethod
import com.example.twowayapp.R
import kotlinx.android.synthetic.main.activity_main.view.*


object NumberOfSetsConverters{
    @InverseMethod("stringToSetArray")
    @JvmStatic fun setArrayToString(context: Context, value: Array<Int>): String {
        return context.getString(R.string.sets_format, value[0] + 1, value[1])
    }
    /**
     * This is the Inverse Method used in `numberOfSets`, to convert from String to array.
     *
     * Note that Context is passed
     */
    @JvmStatic fun stringToSetArray(unused: Context, value: String): Array<Int> {
        // Converts String to long
        if (value.isEmpty()) {
            return arrayOf(0, 0)
        }

        return try {
            arrayOf(0, value.toInt()) // First item is not passed
        } catch (e: NumberFormatException) {
            arrayOf(0, 0)
        }
    }
}
object  NumberOfSetsBindingAdapters{
    @BindingAdapter("numberOfSets")
    @JvmStatic fun setNumberOfSets(view: EditText, value: String){
        if (view.text.toString() != value){
            view.setText(value)
        }
    }

    @InverseBindingAdapter(attribute = "numberOfSets")
    @JvmStatic fun getNumberOfSets(editText: EditText): String {
        return editText.text.toString()
    }

    @BindingAdapter("numberOfSetsAttrChanged")
    @JvmStatic fun setListener(view: EditText, listener: InverseBindingListener?){
        view.onFocusChangeListener = View.OnFocusChangeListener { focusedView, hasFocus ->
            val textView = focusedView as TextView
            Log.i("NumberOfSetsConverters", "onFocusChangeListener =" + hasFocus )
            if (hasFocus) {
                // Delete contents of the EditText if the focus entered.
                textView.text = ""
            } else {
                Log.i("NumberOfSetsConverters", "onFocusChangeListener = false" )
                // If the focus left, update the listener
                listener?.onChange()
            }
        }
    }
}