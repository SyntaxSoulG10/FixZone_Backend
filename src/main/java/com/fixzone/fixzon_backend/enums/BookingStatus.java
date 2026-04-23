package com.fixzone.fixzon_backend.enums;

public enum BookingStatus {
    PENDING, // Legacy status to allow deleting old records
    PENDING_PAYMENT, //User created booking but NOT paid yet_soft locking
    CONFIRMED,//Payment completed → booking confirmed
    COMPLETED, //Service is finished
    CANCELLED,//Booking cancelled by user or system
    EXPIRED //EXPIRED = booking was created but NOT completed in time
}
