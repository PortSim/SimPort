package com.group7

class TruckWithoutContainer() : RoadObject {}

data class TruckWithContainer(val container: Container) : RoadObject {}

data object Container

class Ship() {}
