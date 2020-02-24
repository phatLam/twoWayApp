package com.example.twowayapp.ui

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.example.twowayapp.R

object BindingAdapter {
    @BindingAdapter("clearOnFocusAndDispatch")
    @JvmStatic fun clearOnFocusAndDispatch(view: EditText, listener: View.OnFocusChangeListener?){
        view.onFocusChangeListener = View.OnFocusChangeListener{view: View?, b: Boolean ->  
            val textView = view as TextView
            if (b){
                view.setTag(R.id.previous_value, textView.text)
                textView.text = ""
            }else {
                if(textView.text.isEmpty()){
                    val tag: CharSequence? = textView.getTag(R.id.previous_value) as CharSequence
                    textView.text = tag?: ""
                }
                listener?.onFocusChange(view, b)
            }
        }
    }
}