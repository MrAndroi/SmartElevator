package com.shorman.smartelevator.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.R
import kotlinx.android.synthetic.main.elevator_moving_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//Fragment to show the user that the elevator is moving
class ElevatorMovingFragment: Fragment (R.layout.elevator_moving_fragment){

    //arguments that passed for this fragment
    private val args:ElevatorMovingFragmentArgs by navArgs()
    lateinit var elevatorDatabaseReference: DatabaseReference

    //elevator stack which represents the stack that the elevator should go for
    private val elevatorStack by lazy {
        args.ElevatorStackMoving
    }

    private val valueEventListener = (object :ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            val currentFloor = snapshot.getValue(Int::class.java)
            currentFloor?.let { floorNum ->
                tvElevatorCurrentFloor?.let {
                    //text view to represent the current elevator floor for the user
                    it.text = floorNum.toString()
                }

                //when the current floor number equal to the user destination
                //remove the user destination from elevator stack and open elevator finish moving fragment
                if(floorNum == elevatorStack.toFloor){
                    lifecycleScope.launchWhenResumed {
                        removeFromStack()
                        elevatorStack.fromFloor = elevatorStack.toFloor
                        val direction = ElevatorMovingFragmentDirections.actionElevatorMovingFragmentToElevatorFinishedMovingFragment(elevatorStack)
                        findNavController().navigate(direction)
                    }
                }

            }
        }

        override fun onCancelled(error: DatabaseError) = Unit

    })

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            //some logic that needs to be run before fragment is destroyed
            Snackbar.make(requireView(),getString(R.string.please_wait_elevator_is_moving),1000).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //firebase reference to elevator stack so we can modify on it
        elevatorDatabaseReference = Firebase.database.reference
                .child("Elevators")
                .child(elevatorStack.elevatorId)

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
                onBackPressedCallback
        )

        getElevatorCurrentFloor()

        //text view to show the user hes destination
        tvYourdestnation.text = "${getString(R.string.your_destination)} ${elevatorStack.toFloor}"

    }

    //function to listen for current elevator floor
    private fun getElevatorCurrentFloor(){
        elevatorDatabaseReference
                .child("elevatorCurrentFloor")
                .addValueEventListener(valueEventListener)
    }

    //function to remove user destination from elevator stack when he arrive hes destination
    fun removeFromStack(){
        elevatorDatabaseReference
                .child("elevatorStack")
                .child("\"${elevatorStack.toFloor}\"")
                .removeValue()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //unregister listener here
        onBackPressedCallback.isEnabled = false
        onBackPressedCallback.remove()
        elevatorDatabaseReference.removeEventListener(valueEventListener)
    }
}