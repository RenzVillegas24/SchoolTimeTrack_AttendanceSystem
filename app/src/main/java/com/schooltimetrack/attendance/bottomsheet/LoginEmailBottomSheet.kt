package com.schooltimetrack.attendance.bottomsheet

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.schooltimetrack.attendance.MainActivity
import com.schooltimetrack.attendance.R
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.launch

class LoginEmailBottomSheet(
    private val onButtonLoginClick: (bottomSheet: LoginEmailBottomSheet, email: String, password: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var client: Client
    private lateinit var account: Account
    private lateinit var storage: Storage
    private lateinit var databases: Databases

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        client = (activity as MainActivity).client
        account = (activity as MainActivity).account
        storage = (activity as MainActivity).storage
        databases = (activity as MainActivity).databases
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_login_email, container, false)

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btnLogin)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)

        btnLogin.setOnClickListener {
            handleLogin(etEmail.text.toString(), etPassword.text.toString())
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as? ViewGroup
            bottomSheet?.let {
                BottomSheetBehavior.from(it).apply {
                    state = BottomSheetBehavior.STATE_EXPANDED
                    skipCollapsed = true
                    isDraggable = true
                }
            }
        }

        dialog.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }

        return dialog
    }

    private fun handleLogin(email: String, password: String) {
        if (email.isNotEmpty() && password.isNotEmpty()) {
            onButtonLoginClick(this, email, password)
        } else {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
        }
    }
}