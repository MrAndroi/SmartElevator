package com.shorman.smartelevator.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

//elevator stack data class
@Parcelize
data class ElevatorStack(
    //this will save user name that asked for the elevator
    var userName:String="",
    //this will save elevator uiqiue id
    var elevatorId:String="",
    //this will save user current floor
    var fromFloor:Int=-1,
    //this will save user destination
    var toFloor:Int=-1,
):Parcelable
