package com.shorman.smartelevator.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.R
import kotlinx.android.synthetic.main.change_language_dialog.view.*
import kotlinx.android.synthetic.main.enter_password_dialog.*
import kotlinx.android.synthetic.main.enter_password_dialog.view.*
import kotlinx.android.synthetic.main.login_fragment.*

class LoginFragment:Fragment(R.layout.login_fragment) {

    private lateinit var auth:FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var usersDatabaseReference:DatabaseReference
    private val requestCode = 1000


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getCameraPermission()

        auth = Firebase.auth
        usersDatabaseReference = Firebase.database.reference.child("Users")
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage(getString(R.string.authinticationg))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        })

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            hideKeyboard()
            if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Snackbar.make(requireView(),getString(R.string.please_enter_vaild_email), Snackbar.LENGTH_SHORT)
                    .show()
            }
            else if(password.length < 6){
                Snackbar.make(requireView(),getString(R.string.wrong_pass), Snackbar.LENGTH_SHORT)
                    .show()
            }
            else{
                progressDialog.show()
                btnLogin.isEnabled = false
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            progressDialog.dismiss()
                            btnLogin.isEnabled = true

                            val user = auth.currentUser
                            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
                                usersDatabaseReference.child(user!!.uid).child("token").setValue(it.token)
                            }
                            updateUi(user)
                        }
                    }.addOnFailureListener {

                        progressDialog.dismiss()
                        btnLogin.isEnabled = true

                        when {
                            it.message?.contains("password")!! -> {
                                Snackbar.make(requireView(),"Error:Wrong password", Snackbar.LENGTH_LONG)
                                    .show()
                            }
                            it.message?.contains("record")!! -> {
                                Snackbar.make(requireView(),"Error:No account registered with this email", Snackbar.LENGTH_LONG)
                                    .show()
                            }
                            else -> {
                                Snackbar.make(requireView(),"Error:Unknown error", Snackbar.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }
            }

        }

        btnGoToRegister.setOnClickListener{
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgetPasswordFragment)
        }

        optionIconLogin.setOnClickListener {
            showPopUpMenuLoggedIn(it)
        }
    }

    private fun updateUi(currentUser: FirebaseUser?){
        if(currentUser != null) {
           findNavController().navigate(R.id.action_loginFragment_to_mainScreenFragment)
        }
    }

    private fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getCameraPermission(){

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), requestCode)
        }
    }

    private fun showPopUpMenuLoggedIn(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.login_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.changeLanguageLogin ->{
                    showChangeLanguageDialog()
                    true
                }
                R.id.addNewElevator -> {
                    showConfirmPasswordDialog()
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    //function to show admin password dialog
    @SuppressLint("InflateParams")
    private fun showConfirmPasswordDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext()).create()
        val layout = layoutInflater.inflate(R.layout.enter_password_dialog, null)
        layout.tvContinueChangeEmail.setOnClickListener {
            val adminCode = layout.etConfirmPassword.text.toString()
            //check if the password is current and open add new elevator
            if(adminCode == "Smart_elevator122"){
                findNavController().navigate(R.id.action_loginFragment_to_addNewElevatorFragment)
                dialog.dismiss()
            }
            else{
                Snackbar.make(requireView(),getString(R.string.wrong_pass),1000).show()
                dialog.dismiss()
            }
        }
        layout.tvCancelEmailChange.setOnClickListener { dialog.dismiss() }
        dialog.setView(layout)
        dialog.show()

    }

    @SuppressLint("InflateParams")
    private fun showChangeLanguageDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext()).create()
        val layout = layoutInflater.inflate(R.layout.change_language_dialog, null)
        layout.languageButtons.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.arabicLanguage -> {
                    changeLanguage("ar")
                }
                R.id.englishLanguage -> {
                    changeLanguage("en")
                }
            }
        }
        layout.tvCancelLanguageDialog.setOnClickListener { dialog.dismiss() }
        dialog.setView(layout)
        dialog.show()

    }

    private fun changeLanguage(lang: String) {
        val editor = activity?.getSharedPreferences("language", Context.MODE_PRIVATE)?.edit()
        editor?.putString("lang", lang)
        editor?.apply()
        activity?.finish()
        startActivity(activity?.intent)
        activity?.overridePendingTransition(0, 0)
    }

}