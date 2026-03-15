package com.example.driverdashapp.data.remote.dto

import com.example.driverdashapp.domain.model.*

/**
 * Centralized DTO → Domain mappers.
 */

fun DriverData.toDomain(): Driver = Driver(
    driverId = driverId,
    name = name,
    phone = phone,
    email = email,
    isVerified = isVerified,
    isOnline = isOnline,
    rating = rating,
    totalRatings = totalRatings,
    totalDeliveries = totalDeliveries
)

fun EarningsData.toDomain(): Earnings = Earnings(
    totalDeliveries = totalDeliveries,
    totalEarnings = totalEarnings,
    todayEarnings = todayEarnings,
    rating = rating,
    totalRatings = totalRatings
)

fun VehicleData.toDomain(): Vehicle = Vehicle(
    vehicleId = vehicleId,
    vehicleType = vehicleType,
    licensePlate = licensePlate,
    brand = brand,
    model = model,
    color = color,
    year = year
)

fun VehicleAssignmentData.toDomain(): VehicleAssignment = VehicleAssignment(
    id = id,
    isPrimary = isPrimary,
    vehicle = vehicle?.toDomain()
)
