package ch.hslu.blebooking.mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform