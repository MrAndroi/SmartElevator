package com.shorman.smartelevator.ui.fragments

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.google.zxing.WriterException
import com.shorman.smartelevator.R
import com.shorman.smartelevator.models.Elevator
import com.shorman.smartelevator.models.ElevatorPanel
import kotlinx.android.synthetic.main.add_new_elevator_fragment.*
import java.io.File
import java.io.FileOutputStream

//Fragment to add new elevators
class AddNewElevatorFragment : Fragment(R.layout.add_new_elevator_fragment) {

    lateinit var firebaseStorageRef:StorageReference
    lateinit var firebaseStorage: FirebaseStorage
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //reference to elevator qr images (here we upload elevator qr images)
        firebaseStorageRef = Firebase.storage.reference.child("Elevators_Images")

        firebaseStorage = FirebaseStorage.getInstance()
        progressDialog = ProgressDialog(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddElevator.setOnClickListener {
            val floorNumbers = etElevatorFloorNumbers.text.toString()
            val elevatorCode = etElevatorCode.text.toString()
            val firstFloor = etElevatorFirstFloorNumber.text.toString().toInt()

            //check if there is missing information
            if(floorNumbers.isBlank() || elevatorCode.isBlank()){
                Snackbar.make(requireView(), getString(R.string.please_fill_info), 2000).show()
            }
            else{
                //call add elevator function
                addNewElevator(firstFloor,floorNumbers.toInt(), elevatorCode)
            }
        }

    }

    // function to add new elevator
    private fun addNewElevator(firstFloorNumber:Int,floorNumbers: Int, elevatorCode: String) {
        //generate elevator panel objects
        val elevatorPanel = mutableListOf<ElevatorPanel>()
        for (i in firstFloorNumber..floorNumbers) {
            elevatorPanel.add(
                    element = ElevatorPanel(i, false)
            )
        }

        //creating elevator object
        val elevator = Elevator(
                elevatorCode = elevatorCode,
                elevatorCurrentFloor = 0,
                elevatorMoving = false,
                elevatorPanel = elevatorPanel,
                firstFloor = firstFloorNumber,
                lastFloor = elevatorPanel[elevatorPanel.size-1].number
        )

        //generate qr code image for elevator code
        val qrEncoder = QRGEncoder(elevatorCode, null, QRGContents.Type.TEXT, 512)
        qrEncoder.colorBlack = Color.BLACK
        qrEncoder.colorWhite = Color.WHITE

        val bitmap:Bitmap?

        //try to save and upload generated qrCode image and elevator
        try {
            //bitmap to save qrCode image
            bitmap = qrEncoder.bitmap
            ivElevatorQr.visibility = View.VISIBLE
            ivElevatorQr.setImageBitmap(bitmap)

            //check for storage permission
            if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED){

                //qr saver value which saves uri to the image that we save in the phone
                //so we can upload it to firebase storage
                val qrgSaver =  saveImage(bitmap, elevatorCode)

                //function to upload elevator to firebase
                uploadElevator(qrgSaver.toUri(),elevator)

            }
            else{
                //ask user for storage permission
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 200)
            }
        }
        catch (e: WriterException) {
            //catch error for saving the qrImage
            Snackbar.make(requireView(), e.message.toString(), 2000).show()
        }

    }

    //function to save qrImage in the phone
    //first parameter is the bitmap that we want to save
    //second parameter is the image name
    //return type is File so we can access the qrImage and upload it to firebase storage later
    private fun saveImage(finalBitmap: Bitmap?, image_name: String):File {
        val root = Environment.getExternalStorageDirectory().toString()+"/SmartElevator/"
        val myDir = File(root)
        myDir.mkdirs()
        val fname = "Image-$image_name.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        Log.i("LOAD", root + fname)
        return try {
            val out = FileOutputStream(file)
            finalBitmap?.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file
        }
    }

    //function to upload elevator to firebase
    //first parameter is the qrImage for elevator
    //second parameter is the elevator that we want to upload
    private fun uploadElevator(imgUri:Uri?,elevator:Elevator) {
        if(imgUri != null){
            //generating image reference for the based on the image path and the current time
            val imageRef = firebaseStorageRef.child("${System.currentTimeMillis()}${imgUri.lastPathSegment}")
            val uploadTask = imageRef.putFile(imgUri)
            //add upload callbacks for the image
            uploadTask.addOnProgressListener {
                val progress = (100.0 * it.bytesTransferred)/it.totalByteCount
                progressDialog.setMessage("uploading ${progress.toInt()} %")
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()
            }.addOnCompleteListener {
                progressDialog.dismiss()
            }
            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                //get the download url of the image that we upload so we can add
                //this url to elevator object
                imageRef.downloadUrl.addOnCompleteListener { downloadTask ->
                    if (downloadTask.isSuccessful) {
                        //saving image url in new value
                        val imgUrl = downloadTask.result.toString()

                        //update elevator image to the new image url
                        elevator.elevatorCodeImage = imgUrl

                        //firebase reference to elevators
                        val elevatordatabaseref = Firebase.database.reference
                                .child("Elevators")
                                .child(elevator.elevatorCode)

                        //add the new elevator to firebase
                        elevatordatabaseref.setValue(elevator)

                        Snackbar.make(requireView(), getString(R.string.elevator_added), 2000).show()
                        etElevatorFloorNumbers.setText("")
                        etElevatorCode.setText("")
                    }
                }
            }
        }
        else{
            //catch error
            Toast.makeText(requireContext(),"something_wrong_happen",Toast.LENGTH_SHORT).show()
        }
    }

}