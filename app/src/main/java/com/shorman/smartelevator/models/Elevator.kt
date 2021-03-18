package com.shorman.smartelevator.models

//elevator data class
data class Elevator(
    //this will be uique id for elevator to help access the elevator information in database
    var elevatorCode:String="",
    //this will save elevator current floor
    var elevatorCurrentFloor:Int=0,
    //this will show if the user is currently moving or not
    var elevatorMoving:Boolean=false,
    // this will save elevator panel in list
    var elevatorPanel:List<ElevatorPanel> = mutableListOf(),
    //this will save elevator stack, based on this stack the elevator will move
    var elevatorStack:List<Int> = mutableListOf(),
    //this will save the url of the elevator qr code image
    var elevatorCodeImage:String = "",
    //this will represent the number of first floor in the elevator
    var firstFloor:Int=0,
        //this will represent the number of last floor in the elevator
    var lastFloor:Int=-1,
)
