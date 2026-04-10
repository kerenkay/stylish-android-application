package com.example.stylish_android_application

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.view.ViewGroup
import com.example.stylish_android_application.databinding.DialogConfirmBinding

fun showConfirmDialog(
    context: Context,
    title: String,
    message: String,
    positiveLabel: String = "Confirm",
    onConfirm: () -> Unit
) {
    val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
    binding.tvDialogTitle.text = title
    binding.tvDialogMessage.text = message
    binding.btnDialogPositive.text = positiveLabel

    val dialog = Dialog(context)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setContentView(binding.root)
    dialog.window?.apply {
        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    binding.btnDialogPositive.setOnClickListener {
        dialog.dismiss()
        onConfirm()
    }
    binding.btnDialogNegative.setOnClickListener { dialog.dismiss() }

    dialog.show()
}
