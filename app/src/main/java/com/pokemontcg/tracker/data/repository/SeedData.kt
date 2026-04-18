package com.pokemontcg.tracker.data.repository

import com.pokemontcg.tracker.data.db.AppDatabase
import com.pokemontcg.tracker.data.model.PokemonCard
import com.pokemontcg.tracker.data.model.PokemonSet

/**
 * Seeds the database with a selection of real Pokemon TCG sets and their cards.
 * In a real app, you'd populate this from a full dataset JSON asset.
 */
object SeedData {

    suspend fun seedDatabase(db: AppDatabase) {
        val sets = getSets()
        db.setDao().insertSets(sets)

        sets.forEach { set ->
            db.cardDao().insertCards(getCardsForSet(set.id))
        }
    }

    private fun getSets(): List<PokemonSet> = listOf(
        PokemonSet("sv1", "Scarlet & Violet", "Scarlet & Violet", 198, 258, "2023/03/31"),
        PokemonSet("sv2", "Paldea Evolved", "Scarlet & Violet", 193, 279, "2023/06/09"),
        PokemonSet("sv3", "Obsidian Flames", "Scarlet & Violet", 197, 230, "2023/08/11"),
        PokemonSet("sv3pt5", "151", "Scarlet & Violet", 165, 207, "2023/09/22"),
        PokemonSet("sv4", "Paradox Rift", "Scarlet & Violet", 182, 266, "2023/11/03"),
        PokemonSet("sv4pt5", "Paldean Fates", "Scarlet & Violet", 91, 245, "2024/01/26"),
        PokemonSet("sv5", "Temporal Forces", "Scarlet & Violet", 162, 218, "2024/03/22"),
        PokemonSet("sv6", "Twilight Masquerade", "Scarlet & Violet", 167, 226, "2024/05/24"),
        PokemonSet("sv6pt5", "Shrouded Fable", "Scarlet & Violet", 64, 99, "2024/08/02"),
        PokemonSet("sv7", "Stellar Crown", "Scarlet & Violet", 142, 175, "2024/09/13"),
        PokemonSet("sv8", "Surging Sparks", "Scarlet & Violet", 191, 252, "2024/11/08"),
        PokemonSet("swsh1", "Sword & Shield", "Sword & Shield", 202, 216, "2020/02/07"),
        PokemonSet("swsh2", "Rebel Clash", "Sword & Shield", 192, 209, "2020/05/01"),
        PokemonSet("swsh3", "Darkness Ablaze", "Sword & Shield", 185, 201, "2020/08/14"),
        PokemonSet("swsh4", "Vivid Voltage", "Sword & Shield", 185, 203, "2020/11/13"),
        PokemonSet("swsh5", "Battle Styles", "Sword & Shield", 163, 183, "2021/03/19"),
        PokemonSet("swsh6", "Chilling Reign", "Sword & Shield", 198, 233, "2021/06/18"),
        PokemonSet("swsh7", "Evolving Skies", "Sword & Shield", 203, 237, "2021/08/27"),
        PokemonSet("swsh8", "Fusion Strike", "Sword & Shield", 260, 284, "2021/11/12"),
        PokemonSet("swsh9", "Brilliant Stars", "Sword & Shield", 172, 186, "2022/02/25"),
        PokemonSet("swsh10", "Astral Radiance", "Sword & Shield", 189, 246, "2022/05/27"),
        PokemonSet("swsh11", "Lost Origin", "Sword & Shield", 196, 231, "2022/09/09"),
        PokemonSet("swsh12", "Silver Tempest", "Sword & Shield", 195, 245, "2022/11/11"),
        PokemonSet("sm1", "Sun & Moon", "Sun & Moon", 149, 173, "2017/02/03"),
        PokemonSet("sm2", "Guardians Rising", "Sun & Moon", 145, 169, "2017/05/05"),
        PokemonSet("sm3", "Burning Shadows", "Sun & Moon", 147, 169, "2017/08/04"),
        PokemonSet("sm35", "Shining Legends", "Sun & Moon", 73, 78, "2017/10/06"),
        PokemonSet("sm4", "Crimson Invasion", "Sun & Moon", 111, 124, "2017/11/03"),
        PokemonSet("sm5", "Ultra Prism", "Sun & Moon", 156, 173, "2018/02/02"),
        PokemonSet("sm6", "Forbidden Light", "Sun & Moon", 131, 146, "2018/05/04"),
        PokemonSet("base1", "Base Set", "Base", 102, 102, "1999/01/09"),
        PokemonSet("base2", "Jungle", "Base", 64, 64, "1999/06/16"),
        PokemonSet("base3", "Fossil", "Base", 62, 62, "1999/10/10"),
        PokemonSet("base4", "Base Set 2", "Base", 130, 130, "2000/02/24"),
        PokemonSet("gym1", "Gym Heroes", "Gym", 132, 132, "2000/08/14"),
        PokemonSet("gym2", "Gym Challenge", "Gym", 132, 132, "2000/10/16"),
        PokemonSet("neo1", "Neo Genesis", "Neo", 111, 111, "2000/12/16"),
        PokemonSet("neo2", "Neo Discovery", "Neo", 75, 75, "2001/06/01"),
        PokemonSet("neo3", "Neo Revelation", "Neo", 64, 64, "2001/09/21"),
        PokemonSet("neo4", "Neo Destiny", "Neo", 105, 113, "2002/02/28"),
        PokemonSet("ex1", "Ruby & Sapphire", "EX", 109, 109, "2003/07/01"),
        PokemonSet("ex2", "Sandstorm", "EX", 100, 100, "2003/09/17"),
        PokemonSet("dp1", "Diamond & Pearl", "Diamond & Pearl", 130, 130, "2007/05/23"),
        PokemonSet("dp2", "Mysterious Treasures", "Diamond & Pearl", 123, 123, "2007/08/22"),
        PokemonSet("hgss1", "HeartGold & SoulSilver", "HeartGold & SoulSilver", 123, 123, "2010/02/10"),
        PokemonSet("bw1", "Black & White", "Black & White", 114, 115, "2011/04/25"),
        PokemonSet("xy1", "XY", "XY", 146, 146, "2014/02/05"),
        PokemonSet("xy2", "Flashfire", "XY", 106, 106, "2014/05/07"),
        PokemonSet("xy3", "Furious Fists", "XY", 111, 111, "2014/08/13"),
        PokemonSet("xy4", "Phantom Forces", "XY", 119, 122, "2014/11/05")
    )

    /**
     * Generates sample cards for a set.
     * In a real app, you'd load from a bundled JSON asset file.
     */
    private fun getCardsForSet(setId: String): List<PokemonCard> {
        return when (setId) {
            "sv1" -> generateSV1Cards()
            "base1" -> generateBase1Cards()
            "swsh1" -> generateSwSh1Cards()
            else -> generateGenericCards(setId, getSetInfo(setId))
        }
    }

    private fun getSetInfo(setId: String): Pair<Int, String> {
        // Returns (total, series) for generic generation
        return when (setId) {
            "sv2" -> Pair(193, "Scarlet & Violet")
            "sv3" -> Pair(197, "Scarlet & Violet")
            "sv3pt5" -> Pair(165, "Scarlet & Violet")
            "sv4" -> Pair(182, "Scarlet & Violet")
            "sv4pt5" -> Pair(91, "Scarlet & Violet")
            "sv5" -> Pair(162, "Scarlet & Violet")
            "sv6" -> Pair(167, "Scarlet & Violet")
            "sv6pt5" -> Pair(64, "Scarlet & Violet")
            "sv7" -> Pair(142, "Scarlet & Violet")
            "sv8" -> Pair(191, "Scarlet & Violet")
            "swsh2" -> Pair(192, "Sword & Shield")
            "swsh3" -> Pair(185, "Sword & Shield")
            "swsh4" -> Pair(185, "Sword & Shield")
            "swsh5" -> Pair(163, "Sword & Shield")
            "swsh6" -> Pair(198, "Sword & Shield")
            "swsh7" -> Pair(203, "Sword & Shield")
            "swsh8" -> Pair(260, "Sword & Shield")
            "swsh9" -> Pair(172, "Sword & Shield")
            "swsh10" -> Pair(189, "Sword & Shield")
            "swsh11" -> Pair(196, "Sword & Shield")
            "swsh12" -> Pair(195, "Sword & Shield")
            else -> Pair(100, "Other")
        }
    }

    private fun generateGenericCards(setId: String, info: Pair<Int, String>): List<PokemonCard> {
        val rarities = listOf("Common", "Common", "Common", "Uncommon", "Uncommon", "Rare", "Rare Holo", "Rare Ultra", "Rare Secret")
        val types = listOf("Fire", "Water", "Grass", "Lightning", "Psychic", "Fighting", "Darkness", "Metal", "Dragon", "Fairy", "Colorless")
        val pokemonNames = listOf(
            "Bulbasaur", "Charmander", "Squirtle", "Caterpie", "Metapod", "Butterfree",
            "Weedle", "Kakuna", "Beedrill", "Pidgey", "Pidgeotto", "Pidgeot",
            "Rattata", "Raticate", "Spearow", "Fearow", "Ekans", "Arbok",
            "Pikachu", "Raichu", "Sandshrew", "Sandslash", "Nidoran♀", "Nidorina"
        )
        val trainerNames = listOf("Professor's Research", "Boss's Orders", "Marnie", "Quick Ball", "Evolution Incense", "Air Balloon")
        val energyNames = listOf("Fire Energy", "Water Energy", "Grass Energy", "Lightning Energy", "Psychic Energy", "Fighting Energy")

        return (1..info.first).map { num ->
            val supertype = when {
                num <= info.first * 0.65 -> "Pokémon"
                num <= info.first * 0.85 -> "Trainer"
                else -> "Energy"
            }
            val name = when (supertype) {
                "Trainer" -> trainerNames[(num % trainerNames.size)]
                "Energy" -> energyNames[(num % energyNames.size)]
                else -> pokemonNames[(num % pokemonNames.size)]
            }
            PokemonCard(
                id = "$setId-$num",
                name = name,
                number = num.toString(),
                setId = setId,
                rarity = rarities[(num % rarities.size)],
                types = types[(num % types.size)],
                supertype = supertype
            )
        }
    }

    private fun generateSV1Cards(): List<PokemonCard> {
        val cards = mutableListOf<PokemonCard>()
        // Grass
        cards.addAll(listOf(
            PokemonCard("sv1-1", "Sprigatito", "1", "sv1", "Common", "Grass", "Pokémon"),
            PokemonCard("sv1-2", "Floragato", "2", "sv1", "Uncommon", "Grass", "Pokémon"),
            PokemonCard("sv1-3", "Meowscarada", "3", "sv1", "Rare Holo", "Grass", "Pokémon"),
            PokemonCard("sv1-4", "Meowscarada ex", "4", "sv1", "Double Rare", "Grass", "Pokémon"),
            PokemonCard("sv1-5", "Smoliv", "5", "sv1", "Common", "Grass", "Pokémon"),
            PokemonCard("sv1-6", "Dolliv", "6", "sv1", "Uncommon", "Grass", "Pokémon"),
            PokemonCard("sv1-7", "Arboliva", "7", "sv1", "Rare", "Grass", "Pokémon"),
            PokemonCard("sv1-8", "Toedscool", "8", "sv1", "Common", "Grass", "Pokémon"),
            PokemonCard("sv1-9", "Toedscruel", "9", "sv1", "Rare", "Grass", "Pokémon"),
            PokemonCard("sv1-10", "Capsakid", "10", "sv1", "Common", "Grass", "Pokémon"),
            PokemonCard("sv1-11", "Scovillain", "11", "sv1", "Rare", "Grass", "Pokémon"),
            PokemonCard("sv1-12", "Bramblin", "12", "sv1", "Common", "Grass", "Pokémon"),
            PokemonCard("sv1-13", "Brambleghast", "13", "sv1", "Uncommon", "Grass", "Pokémon"),
            // Fire
            PokemonCard("sv1-14", "Fuecoco", "14", "sv1", "Common", "Fire", "Pokémon"),
            PokemonCard("sv1-15", "Crocalor", "15", "sv1", "Uncommon", "Fire", "Pokémon"),
            PokemonCard("sv1-16", "Skeledirge", "16", "sv1", "Rare Holo", "Fire", "Pokémon"),
            PokemonCard("sv1-17", "Skeledirge ex", "17", "sv1", "Double Rare", "Fire", "Pokémon"),
            PokemonCard("sv1-18", "Charcadet", "18", "sv1", "Common", "Fire", "Pokémon"),
            PokemonCard("sv1-19", "Armarouge", "19", "sv1", "Rare Holo", "Fire", "Pokémon"),
            PokemonCard("sv1-20", "Armarouge ex", "20", "sv1", "Double Rare", "Fire", "Pokémon"),
            PokemonCard("sv1-21", "Ceruledge", "21", "sv1", "Rare Holo", "Fire", "Pokémon"),
            // Water
            PokemonCard("sv1-22", "Quaxly", "22", "sv1", "Common", "Water", "Pokémon"),
            PokemonCard("sv1-23", "Quaxwell", "23", "sv1", "Uncommon", "Water", "Pokémon"),
            PokemonCard("sv1-24", "Quaquaval", "24", "sv1", "Rare Holo", "Water", "Pokémon"),
            PokemonCard("sv1-25", "Quaquaval ex", "25", "sv1", "Double Rare", "Water", "Pokémon"),
            PokemonCard("sv1-26", "Wiglett", "26", "sv1", "Common", "Water", "Pokémon"),
            PokemonCard("sv1-27", "Wugtrio", "27", "sv1", "Uncommon", "Water", "Pokémon"),
            PokemonCard("sv1-28", "Finizen", "28", "sv1", "Common", "Water", "Pokémon"),
            PokemonCard("sv1-29", "Palafin", "29", "sv1", "Rare Holo", "Water", "Pokémon"),
            PokemonCard("sv1-30", "Palafin ex", "30", "sv1", "Double Rare", "Water", "Pokémon"),
            // Trainers
            PokemonCard("sv1-170", "Arven", "170", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-171", "Boss's Orders (Geeta)", "171", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-172", "Jacq", "172", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-173", "Klara", "173", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-174", "Iono", "174", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-175", "Miriam", "175", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-176", "Nemona", "176", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-177", "Penny", "177", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-178", "Professor Sada's Vitality", "178", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-179", "Professor Turo's Scenario", "179", "sv1", "Uncommon", "", "Trainer"),
            PokemonCard("sv1-180", "Nest Ball", "180", "sv1", "Common", "", "Trainer"),
            PokemonCard("sv1-181", "Switch", "181", "sv1", "Common", "", "Trainer"),
            PokemonCard("sv1-182", "Ultra Ball", "182", "sv1", "Uncommon", "", "Trainer"),
            // Energy
            PokemonCard("sv1-193", "Grass Energy", "193", "sv1", "Common", "Grass", "Energy"),
            PokemonCard("sv1-194", "Fire Energy", "194", "sv1", "Common", "Fire", "Energy"),
            PokemonCard("sv1-195", "Water Energy", "195", "sv1", "Common", "Water", "Energy"),
            PokemonCard("sv1-196", "Lightning Energy", "196", "sv1", "Common", "Lightning", "Energy"),
            PokemonCard("sv1-197", "Psychic Energy", "197", "sv1", "Common", "Psychic", "Energy"),
            PokemonCard("sv1-198", "Fighting Energy", "198", "sv1", "Common", "Fighting", "Energy")
        ))
        return cards
    }

    private fun generateBase1Cards(): List<PokemonCard> = listOf(
        PokemonCard("base1-1", "Alakazam", "1", "base1", "Rare Holo", "Psychic", "Pokémon"),
        PokemonCard("base1-2", "Blastoise", "2", "base1", "Rare Holo", "Water", "Pokémon"),
        PokemonCard("base1-3", "Chansey", "3", "base1", "Rare Holo", "Colorless", "Pokémon"),
        PokemonCard("base1-4", "Charizard", "4", "base1", "Rare Holo", "Fire", "Pokémon"),
        PokemonCard("base1-5", "Clefairy", "5", "base1", "Rare Holo", "Colorless", "Pokémon"),
        PokemonCard("base1-6", "Gyarados", "6", "base1", "Rare Holo", "Water", "Pokémon"),
        PokemonCard("base1-7", "Hitmonchan", "7", "base1", "Rare Holo", "Fighting", "Pokémon"),
        PokemonCard("base1-8", "Machamp", "8", "base1", "Rare Holo", "Fighting", "Pokémon"),
        PokemonCard("base1-9", "Magneton", "9", "base1", "Rare Holo", "Lightning", "Pokémon"),
        PokemonCard("base1-10", "Mewtwo", "10", "base1", "Rare Holo", "Psychic", "Pokémon"),
        PokemonCard("base1-11", "Nidoking", "11", "base1", "Rare Holo", "Psychic", "Pokémon"),
        PokemonCard("base1-12", "Ninetales", "12", "base1", "Rare Holo", "Fire", "Pokémon"),
        PokemonCard("base1-13", "Poliwrath", "13", "base1", "Rare Holo", "Water", "Pokémon"),
        PokemonCard("base1-14", "Raichu", "14", "base1", "Rare Holo", "Lightning", "Pokémon"),
        PokemonCard("base1-15", "Scyther", "15", "base1", "Rare Holo", "Grass", "Pokémon"),
        PokemonCard("base1-16", "Venusaur", "16", "base1", "Rare Holo", "Grass", "Pokémon"),
        PokemonCard("base1-17", "Zapdos", "17", "base1", "Rare Holo", "Lightning", "Pokémon"),
        PokemonCard("base1-18", "Beedrill", "18", "base1", "Rare", "Grass", "Pokémon"),
        PokemonCard("base1-19", "Dragonair", "19", "base1", "Rare", "Colorless", "Pokémon"),
        PokemonCard("base1-20", "Dugtrio", "20", "base1", "Rare", "Fighting", "Pokémon"),
        PokemonCard("base1-21", "Electabuzz", "21", "base1", "Rare", "Lightning", "Pokémon"),
        PokemonCard("base1-22", "Electrode", "22", "base1", "Rare", "Lightning", "Pokémon"),
        PokemonCard("base1-23", "Pidgeotto", "23", "base1", "Rare", "Colorless", "Pokémon"),
        PokemonCard("base1-24", "Arcanine", "24", "base1", "Rare", "Fire", "Pokémon"),
        PokemonCard("base1-25", "Charmeleon", "25", "base1", "Uncommon", "Fire", "Pokémon"),
        PokemonCard("base1-26", "Haunter", "26", "base1", "Uncommon", "Psychic", "Pokémon"),
        PokemonCard("base1-27", "Ivysaur", "27", "base1", "Uncommon", "Grass", "Pokémon"),
        PokemonCard("base1-28", "Jynx", "28", "base1", "Uncommon", "Psychic", "Pokémon"),
        PokemonCard("base1-29", "Kadabra", "29", "base1", "Uncommon", "Psychic", "Pokémon"),
        PokemonCard("base1-30", "Wartortle", "30", "base1", "Uncommon", "Water", "Pokémon"),
        PokemonCard("base1-45", "Bulbasaur", "45", "base1", "Common", "Grass", "Pokémon"),
        PokemonCard("base1-46", "Caterpie", "46", "base1", "Common", "Grass", "Pokémon"),
        PokemonCard("base1-47", "Charmander", "47", "base1", "Common", "Fire", "Pokémon"),
        PokemonCard("base1-48", "Diglett", "48", "base1", "Common", "Fighting", "Pokémon"),
        PokemonCard("base1-49", "Doduo", "49", "base1", "Common", "Colorless", "Pokémon"),
        PokemonCard("base1-50", "Drowzee", "50", "base1", "Common", "Psychic", "Pokémon"),
        PokemonCard("base1-51", "Gastly", "51", "base1", "Common", "Psychic", "Pokémon"),
        PokemonCard("base1-52", "Koffing", "52", "base1", "Common", "Psychic", "Pokémon"),
        PokemonCard("base1-53", "Machop", "53", "base1", "Common", "Fighting", "Pokémon"),
        PokemonCard("base1-54", "Magikarp", "54", "base1", "Common", "Water", "Pokémon"),
        PokemonCard("base1-55", "Onix", "55", "base1", "Common", "Fighting", "Pokémon"),
        PokemonCard("base1-56", "Pidgey", "56", "base1", "Common", "Colorless", "Pokémon"),
        PokemonCard("base1-57", "Pikachu", "57", "base1", "Common", "Lightning", "Pokémon"),
        PokemonCard("base1-58", "Poliwag", "58", "base1", "Common", "Water", "Pokémon"),
        PokemonCard("base1-59", "Ponyta", "59", "base1", "Common", "Fire", "Pokémon"),
        PokemonCard("base1-60", "Rattata", "60", "base1", "Common", "Colorless", "Pokémon"),
        PokemonCard("base1-61", "Squirtle", "61", "base1", "Common", "Water", "Pokémon"),
        PokemonCard("base1-62", "Starmie", "62", "base1", "Common", "Water", "Pokémon"),
        PokemonCard("base1-63", "Staryu", "63", "base1", "Common", "Water", "Pokémon"),
        PokemonCard("base1-64", "Tangela", "64", "base1", "Common", "Grass", "Pokémon"),
        PokemonCard("base1-65", "Voltorb", "65", "base1", "Common", "Lightning", "Pokémon"),
        PokemonCard("base1-66", "Weedle", "66", "base1", "Common", "Grass", "Pokémon"),
        // Trainers
        PokemonCard("base1-77", "Computer Search", "77", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-78", "Devolution Spray", "78", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-79", "Impostor Professor Oak", "79", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-80", "Item Finder", "80", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-81", "Lass", "81", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-82", "Pokémon Breeder", "82", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-83", "Pokémon Trader", "83", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-84", "Scoop Up", "84", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-85", "Super Energy Removal", "85", "base1", "Rare", "", "Trainer"),
        PokemonCard("base1-86", "Defender", "86", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-87", "Energy Retrieval", "87", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-88", "Full Heal", "88", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-89", "Maintenance", "89", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-90", "PlusPower", "90", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-91", "Pokémon Center", "91", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-92", "Pokémon Flute", "92", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-93", "Pokedex", "93", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-94", "Professor Oak", "94", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-95", "Revive", "95", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-96", "Super Potion", "96", "base1", "Uncommon", "", "Trainer"),
        PokemonCard("base1-97", "Bill", "97", "base1", "Common", "", "Trainer"),
        PokemonCard("base1-98", "Energy Removal", "98", "base1", "Common", "", "Trainer"),
        PokemonCard("base1-99", "Gust of Wind", "99", "base1", "Common", "", "Trainer"),
        PokemonCard("base1-100", "Potion", "100", "base1", "Common", "", "Trainer"),
        PokemonCard("base1-101", "Switch", "101", "base1", "Common", "", "Trainer"),
        // Energy
        PokemonCard("base1-102", "Double Colorless Energy", "102", "base1", "Uncommon", "Colorless", "Energy")
    )

    private fun generateSwSh1Cards(): List<PokemonCard> {
        val cards = mutableListOf<PokemonCard>()
        val names = listOf(
            "Caterpie", "Metapod", "Butterfree V", "Celebi V", "Celebi VMAX",
            "Grookey", "Thwackey", "Rillaboom", "Rillaboom V", "Rillaboom VMAX",
            "Chewtle", "Drednaw", "Drednaw V", "Drednaw VMAX", "Snom", "Frosmoth",
            "Crobat V", "Grimmsnarl V", "Grimmsnarl VMAX", "Scorbunny", "Raboot", "Cinderace",
            "Cinderace V", "Cinderace VMAX", "Arcanine", "Torkoal V", "Coalossal V", "Coalossal VMAX",
            "Sobble", "Drizzile", "Inteleon", "Inteleon V", "Inteleon VMAX", "Lapras V", "Lapras VMAX",
            "Pikachu V", "Morpeko V", "Boltund V",
            "Hatterene V", "Hatterene VMAX", "Indeedee V",
            "Stonjourner V", "Coalossal V",
            "Eternatus V", "Eternatus VMAX",
            "Zacian V", "Zamazenta V", "Falinks V",
            "Professor's Research", "Marnie", "Boss's Orders", "Sonia",
            "Training Court", "Rose Tower", "Galar Mine",
            "Air Balloon", "Energy Switch", "Evolution Incense",
            "Fairy Energy", "Metal Energy", "Darkness Energy"
        )
        names.forEachIndexed { index, name ->
            val num = index + 1
            val supertype = when {
                name.contains("Energy") -> "Energy"
                name.contains("Tower") || name.contains("Mine") || name.contains("Court") -> "Trainer"
                name.contains("Ball") || name.contains("Research") || name.contains("Marnie") || name.contains("Boss") || name.contains("Sonia") || name.contains("Switch") || name.contains("Incense") || name.contains("Balloon") -> "Trainer"
                else -> "Pokémon"
            }
            val rarity = when {
                name.contains("VMAX") -> "Rare VMAX"
                name.contains(" V") || name.endsWith("V") -> "Rare Ultra"
                num <= 17 -> "Rare Holo"
                num <= 40 -> "Uncommon"
                else -> "Common"
            }
            val type = when {
                name.contains("Grass") || name.contains("Grookey") || name.contains("Rillaboom") || name.contains("Caterpie") || name.contains("Celebi") -> "Grass"
                name.contains("Fire") || name.contains("Scorbunny") || name.contains("Cinderace") || name.contains("Arcanine") || name.contains("Torkoal") || name.contains("Coalossal") -> "Fire"
                name.contains("Water") || name.contains("Sobble") || name.contains("Inteleon") || name.contains("Lapras") || name.contains("Drednaw") || name.contains("Chewtle") -> "Water"
                name.contains("Lightning") || name.contains("Pikachu") || name.contains("Boltund") || name.contains("Morpeko") -> "Lightning"
                name.contains("Psychic") || name.contains("Hatterene") || name.contains("Indeedee") -> "Psychic"
                name.contains("Darkness") || name.contains("Grimmsnarl") || name.contains("Crobat") || name.contains("Eternatus") -> "Darkness"
                name.contains("Metal") || name.contains("Zacian") || name.contains("Zamazenta") || name.contains("Falinks") || name.contains("Stonjourner") -> "Metal"
                else -> ""
            }
            cards.add(PokemonCard("swsh1-$num", name, num.toString(), "swsh1", rarity, type, supertype))
        }
        return cards
    }
}
