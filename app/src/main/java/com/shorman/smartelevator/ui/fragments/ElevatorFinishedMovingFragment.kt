package com.shorman.smartelevator.ui.fragments

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.database.DatabaseReference
import com.shorman.smartelevator.R
import kotlinx.android.synthetic.main.elevator_finish_moving_fragment.*

//Fragment to show the user that he arrive to asked floor
class ElevatorFinishedMovingFragment:Fragment(R.layout.elevator_finish_moving_fragment) {

    private val args:ElevatorFinishedMovingFragmentArgs by navArgs()

    //elevator stack which represents the stack that the elevator should go for
    private val elevatorStack by lazy {
        args.ElevatorStack
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            //some logic that needs to be run before fragment is destroyed
            activity?.finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
                onBackPressedCallback
        )

        vibratePhone()

        //button to exit from the app
        exitBtn.setOnClickListener {
            activity?.finish()
        }
        btnNewDestination.setOnClickListener {
            val direction = ElevatorFinishedMovingFragmentDirections.actionElevatorFinishedMovingFragmentToElevatorPanelFragment(elevatorStack)
            findNavController().navigate(direction)
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        //unregister listener here
        onBackPressedCallback.isEnabled = false
        onBackPressedCallback.remove()
    }
}