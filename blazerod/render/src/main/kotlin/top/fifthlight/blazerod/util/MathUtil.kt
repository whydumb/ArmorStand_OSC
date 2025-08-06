package top.fifthlight.blazerod.util

internal infix fun Int.ceilDiv(other: Int) = if (this % other == 0) {
    this / other
} else {
    this / other + 1
}

internal infix fun Int.roundUpToMultiple(divisor: Int) = (this ceilDiv divisor) * divisor

internal tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) {
    a
} else {
    gcd(b, a % b)
}

internal fun lcm(a: Int, b: Int): Int = a * (b / gcd(a, b))
