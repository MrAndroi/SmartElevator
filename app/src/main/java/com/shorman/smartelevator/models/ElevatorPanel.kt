package com.shorman.smartelevator.models

//elevator panel data class
data class ElevatorPanel(
        //panel number(floor number)
        var number:Int =-1,
        //this will show wheather this floor is selected or not
        var selected:Boolean=false,
)
