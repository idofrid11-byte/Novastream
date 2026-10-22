package com.example.data.api

import com.example.data.model.MetaDetail
import com.example.data.model.MetaItem
import com.example.data.model.StreamItem
import com.example.data.model.VideoEpisode

object StreamingServicesData {

    val NETFLIX_ITEMS = listOf(
        MetaItem(
            id = "stream_netflix_stranger_things",
            type = "series",
            name = "Stranger Things",
            poster = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=800",
            background = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1600",
            description = "When a young boy vanishes, a small town uncovers a mystery involving secret experiments, terrifying supernatural forces and one strange little girl.",
            releaseInfo = "2016-2025",
            imdbRating = "8.7",
            genres = listOf("Sci-Fi", "Horror", "Drama"),
            addonName = "Netflix Originals",
            addonId = "service_netflix"
        ),
        MetaItem(
            id = "stream_netflix_wednesday",
            type = "series",
            name = "Wednesday",
            poster = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?q=80&w=800",
            background = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1600",
            description = "Follows Wednesday Addams' years as a student, attempting to master her emerging psychic ability and solve the mystery that embroiled her parents.",
            releaseInfo = "2022-",
            imdbRating = "8.1",
            genres = listOf("Comedy", "Crime", "Fantasy"),
            addonName = "Netflix Originals",
            addonId = "service_netflix"
        ),
        MetaItem(
            id = "stream_netflix_witcher",
            type = "series",
            name = "The Witcher",
            poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=800",
            background = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1600",
            description = "Geralt of Rivia, a solitary monster hunter, struggles to find his place in a world where people often prove more wicked than beasts.",
            releaseInfo = "2019-",
            imdbRating = "8.0",
            genres = listOf("Action", "Adventure", "Fantasy"),
            addonName = "Netflix Originals",
            addonId = "service_netflix"
        ),
        MetaItem(
            id = "stream_netflix_glass_onion",
            type = "movie",
            name = "Glass Onion: Knives Out",
            poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=800",
            background = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1600",
            description = "Famed Southern detective Benoit Blanc travels to Greece for his latest case, peeling back the layers of an opulent puzzle with a tech billionaire.",
            releaseInfo = "2022",
            imdbRating = "7.2",
            genres = listOf("Comedy", "Crime", "Drama"),
            addonName = "Netflix Originals",
            addonId = "service_netflix"
        )
    )

    val APPLE_TV_ITEMS = listOf(
        MetaItem(
            id = "stream_appletv_severance",
            type = "series",
            name = "Severance",
            poster = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=800",
            background = "https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1600",
            description = "Mark leads a team of office workers whose memories have been surgically divided between their work and personal lives.",
            releaseInfo = "2022-",
            imdbRating = "8.7",
            genres = listOf("Drama", "Mystery", "Sci-Fi"),
            addonName = "Apple TV+",
            addonId = "service_appletv"
        ),
        MetaItem(
            id = "stream_appletv_ted_lasso",
            type = "series",
            name = "Ted Lasso",
            poster = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=800",
            background = "https://images.unsplash.com/photo-1522778119026-d647f0596c20?q=80&w=1600",
            description = "American college football coach Ted Lasso heads to London to manage AFC Richmond, a struggling English Premier League football team.",
            releaseInfo = "2020-2023",
            imdbRating = "8.8",
            genres = listOf("Comedy", "Drama", "Sport"),
            addonName = "Apple TV+",
            addonId = "service_appletv"
        ),
        MetaItem(
            id = "stream_appletv_silo",
            type = "series",
            name = "Silo",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800",
            background = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1600",
            description = "Men and women live in a giant subterranean silo with regulations which they believe are meant to protect them from the toxic ruined world outside.",
            releaseInfo = "2023-",
            imdbRating = "8.1",
            genres = listOf("Drama", "Sci-Fi"),
            addonName = "Apple TV+",
            addonId = "service_appletv"
        )
    )

    val DISNEY_ITEMS = listOf(
        MetaItem(
            id = "stream_disney_mandalorian",
            type = "series",
            name = "The Mandalorian",
            poster = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=800",
            background = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=1600",
            description = "The travels of a lone bounty hunter in the outer reaches of the galaxy, far from the authority of the New Republic.",
            releaseInfo = "2019-",
            imdbRating = "8.7",
            genres = listOf("Action", "Adventure", "Sci-Fi"),
            addonName = "Disney+",
            addonId = "service_disneyplus"
        ),
        MetaItem(
            id = "stream_disney_loki",
            type = "series",
            name = "Loki",
            poster = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=800",
            background = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1600",
            description = "The mercurial villain Loki resumes his role as the God of Mischief following the events of Avengers: Endgame.",
            releaseInfo = "2021-2023",
            imdbRating = "8.2",
            genres = listOf("Action", "Adventure", "Fantasy"),
            addonName = "Disney+",
            addonId = "service_disneyplus"
        ),
        MetaItem(
            id = "stream_disney_andor",
            type = "series",
            name = "Andor",
            poster = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=800",
            background = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1600",
            description = "Prequel series to Star Wars' 'Rogue One'. In an era filled with danger, deception and intrigue, Cassian will embark on the path that is destined to turn him into a rebel hero.",
            releaseInfo = "2022-",
            imdbRating = "8.4",
            genres = listOf("Action", "Adventure", "Drama"),
            addonName = "Disney+",
            addonId = "service_disneyplus"
        )
    )

    val HBO_MAX_ITEMS = listOf(
        MetaItem(
            id = "stream_max_house_of_dragon",
            type = "series",
            name = "House of the Dragon",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800",
            background = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=1600",
            description = "An internal succession war within House Targaryen at the height of its power, 172 years before the birth of Daenerys Targaryen.",
            releaseInfo = "2022-",
            imdbRating = "8.4",
            genres = listOf("Action", "Adventure", "Drama"),
            addonName = "Max",
            addonId = "service_hbomax"
        ),
        MetaItem(
            id = "stream_max_last_of_us",
            type = "series",
            name = "The Last of Us",
            poster = "https://images.unsplash.com/photo-1509248961158-e54f6934749c?q=80&w=800",
            background = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1600",
            description = "After a global pandemic destroys civilization, a hardened survivor takes charge of a 14-year-old girl who may be humanity's last hope.",
            releaseInfo = "2023-",
            imdbRating = "8.8",
            genres = listOf("Action", "Adventure", "Drama"),
            addonName = "Max",
            addonId = "service_hbomax"
        ),
        MetaItem(
            id = "stream_max_succession",
            type = "series",
            name = "Succession",
            poster = "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=800",
            background = "https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1600",
            description = "The Roy family is known for controlling the biggest media and entertainment company in the world. However, their world changes when their aging father steps down.",
            releaseInfo = "2018-2023",
            imdbRating = "8.9",
            genres = listOf("Drama"),
            addonName = "Max",
            addonId = "service_hbomax"
        )
    )

    val PRIME_ITEMS = listOf(
        MetaItem(
            id = "stream_prime_the_boys",
            type = "series",
            name = "The Boys",
            poster = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=800",
            background = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?q=80&w=1600",
            description = "A fun and irreverent take on what happens when superheroes—who are as popular as celebrities, as influential as politicians, and as revered as gods—abuse their superpowers.",
            releaseInfo = "2019-",
            imdbRating = "8.7",
            genres = listOf("Action", "Comedy", "Drama"),
            addonName = "Prime Video",
            addonId = "service_prime"
        ),
        MetaItem(
            id = "stream_prime_fallout",
            type = "series",
            name = "Fallout",
            poster = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=800",
            background = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1600",
            description = "In a future, post-apocalyptic Los Angeles brought about by nuclear decimation, citizens must live in underground bunkers to protect themselves from radiation and mutants.",
            releaseInfo = "2024-",
            imdbRating = "8.4",
            genres = listOf("Action", "Adventure", "Drama"),
            addonName = "Prime Video",
            addonId = "service_prime"
        ),
        MetaItem(
            id = "stream_prime_reacher",
            type = "series",
            name = "Reacher",
            poster = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=800",
            background = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1600",
            description = "When retired Military Police Officer Jack Reacher is arrested for a murder he did not commit, he finds himself in the middle of a deadly conspiracy.",
            releaseInfo = "2022-",
            imdbRating = "8.1",
            genres = listOf("Action", "Crime", "Drama"),
            addonName = "Prime Video",
            addonId = "service_prime"
        )
    )

    val PARAMOUNT_ITEMS = listOf(
        MetaItem(
            id = "stream_paramount_yellowstone",
            type = "series",
            name = "Yellowstone",
            poster = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?q=80&w=800",
            background = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=1600",
            description = "A ranching family in Montana faces off against others encroaching on their land.",
            releaseInfo = "2018-2024",
            imdbRating = "8.7",
            genres = listOf("Drama", "Western"),
            addonName = "Paramount+",
            addonId = "service_paramount"
        ),
        MetaItem(
            id = "stream_paramount_tulsa_king",
            type = "series",
            name = "Tulsa King",
            poster = "https://images.unsplash.com/photo-1485846234645-a62644f84728?q=80&w=800",
            background = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?q=80&w=1600",
            description = "Following his release from prison, Mafia capo Dwight 'The General' Manfredi is exiled to Tulsa, Oklahoma, where he builds a new criminal empire.",
            releaseInfo = "2022-",
            imdbRating = "8.0",
            genres = listOf("Crime", "Drama"),
            addonName = "Paramount+",
            addonId = "service_paramount"
        )
    )

    val HULU_ITEMS = listOf(
        MetaItem(
            id = "stream_hulu_the_bear",
            type = "series",
            name = "The Bear",
            poster = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?q=80&w=800",
            background = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?q=80&w=1600",
            description = "A young chef from the fine dining world returns to Chicago to run his family's sandwich shop after a heartbreaking death.",
            releaseInfo = "2022-",
            imdbRating = "8.6",
            genres = listOf("Comedy", "Drama"),
            addonName = "Hulu",
            addonId = "service_hulu"
        ),
        MetaItem(
            id = "stream_hulu_shogun",
            type = "series",
            name = "Shōgun",
            poster = "https://images.unsplash.com/photo-1528164344705-475426879c0d?q=80&w=800",
            background = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?q=80&w=1600",
            description = "When a mysterious European ship is found marooned in a nearby fishing village, Lord Yoshii Toranaga discovers secrets that could tip the scales of power.",
            releaseInfo = "2024",
            imdbRating = "8.7",
            genres = listOf("Adventure", "Drama", "History"),
            addonName = "Hulu",
            addonId = "service_hulu"
        )
    )

    val ALL_STREAMING_ITEMS: List<MetaItem> =
        NETFLIX_ITEMS + APPLE_TV_ITEMS + DISNEY_ITEMS + HBO_MAX_ITEMS + PRIME_ITEMS + PARAMOUNT_ITEMS + HULU_ITEMS

    fun getItemsForProvider(providerId: String): List<MetaItem> {
        return when (providerId.lowercase()) {
            "netflix" -> NETFLIX_ITEMS
            "appletv" -> APPLE_TV_ITEMS
            "disneyplus" -> DISNEY_ITEMS
            "hbomax", "max" -> HBO_MAX_ITEMS
            "prime" -> PRIME_ITEMS
            "paramount" -> PARAMOUNT_ITEMS
            "hulu" -> HULU_ITEMS
            else -> ALL_STREAMING_ITEMS
        }
    }

    fun getStreamingMeta(id: String): MetaDetail? {
        val item = ALL_STREAMING_ITEMS.find { it.id == id } ?: return null
        val episodes = if (item.type == "series") {
            listOf(
                VideoEpisode(
                    id = "${item.id}:1:1",
                    title = "Episode 1: Pilot & Arrival",
                    season = 1,
                    episode = 1,
                    released = "2023-01-15",
                    thumbnail = item.background ?: item.poster,
                    overview = "The journey begins as characters confront new realities and unforeseen crises.",
                    seasonTitle = "Season 1: Premiere"
                ),
                VideoEpisode(
                    id = "${item.id}:1:2",
                    title = "Episode 2: Deep Shadows",
                    season = 1,
                    episode = 2,
                    released = "2023-01-22",
                    thumbnail = item.poster,
                    overview = "Secrets unravel under pressure as stakes escalate dramatically.",
                    seasonTitle = "Season 1: Premiere"
                ),
                VideoEpisode(
                    id = "${item.id}:1:3",
                    title = "Episode 3: The Turning Point",
                    season = 1,
                    episode = 3,
                    released = "2023-01-29",
                    thumbnail = item.background,
                    overview = "An unexpected revelation forces alliances to fracture and evolve.",
                    seasonTitle = "Season 1: Premiere"
                ),
                VideoEpisode(
                    id = "${item.id}:2:1",
                    title = "Episode 1: Reckoning",
                    season = 2,
                    episode = 1,
                    released = "2024-03-10",
                    thumbnail = item.background,
                    overview = "Aftershock from season one creates a thrilling new battlefield.",
                    seasonTitle = "Season 2: Escalation"
                ),
                VideoEpisode(
                    id = "${item.id}:2:2",
                    title = "Episode 2: No Return",
                    season = 2,
                    episode = 2,
                    released = "2024-03-17",
                    thumbnail = item.poster,
                    overview = "Every choice brings consequences in the race against time.",
                    seasonTitle = "Season 2: Escalation"
                )
            )
        } else null

        return MetaDetail(
            id = item.id,
            type = item.type,
            name = item.name,
            poster = item.poster,
            background = item.background,
            description = item.description,
            releaseInfo = item.releaseInfo,
            imdbRating = item.imdbRating,
            genres = item.genres,
            runtime = if (item.type == "movie") "135 min" else "55 min",
            director = listOf("Showrunner / Executive Producers"),
            cast = listOf("Ensemble Cast", "Award Winner", "Lead Protagonist", "Supporting Star"),
            videos = episodes
        )
    }

    fun getStreamingStreams(targetId: String): List<StreamItem> {
        val streamUrl = when {
            targetId.contains("stranger_things") || targetId.contains("severance") ->
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            targetId.contains("mandalorian") || targetId.contains("the_boys") ->
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
            targetId.contains("house_of_dragon") || targetId.contains("last_of_us") ->
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            else ->
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        }

        return listOf(
            StreamItem(
                title = "Stremio Auto-Play Direct CDN [4K UHD HDR]",
                name = "Stremio Fast Stream Engine",
                url = streamUrl,
                quality = "4K UHD",
                addonName = "Stremio Official CDN"
            ),
            StreamItem(
                title = "Stremio High-Speed 1080p Mirror",
                name = "Stremio CDN",
                url = streamUrl,
                quality = "1080p",
                addonName = "Stremio Official CDN"
            )
        )
    }
}
