package com.example.data.subtitles

import com.example.data.model.SubtitleCue
import com.example.data.model.SubtitleItem

object SubtitleProvider {

    val AVAILABLE_LANGUAGES = listOf(
        "eng" to "English (Original)",
        "spa" to "Español (Spanish)",
        "fre" to "Français (French)",
        "ger" to "Deutsch (German)",
        "jpn" to "日本語 (Japanese)",
        "por" to "Português (Portuguese)"
    )

    fun getAvailableSubtitles(itemId: String): List<SubtitleItem> {
        return AVAILABLE_LANGUAGES.map { (code, name) ->
            SubtitleItem(
                id = "${itemId}_$code",
                lang = code,
                label = name,
                addonName = "OpenSubtitles v3"
            )
        }
    }

    /**
     * Returns synchronized subtitle cues for a media item
     */
    fun getCues(itemId: String, lang: String): List<SubtitleCue> {
        return when (lang) {
            "spa" -> getSpanishCues(itemId)
            "fre" -> getFrenchCues(itemId)
            "ger" -> getGermanCues(itemId)
            "jpn" -> getJapaneseCues(itemId)
            else -> getEnglishCues(itemId)
        }
    }

    private fun getEnglishCues(itemId: String): List<SubtitleCue> {
        return when (itemId) {
            "blender_big_buck_bunny" -> listOf(
                SubtitleCue(2000L, 6000L, "[Peaceful birds chirping in a sunny forest clearing]"),
                SubtitleCue(7000L, 12000L, "[Big Buck Bunny steps out of his tree burrow and yawns]"),
                SubtitleCue(13000L, 18000L, "[Bunny notices a vibrant blue butterfly resting on an apple]"),
                SubtitleCue(19000L, 25000L, "[Gentle acoustic score swells as he smiles warmly]"),
                SubtitleCue(26000L, 33000L, "[Rustling in the tall branches above]"),
                SubtitleCue(34000L, 41000L, "Frank: 'Look down there, boys! That giant fluffball has no idea.'"),
                SubtitleCue(42000L, 48000L, "Rinky: 'Shall we drop the acorns on his ears?'"),
                SubtitleCue(49000L, 56000L, "Frank: 'Wait for my signal... three, two, one, NOW!'"),
                SubtitleCue(57000L, 64000L, "[Thump! The apple is knocked flying onto Bunny's nose]"),
                SubtitleCue(65000L, 73000L, "[High pitched squirrel giggling echoing through the canopy]"),
                SubtitleCue(74000L, 82000L, "Bunny: 'Enough playing around. It's time for payback.'"),
                SubtitleCue(83000L, 95000L, "[Bunny measures tree vines with engineering precision]"),
                SubtitleCue(96000L, 110000L, "[Suspenseful orchestral music intensifies]")
            )
            "blender_tears_of_steel" -> listOf(
                SubtitleCue(2000L, 7000L, "Thom: 'Celia, look at me. It has been forty years since the fallout.'"),
                SubtitleCue(8000L, 14000L, "Celia: 'The robotic network has surrounded the Oude Kerk square.'"),
                SubtitleCue(15000L, 22000L, "Thom: 'We only have one memory cycle left before the pulse collapses.'"),
                SubtitleCue(23000L, 30000L, "Barley: 'Sensors detecting heavy aerial drones inbound. 30 seconds!'"),
                SubtitleCue(31000L, 39000L, "Celia: 'If we calibrate the neural beam, the war ends today.'"),
                SubtitleCue(40000L, 48000L, "[Deafening plasma cannon fire shaking the concrete bridge]"),
                SubtitleCue(49000L, 58000L, "Thom: 'Initiating emotional transfer sequence. Hold your positions!'")
            )
            "blender_sintel" -> listOf(
                SubtitleCue(2000L, 8000L, "[Bitter howling mountain blizzard echoing across icy ridges]"),
                SubtitleCue(9000L, 16000L, "Sintel: 'Scales... I know you are up there in the lonely towers.'"),
                SubtitleCue(17000L, 24000L, "[Flute melody playing a melancholic traveler theme]"),
                SubtitleCue(25000L, 33000L, "Old Shaman: 'Turn back, wanderer. What was taken can never return the same.'"),
                SubtitleCue(34000L, 43000L, "Sintel: 'He didn't choose to be taken. I promised him I would find him.'"),
                SubtitleCue(44000L, 54000L, "[Roar of a colossal dragon vibrating through the stone caverns]")
            )
            "pd_night_of_the_living_dead" -> listOf(
                SubtitleCue(2000L, 7000L, "Johnny: 'They're coming to get you, Barbara!'"),
                SubtitleCue(8000L, 13000L, "Barbara: 'Stop it, Johnny! You're acting like a child.'"),
                SubtitleCue(14000L, 21000L, "Johnny: 'Look, there comes one now! Walking through the graves.'"),
                SubtitleCue(22000L, 29000L, "Barbara: 'Johnny, please, it's not funny anymore.'"),
                SubtitleCue(30000L, 38000L, "[Violent struggle breaks out against the tombstone]"),
                SubtitleCue(39000L, 48000L, "Barbara: 'Johnny! No! Help! Somebody help me!'")
            )
            "nasa_iss_earth_views" -> listOf(
                SubtitleCue(2000L, 8000L, "Mission Control: 'Station, this is Houston. Orbital camera 1 is now active.'"),
                SubtitleCue(9000L, 17000L, "Flight Engineer: 'Loud and clear Houston. Passing over the Mediterranean coastline.'"),
                SubtitleCue(18000L, 26000L, "Capcom: 'Solar array telemetry is nominal. Sunset transition in 12 minutes.'"),
                SubtitleCue(27000L, 38000L, "[Calm cosmic radio static with breathtaking curvature of Earth below]")
            )
            else -> listOf(
                SubtitleCue(2000L, 7000L, "[Dynamic cinematic score begins]"),
                SubtitleCue(8000L, 15000L, "[Full High Definition audio stream synchronized]"),
                SubtitleCue(16000L, 24000L, "[Dialogue track in progress]"),
                SubtitleCue(25000L, 35000L, "[Immersive surround atmosphere active]")
            )
        }
    }

    private fun getSpanishCues(itemId: String): List<SubtitleCue> {
        return when (itemId) {
            "blender_big_buck_bunny" -> listOf(
                SubtitleCue(2000L, 6000L, "[Aves cantando pacíficamente en el bosque soleado]"),
                SubtitleCue(7000L, 12000L, "[El conejo Big Buck sale de su madriguera y bosteza]"),
                SubtitleCue(13000L, 18000L, "[El conejo observa una mariposa azul descansando sobre una manzana]"),
                SubtitleCue(19000L, 25000L, "[La música acústica crece mientras sonríe calurosamente]"),
                SubtitleCue(26000L, 33000L, "[Crujidos en las ramas altas del árbol]"),
                SubtitleCue(34000L, 41000L, "Frank: '¡Miren allá abajo, chicos! No tiene ni idea.'"),
                SubtitleCue(42000L, 48000L, "Rinky: '¿Le arrojamos las bellotas en las orejas?'"),
                SubtitleCue(49000L, 56000L, "Frank: 'Esperen mi señal... tres, dos, uno, ¡YA!'"),
                SubtitleCue(57000L, 64000L, "[¡Pum! La manzana sale volando directo a su nariz]"),
                SubtitleCue(65000L, 73000L, "[Risas agudas de ardillas resonando en el bosque]"),
                SubtitleCue(74000L, 82000L, "Conejo: 'Se acabó el juego. Es hora de la revancha.'")
            )
            "blender_tears_of_steel" -> listOf(
                SubtitleCue(2000L, 7000L, "Thom: 'Celia, mírame. Han pasado cuarenta años desde la caída.'"),
                SubtitleCue(8000L, 14000L, "Celia: 'La red robótica ha rodeado por completo la plaza.'"),
                SubtitleCue(15000L, 22000L, "Thom: 'Solo nos queda un ciclo de memoria antes del pulso.'"),
                SubtitleCue(23000L, 30000L, "Barley: 'Sensores detectan drones aéreos pesados. ¡30 segundos!'"),
                SubtitleCue(31000L, 39000L, "Celia: 'Si calibramos el rayo neural, la guerra termina hoy.'")
            )
            "pd_night_of_the_living_dead" -> listOf(
                SubtitleCue(2000L, 7000L, "Johnny: '¡Vienen a buscarte, Barbara!'"),
                SubtitleCue(8000L, 13000L, "Barbara: '¡Basta, Johnny! Te estás comportando como un niño.'"),
                SubtitleCue(14000L, 21000L, "Johnny: '¡Mira, ahí viene uno! Caminando entre las tumbas.'"),
                SubtitleCue(22000L, 29000L, "Barbara: 'Johnny, por favor, ya no tiene gracia.'")
            )
            else -> listOf(
                SubtitleCue(2000L, 8000L, "[Banda sonora cinematográfica en reproducción]"),
                SubtitleCue(9000L, 18000L, "[Diálogo sincronizado en español]")
            )
        }
    }

    private fun getFrenchCues(itemId: String): List<SubtitleCue> {
        return listOf(
            SubtitleCue(2000L, 8000L, "[Musique orchestrale française en cours de lecture]"),
            SubtitleCue(9000L, 16000L, "Protagoniste: 'Regardez l'horizon, le voyage ne fait que commencer.'"),
            SubtitleCue(17000L, 25000L, "[Bruit d'ambiance et effets sonores haute définition]")
        )
    }

    private fun getGermanCues(itemId: String): List<SubtitleCue> {
        return listOf(
            SubtitleCue(2000L, 8000L, "[Dramatische Orchestermusik beginnt]"),
            SubtitleCue(9000L, 16000L, "Sprecher: 'Die Zeit für die letzte Entscheidung ist gekommen.'"),
            SubtitleCue(17000L, 25000L, "[Präzise deutsche Untertitel aktiv]")
        )
    }

    private fun getJapaneseCues(itemId: String): List<SubtitleCue> {
        return listOf(
            SubtitleCue(2000L, 8000L, "[壮大なオーケストラの楽曲が響き渡る]"),
            SubtitleCue(9000L, 16000L, "「諦めるな、運命はまだ我々の手にある」"),
            SubtitleCue(17000L, 25000L, "[超高精細サラウンド音響が再生中]")
        )
    }
}
