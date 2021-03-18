package com.shorman.smartelevator.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.R
import kotlinx.android.synthetic.main.forget_password_fragment.*
import java.lang.Exception

class ForgetPasswordFragment:Fragment(R.layout.forget_password_fragment){

    private lateinit var auth: FirebaseAuth
    private var timer: CountDownTimer? = null
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage(getString(R.string.sending_link))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSendEmail.setOnClickListener {
            val email = etEmail.text.toString()
            val dialog = MaterialAlertDialogBuilder(requireContext())
            hideKeyboard()
            dialog.setTitle(getString(R.string.are_u_sure))
            dialog.setMessage("${getString(R.string.send_link_to)} $email")
            dialog.setPositiveButton(getString(R.string.yes)){ emailDialog, _ ->
                if(!Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.length < 6){
                    Snackbar.make(requireView(),getString(R.string.please_enter_vaild_email),
                        Snackbar.LENGTH_SHORT)
                        .show()

                    emailDialog.dismiss()
                }
                else{
                    progressDialog.show()
                    auth.useAppLanguage()
                    auth.sendPasswordResetEmail(email)
                        .addOnCompleteListener {task ->
                            if(task.isSuccessful){
                                btnSendEmail.isEnabled = false
                                startTimer()
                                Snackbar.make(requireView(),getString(R.string.email_sent),
                                    Snackbar.LENGTH_LONG)
                                    .show()

                                tvEmailStatus.visibility = View.VISIBLE
                                btnOpenEmail.visibility = View.VISIBLE
                                progressDialog.dismiss()
                            }

                            emailDialog.dismiss()
                        }
                        .addOnFailureListener {
                            progressDialog.dismiss()
                            Snackbar.make(requireView(),"Error ${it.message}", Snackbar.LENGTH_LONG)
                                .show()

                            tvEmailStatus.visibility = View.INVISIBLE
                            btnOpenEmail.visibility = View.INVISIBLE


                            emailDialog.dismiss()
                        }
                }
            }
            dialog.setNegativeButton(getString(R.string.no)){ forgetDialog, _ ->
                forgetDialog.dismiss()
            }
            dialog.show()

        }

        btnGoToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        btnOpenEmail.setOnClickListener {
            try {
                val intent = activity?.packageManager?.getLaunchIntentForPackage("com.google.android.gm")
                startActivity(intent)
            }catch (e: Exception){
                Snackbar.make(requireView(),"No email application", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
    }


    private fun startTimer(){
        reSendEmail.visibility = View.VISIBLE
        timer = object :CountDownTimer(180000,1000){
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                reSendEmail.text = "${getString(R.string.resend_email)} ${millisUntilFinished/1000}"
            }

            override fun onFinish() {
                btnSendEmail.isEnabled = true
                reSendEmail.visibility = View.GONE
            }
        }
        timer?.start()
    }

    private fun cancelTimer(){
        if(timer != null){
            timer?.cancel()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancelTimer()
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
