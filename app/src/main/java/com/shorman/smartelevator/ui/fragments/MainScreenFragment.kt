package com.shorman.smartelevator.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.R
import com.shorman.smartelevator.models.*
import kotlinx.android.synthetic.main.change_language_dialog.view.*
import kotlinx.android.synthetic.main.elevator_moving_fragment.*
import kotlinx.android.synthetic.main.main_screen_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//Fragment to scan qr code and enter current floor number
class MainScreenFragment:Fragment(R.layout.main_screen_fragment) {

    private lateinit var auth:FirebaseAuth
    private lateinit var usersDatabaseReference: DatabaseReference
    private lateinit var codeScanner: CodeScanner
    private val requestCode = 1000
    private var userName:String=""
    private var elevatorId:String=""
    private lateinit var progressDialog:ProgressDialog

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            //some logic that needs to be run before fragment is destroyed
            activity?.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        auth = Firebase.auth
        //firebase reference to the users
        usersDatabaseReference = Firebase.database.reference.child("Users").child(auth.uid!!)
        progressDialog = ProgressDialog(context)
        progressDialog.setMessage(getString(R.string.elevator_is_coming))
        progressDialog.setCancelable(false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
                onBackPressedCallback
        )


        //set up code scanner object with activity and scanner view from xml
        codeScanner = CodeScanner(requireActivity(), scanner_view)
        getUserName()

        getCameraPermission()

        optionIconMainScreen.setOnClickListener {
            showPopUpMenuLoggedIn(it)
        }

        btnNext.setOnClickListener {
            val currentFloorNumber = etFloorNumber.text.toString()
            if(currentFloorNumber.isBlank() || elevatorId.isBlank()){
                Snackbar.make(requireView(),getString(R.string.enter_floot_number),1500).show()
            }
            else{
                val fromFloor = etFloorNumber.text.toString().toInt()

                val elevatorStack = ElevatorStack(userName = userName,
                        elevatorId = elevatorId,
                        fromFloor = fromFloor)

                val requestId:String = Firebase.database.reference.push().key!!

                val request = Request(
                        requestId = requestId,
                        userId = auth.uid!!,
                        elevatorId = elevatorId,
                        floorNumber = fromFloor,
                )

                CoroutineScope(Dispatchers.Main).launch {
                    progressDialog.show()

                    askForElevator(elevatorId,fromFloor)
                    sendUserRequest(request)

                    getElevatorCurrentFloor(fromFloor,elevatorStack)
                }
            }

        }

    }

    //function to get user name
    private fun getUserName(){
        usersDatabaseReference.addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                if(tvWelcome !=  null){
                    //text view to show the user name
                    tvWelcome.text = "${getString(R.string.welcome)}, ${user?.userName}"
                }
                userName = user?.userName!!
                hideProgressBar()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    //function to add new request from user
    private fun sendUserRequest(request:Request){
        val requestDatabaseReference = Firebase.database.reference.child("Requests")
                .child(request.requestId)

        requestDatabaseReference.setValue(request)
    }

    //function to add user current floor to the elevator stack so the elevator can go for him
    private fun askForElevator(elevatorId:String,floorNumber: Int){
        val requestDatabaseReference = Firebase.database.reference.child("Elevators")
                .child(elevatorId)
                .child("elevatorStack")
                .child(floorNumber.toString())

        requestDatabaseReference.setValue(floorNumber)
    }

    private fun hideProgressBar(){
        if(progressBar != null){
            progressBar.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    //function to set up the code scanner settings
    private fun initQrCodeScanner(){
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,


        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.CONTINUOUS // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        //Listen for code scanner callbacks

        //called when the code scanned successfully
        codeScanner.decodeCallback = DecodeCallback {
            CoroutineScope(Dispatchers.Main).launch {

                //update elevatorId variable to the scanned Id
                elevatorId = it.text
                //show scanned animation and text for the status
                qrCodeAnimation.visibility = View.VISIBLE
                tvScanStatus.visibility = View.VISIBLE
                qrCodeAnimation.playAnimation()
                scanner_view.visibility = View.GONE

                //clean scanner resource
                codeScanner.releaseResources()

            }
        }
        //called when there is error in the scan
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            CoroutineScope(Dispatchers.Main).launch {
                Snackbar.make(requireView(), "Camera initialization error: ${it.message}", 2000).show()
            }
        }

        scanner_view.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    //function to get camera permission
    private fun getCameraPermission(){

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), requestCode)
        }
        else{
            initQrCodeScanner()
        }
    }

    //show menu for sign out and change language
    private fun showPopUpMenuLoggedIn(view: View) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.main_screen_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.logOut ->{
                    MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.log_out))
                            .setMessage(getString(R.string.are_you_sure_logout))
                            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                                dialog.dismiss()

                            }
                            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                                auth.signOut()
                                val currentUser = auth.currentUser
                                updateUi(currentUser)
                                dialog.dismiss()
                            }
                            .show()
                    true
                }
                R.id.changeLanguage ->{
                    showChangeLanguageDialog()
                    true
                }
                else -> {
                    false
                }
            }
        }
        popupMenu.show()
    }

    //update user interface depends on current user
    //if its null then show login fragment
    private fun updateUi(currentUser: FirebaseUser?){
        if(currentUser == null) {
            findNavController().navigate(R.id.action_mainScreenFragment_to_loginFragment)
        }
    }

    //function to show change language dialog
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

    //function to change language
    private fun changeLanguage(lang: String) {
        val editor = activity?.getSharedPreferences("language", Context.MODE_PRIVATE)?.edit()
        editor?.putString("lang", lang)
        editor?.apply()
        activity?.finish()
        startActivity(activity?.intent)
        activity?.overridePendingTransition(0, 0)
    }

    //function to listen for current elevator floor
    private fun getElevatorCurrentFloor(fromFloor:Int,elevatorStack:ElevatorStack){
        val elevatorDatabaseReference = Firebase.database.reference
                .child("Elevators")
                .child(elevatorId)
                .child("elevatorCurrentFloor")

        val valueEventListener = object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentFloor = snapshot.getValue(Int::class.java)
                currentFloor?.let { floorNum ->
                    //when the current floor number equal to the user destination
                    //remove the user destination from elevator stack and open elevator finish moving fragment
                    progressDialog.setMessage("${getString(R.string.elevator_is_coming)} $floorNum")

                    if(floorNum == fromFloor){
                        lifecycleScope.launchWhenResumed {
                            removeFromStack(fromFloor,elevatorId)
                            progressDialog.dismiss()
                            vibratePhone()
                            val direction = MainScreenFragmentDirections.actionMainScreenFragmentToElevatorPanelFragment(elevatorStack)
                            findNavController().navigate(direction)
                        }
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) = Unit

        }

        elevatorDatabaseReference.addValueEventListener(valueEventListener)
    }

    //function to vibrate the phone
    private fun vibratePhone(){
        val v = activity?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        //check for phone os
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v!!.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            v!!.vibrate(500)
        }
    }
    //function to remove user destination from elevator stack when he arrive hes destination
    fun removeFromStack(fromFloor: Int,elevatorId: String){
        Firebase.database.reference
                .child("Elevators")
                .child(elevatorId)
                .child("elevatorStack")
                .child(fromFloor.toString())
                .removeValue()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //unregister listener here
        onBackPressedCallback.isEnabled = false
        onBackPressedCallback.remove()
    }

}