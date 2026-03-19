package com.example.crmfreelance

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform