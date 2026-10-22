package com.example.data.model

data class StreamingProvider(
    val id: String,
    val name: String,
    val colorHex: Long,
    val secondaryColorHex: Long,
    val logoText: String,
    val tagline: String
)

object StreamingProviders {
    val NETFLIX = StreamingProvider(
        id = "netflix",
        name = "Netflix",
        colorHex = 0xFFE50914,
        secondaryColorHex = 0xFFB81D24,
        logoText = "NETFLIX",
        tagline = "Original Series & Blockbusters"
    )

    val APPLE_TV = StreamingProvider(
        id = "appletv",
        name = "Apple TV+",
        colorHex = 0xFF1C1C1E,
        secondaryColorHex = 0xFF636366,
        logoText = "tv+",
        tagline = "Award-Winning Apple Originals"
    )

    val DISNEY_PLUS = StreamingProvider(
        id = "disneyplus",
        name = "Disney+",
        colorHex = 0xFF113CCF,
        secondaryColorHex = 0xFF0A2472,
        logoText = "Disney+",
        tagline = "Marvel, Star Wars & Pixar"
    )

    val HBO_MAX = StreamingProvider(
        id = "hbomax",
        name = "Max",
        colorHex = 0xFF5822B4,
        secondaryColorHex = 0xFF002BE7,
        logoText = "MAX",
        tagline = "HBO Originals & Warner Bros"
    )

    val PRIME_VIDEO = StreamingProvider(
        id = "prime",
        name = "Prime Video",
        colorHex = 0xFF00A8E1,
        secondaryColorHex = 0xFF005276,
        logoText = "prime video",
        tagline = "Amazon Exclusives & Channels"
    )

    val PARAMOUNT_PLUS = StreamingProvider(
        id = "paramount",
        name = "Paramount+",
        colorHex = 0xFF0064FF,
        secondaryColorHex = 0xFF00388A,
        logoText = "Paramount+",
        tagline = "Live Sports, Hits & Originals"
    )

    val HULU = StreamingProvider(
        id = "hulu",
        name = "Hulu",
        colorHex = 0xFF1CE783,
        secondaryColorHex = 0xFF0F8A4B,
        logoText = "hulu",
        tagline = "FX Series & Next-Day TV"
    )

    val ALL_PROVIDERS = listOf(
        NETFLIX,
        APPLE_TV,
        DISNEY_PLUS,
        HBO_MAX,
        PRIME_VIDEO,
        PARAMOUNT_PLUS,
        HULU
    )
}
