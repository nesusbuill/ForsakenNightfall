package com.studios.forsaken2d

data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float)
data class GameMap(
    val name: String, val bgColor: Int, val width: Float, val height: Float,
    val obstacles: List<Obstacle>, val genPositions: List<Pair<Float, Float>>,
    val playerSpawn: Pair<Float, Float>, val killerSpawn: Pair<Float, Float>
)

object MapDB {
    private val glassHouses = GameMap("Glass Houses", 0xFF1A2A3A.toInt(), 2000f, 1200f,
        listOf(Obstacle(400f, 300f, 500f, 40f), Obstacle(400f, 300f, 40f, 400f),
               Obstacle(1200f, 700f, 400f, 40f)),
        listOf(500f to 200f, 1500f to 800f, 800f to 900f, 300f to 800f, 1600f to 300f),
        1000f to 600f, 1800f to 1100f)
    private val undergroundWar = GameMap("Underground War", 0xFF2A1A0A.toInt(), 2000f, 1200f,
        listOf(Obstacle(200f, 200f, 400f, 40f), Obstacle(700f, 400f, 40f, 500f),
               Obstacle(1000f, 300f, 400f, 40f), Obstacle(500f, 800f, 40f, 300f)),
        listOf(400f to 400f, 1200f to 200f, 800f to 700f, 1600f to 900f, 300f to 950f),
        1000f to 600f, 1800f to 1100f)
    private val horrorHotel = GameMap("Horror Hotel", 0xFF1A0A0A.toInt(), 2000f, 1200f,
        listOf(Obstacle(100f, 100f, 1800f, 30f), Obstacle(100f, 1070f, 1800f, 30f),
               Obstacle(100f, 100f, 30f, 1000f), Obstacle(1870f, 100f, 30f, 1000f),
               Obstacle(600f, 200f, 30f, 500f), Obstacle(1200f, 500f, 30f, 600f)),
        listOf(300f to 300f, 1500f to 900f, 900f to 400f, 1700f to 300f, 400f to 900f),
        1000f to 600f, 1800f to 1100f)
    val all = listOf(glassHouses, undergroundWar, horrorHotel)
    fun random(): GameMap = all.random()
}
