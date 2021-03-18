package com.shorman.smartelevator.ui.fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
import com.shorman.smartelevator.adapters.PanelAdapter
import com.shorman.smartelevator.models.ElevatorPanel
import kotlinx.android.synthetic.main.elevator_panel_fragment.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//Fragment to show the user elevator panel so he can choose hes destination
class ElevatorPanelFragment: Fragment(R.layout.elevator_panel_fragment) {

    private lateinit var elevatorsDatabaseReference: DatabaseReference
    private lateinit var elevatorsStackDatabaseReference: DatabaseReference

    //get the arguments that passed to this fragment
    private val args:ElevatorPanelFragmentArgs by navArgs()

    //save user elevator stack that was passed from prev fragment in new value
    private val elevatorStack by lazy {
        args.ElevatorStack
    }

    private lateinit var panelAdapter: PanelAdapter
    private lateinit var panelList:ArrayList<ElevatorPanel>
    private lateinit var progressDialog:ProgressDialog

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Reference to the elevator that was scanned so we can get the panel of it
        elevatorsDatabaseReference = Firebase.database.reference.child("Elevators")
                .child(elevatorStack.elevatorId)

        //Reference to the elevator stack that was scanned so we can modify it
        elevatorsStackDatabaseReference = Firebase.database.reference.child("Elevators")
                .child(elevatorStack.elevatorId)
                .child("elevatorStack")

        progressDialog = ProgressDialog(context)
        progressDialog.setMessage(getString(R.string.starting))

        //creating panel adapter with click listener for the items
        panelAdapter = PanelAdapter {
            elevatorStack.toFloor = it
            tvYouCanStart.visibility = View.VISIBLE
            //text view to show user that he can go for the next step(appear when the user select hes destination)
            tvYouCanStart.text = "${getString(R.string.floor_number)} $it ${getString(R.string.selected_you_can)}"

            //check if the user selected destination don't equal to hes current floor
            //and when he select hes destination correctly he can press start button
            if(it != elevatorStack.fromFloor){
                btnStart.isEnabled = true
                btnStart.background = ContextCompat.getDrawable(requireContext(),R.drawable.btn_start_background)
            }
            else{
                btnStart.isEnabled = false
                btnStart.background = ContextCompat.getDrawable(requireContext(),R.drawable.btn_start_not_enbled)
            }
        }

        //list to save elevator panel
        panelList = ArrayList()
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvElevatorPanel.setHasFixedSize(true)
        rvElevatorPanel.adapter = panelAdapter
        tvWhereTo.text = "${getString(R.string.where_to)} ${elevatorStack.userName}?"

        btnStart.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                //remove user current floor from elevator stack when he enter the elevator
                progressDialog.show()
                delay(1000)
                progressDialog.dismiss()

                //add user destination to elevator stack so elevator starts moving to the destination
                elevatorsStackDatabaseReference.child(elevatorStack.toFloor.toString()).setValue(elevatorStack.toFloor)

                //open elevator moving fragment passing user elevator stack as argument
                val direction = ElevatorPanelFragmentDirections.actionElevatorPanelFragmentToElevatorMovingFragment(elevatorStack)
                findNavController().navigate(direction)
            }

        }

        getElevatorPanel()
    }

    //function to get the scanned elevator panel and show it to the user in recyclerView
    private fun getElevatorPanel(){
        elevatorsDatabaseReference
                .child("elevatorPanel").addValueEventListener(object :ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        //adding elevator panel objects to panel list
                        for (i in snapshot.children){
                            val panelItem = i.getValue(ElevatorPanel::class.java)
                            panelItem?.let {
                                panelList.add(it)
                            }
                        }
                        if(panelList.size > 1){
                            panelList[elevatorStack.fromFloor].selected = true
                            panelAdapter.differ.submitList(panelList)
                            panelAdapter.notifyDataSetChanged()
                        }
                        else{
                            Snackbar.make(requireView(),getString(R.string.this_qr_code),Snackbar.LENGTH_LONG).show()
                        }
                        hideProgressBar()
                    }

                    override fun onCancelled(error: DatabaseError)= Unit

                })
    }

    private fun hideProgressBar(){
        progressBar2?.let {
            it.visibility = View.GONE
        }
    }
}