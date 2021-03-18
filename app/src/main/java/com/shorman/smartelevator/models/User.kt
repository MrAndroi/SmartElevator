package com.shorman.smartelevator.models

//user data class
data class User(
    var userName:String="",
    var userPhoneNumber:String="",
    var userEmail:String="",
    var userId:String="",
    var userToken:String="",
    var currentElevator:String="",
    var elevatorMoving:Boolean = false,
)
