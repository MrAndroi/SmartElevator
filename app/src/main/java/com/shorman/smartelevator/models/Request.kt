package com.shorman.smartelevator.models


// user requests data class
data class Request(
        var requestId: String="",
        var userId:String="",
        var elevatorId:String="",
        var floorNumber:Int =-1,
        var timestamp: Long = System.currentTimeMillis(),
)
