package com.shorman.smartelevator.ui.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.R
import com.shorman.smartelevator.models.User
import kotlinx.android.synthetic.main.register_fragment.*

class RegisterFragment:Fragment(R.layout.register_fragment) {

    private lateinit var auth: FirebaseAuth
    private lateinit var usersDatabaseReference: DatabaseReference
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth
        usersDatabaseReference = Firebase.database.reference.child("Users")
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage(getString(R.string.registring))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString()
            val password  =etPassword.text.toString()
            val phoneNumber = etPhoneNumber.text.toString()
            val userName = etUserName.text.toString()
            hideKeyboard()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                Snackbar.make(
                    requireView(),
                        getString(R.string.please_enter_vaild_email),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
                etEmail.error = getString(R.string.please_enter_vaild_email)
            }
            else if(phoneNumber.length < 10) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.please_enter_vaild_phone),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
                etPhoneNumber.error =  getString(R.string.please_enter_vaild_phone)
            }
            else if(password.length < 6){
                Snackbar.make(
                    requireView(),
                   getString(R.string.please_enter_valid_pass),
                    Snackbar.LENGTH_SHORT
                )
                    .show()

                etPassword.error =getString(R.string.please_enter_valid_pass)
            }
            else if(userName.length < 4){
                Snackbar.make(
                    requireView(),
                   getString(R.string.please_enter_valid_name),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
                etUserName.error = getString(R.string.please_enter_valid_name)

            }
            else{
                progressDialog.show()
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if(task.isSuccessful){
                            val currentUser = auth.currentUser
                            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instansResult ->
                                val user = User(userEmail = email,
                                    userName = userName,
                                    userPhoneNumber = phoneNumber,
                                    userId = currentUser?.uid!!,
                                    userToken = instansResult.token
                                )

                                usersDatabaseReference.child(user.userId).setValue(user)
                            }
                            progressDialog.dismiss()
                            updateUi(currentUser)
                        }
                    }
                    .addOnFailureListener { exception ->

                        progressDialog.dismiss()

                        if(exception.message?.contains("email address")!!){
                            Snackbar.make(requireView(), "this_email_is_already_registered", Snackbar.LENGTH_LONG)
                                .show()
                        }
                        else{
                            Snackbar.make(requireView(), "unknown_error", Snackbar.LENGTH_LONG)
                                .show()
                        }

                    }

            }

        }

        btnGoToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updateUi(currentUser: FirebaseUser?){
        if(currentUser != null) {
            findNavController().navigate(R.id.action_registerFragment_to_mainScreenFragment)
        }
    }

    private fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

}