package com.studios.forsaken2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.hypot

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    @Volatile private var isPlaying = false
    @Volatile private var isPaused = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val map = MapDB.random()
    private val match = MatchManager()
    private val generators = mutableListOf<Generator>()

    private val playerChar = CharacterDB.survivorById("noob")
    private val playerSkin = playerChar.skins[0]
    private var px = map.playerSpawn.first
    private var py = map.playerSpawn.second
    private var playerHp = playerChar.hp
    private var playerStamina = 100f
    private var isExhausted = false
    private var exhaustTimer = 0f

    private val killerChar = CharacterDB.killerById("slasher")
    private val killerSkin = killerChar.skins[0]
    private var kx = map.killerSpawn.first
    private var ky = map.killerSpawn.second
    private var killerStunTimer = 0f

    private val playerSkills = mutableListOf<Skill>()
    private val joystick = Joystick(250f, 800f)
    private var sprintPointer = -1
    private var joystickPointer = -1
    private var skillPointers = mutableMapOf<Int, Int>()
    private var activeGen: Generator? = null
    private var genPointer = -1

    private val otherPlayers = mutableMapOf<String, Pair<Float, Float>>()
    private var cameraX = 0f
    private var cameraY = 0f

    init {
        holder.addCallback(this)
        try {
            SoundManager.init(context)
        } catch (e: Exception) {
            Log.e("FORSAKEN_VIEW", "SoundManager init failed: ${e.message}")
        }
        map.genPositions.forEachIndexed { i, p ->
            generators.add(Generator(i, p.first, p.second))
        }
        playerChar.abilities.forEach { ab ->
            playerSkills.add(Skill(ab.name, ab.cooldown, 0xFF00E5FF.toInt()))
        }
        NetworkManager.onPosReceived = { name, x, y -> otherPlayers[name] = x to y }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("FORSAKEN_VIEW", "surfaceCreated")
        isPlaying = true
        isPaused = false
        if (gameThread == null || !gameThread!!.isAlive) {
            gameThread = Thread(this, "GameThread").also { it.start() }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d("FORSAKEN_VIEW", "surfaceChanged: ${width}x${height}")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d("FORSAKEN_VIEW", "surfaceDestroyed")
        isPlaying = false
        try { gameThread?.join(1000) } catch (_: Exception) {}
        gameThread = null
        try { SoundManager.release() } catch (_: Exception) {}
    }

    fun resume() {
        isPaused = false
    }

    fun pause() {
        isPaused = true
    }

    fun stop() {
        isPlaying = false
        try { gameThread?.join(2000) } catch (_: Exception) {}
        gameThread = null
    }

    override fun run() {
        try {
            var last = System.nanoTime()
            while (isPlaying) {
                if (isPaused) {
                    Thread.sleep(30)
                    last = System.nanoTime()
                    continue
                }
                val now = System.nanoTime()
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now

                try {
                    update(dt)
                    val c = holder.lockCanvas() ?: continue
                    try {
                        drawGame(c)
                    } finally {
                        holder.unlockCanvasAndPost(c)
                    }
                } catch (e: Exception) {
                    Log.e("FORSAKEN_VIEW", "Frame error: ${e.message}", e)
                }

                try { Thread.sleep(16) } catch (_: InterruptedException) {}
            }
        } catch (e: Exception) {
            Log.e("FORSAKEN_VIEW", "Game loop crashed", e)
        }
    }

    private fun update(dt: Float) {
        if (match.isGameOver) return
        playerSkills.forEach { it.update(dt) }

        val (jx, jy) = joystick.getInput()
        val isMoving = jx != 0f || jy != 0f
        val isSprinting = sprintPointer != -1 && isMoving && !isExhausted && playerStamina > 0
        val speed = if (isSprinting) playerChar.sprintSpeed else playerChar.walkSpeed

        if (isSprinting) {
            playerStamina -= 25f * dt
            if (playerStamina <= 0) {
                playerStamina = 0f
                isExhausted = true
                exhaustTimer = 2f
                sprintPointer = -1
            }
        } else {
            if (playerStamina < 100f) playerStamina = (playerStamina + 15f * dt).coerceAtMost(100f)
            if (isExhausted) { exhaustTimer -= dt; if (exhaustTimer <= 0f) isExhausted = false }
        }

        if (isMoving) {
            val mag = hypot(jx, jy).coerceAtLeast(0.001f)
            px += (jx / mag) * speed * dt
            py += (jy / mag) * speed * dt
        }
        px = px.coerceIn(playerChar.radius, map.width - playerChar.radius)
        py = py.coerceIn(playerChar.radius, map.height - playerChar.radius)
        handleObstacleCollision()

        if (killerStunTimer > 0) killerStunTimer -= dt
        else {
            val dx = px - kx
            val dy = py - ky
            val dist = hypot(dx, dy)
            if (dist > 0) {
                kx += (dx / dist) * killerChar.walkSpeed * dt
                ky += (dy / dist) * killerChar.walkSpeed * dt
            }
            kx = kx.coerceIn(killerChar.radius, map.width - killerChar.radius)
            ky = ky.coerceIn(killerChar.radius, map.height - killerChar.radius)
            if (dist < killerChar.radius + playerChar.radius + 20f && playerHp > 0) {
                playerHp = (playerHp - 15f * dt).coerceAtLeast(0f)
                if (playerHp <= 0f) match.onSurvivorKilled()
            }
        }

        activeGen?.let { gen ->
            if (!gen.isCompleted) {
                gen.repair(dt, match)
                gen.knobX = (gen.knobX - 150f * dt).coerceAtLeast(0f)
            }
        }

        val event = match.update(dt, if (playerHp > 0f) 1 else 0,
            killerChar.id, killerSkin.name, playerChar.id, playerSkin.name)
        if (event == "LMS_TRIGGERED") {
            val cfg = LMSManager.getConfig(killerChar.id, killerSkin.name, playerChar.id, playerSkin.name)
            if (cfg.musicResId != 0) SoundManager.playBGM(context, cfg.musicResId)
        }

        if (NetworkManager.isConnected) NetworkManager.sendPos(px, py)
        cameraX = (px - width / 2f).coerceIn(0f, (map.width - width).coerceAtLeast(0f))
        cameraY = (py - height / 2f).coerceIn(0f, (map.height - height).coerceAtLeast(0f))
    }

    private fun handleObstacleCollision() {
        for (obs in map.obstacles) {
            val cx = px.coerceIn(obs.x, obs.x + obs.w)
            val cy = py.coerceIn(obs.y, obs.y + obs.h)
            val ddx = px - cx
            val ddy = py - cy
            val d = hypot(ddx, ddy)
            if (d < playerChar.radius && d > 0) {
                val push = playerChar.radius - d
                px += (ddx / d) * push
                py += (ddy / d) * push
            }
        }
    }

    private fun drawGame(canvas: Canvas) {
        canvas.drawColor(map.bgColor)
        canvas.save()
        canvas.translate(-cameraX, -cameraY)

        paint.color = Color.parseColor("#4A4A4A")
        for (obs in map.obstacles) {
            canvas.drawRect(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h, paint)
        }
        for (gen in generators) drawGenerator(canvas, gen)

        for ((name, pos) in otherPlayers) {
            paint.color = Color.MAGENTA
            canvas.drawCircle(pos.first, pos.second, 40f, paint)
            paint.color = Color.WHITE
            paint.textSize = 30f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(name, pos.first, pos.second - 55f, paint)
        }

        paint.color = Color.parseColor(killerSkin.accentHex)
        canvas.drawCircle(kx, ky, killerChar.radius, paint)
        paint.color = Color.parseColor(killerSkin.colorHex)
        canvas.drawCircle(kx, ky, killerChar.radius * 0.7f, paint)

        paint.color = Color.parseColor(playerSkin.accentHex)
        canvas.drawCircle(px, py, playerChar.radius + 8f, paint)
        paint.color = Color.parseColor(playerSkin.colorHex)
        canvas.drawCircle(px, py, playerChar.radius, paint)

        canvas.restore()
        paint.textAlign = Paint.Align.LEFT
        drawHUD(canvas)
    }

    private fun drawGenerator(canvas: Canvas, gen: Generator) {
        paint.color = if (gen.isCompleted) Color.GREEN else Color.YELLOW
        canvas.drawCircle(gen.x, gen.y, gen.radius, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 3f
        canvas.drawCircle(gen.x, gen.y, gen.radius, paint)
        paint.style = Paint.Style.FILL

        if (gen.progress > 0 && !gen.isCompleted) {
            paint.style = Paint.Style.STROKE
            paint.color = Color.CYAN
            paint.strokeWidth = 6f
            val r = RectF(gen.x - gen.radius - 10f, gen.y - gen.radius - 10f,
                gen.x + gen.radius + 10f, gen.y + gen.radius + 10f)
            canvas.drawArc(r, -90f, 360f * (gen.progress / 100f), false, paint)
            paint.style = Paint.Style.FILL
        }

        if (gen.isBeingRepaired && !gen.isCompleted) {
            val ts = gen.x - gen.TRACK_WIDTH / 2f
            val ty = gen.y - 80f
            paint.color = Color.DKGRAY
            canvas.drawRect(ts, ty - 12f, ts + gen.TRACK_WIDTH, ty + 12f, paint)
            paint.color = Color.parseColor("#00E5FF")
            canvas.drawCircle(ts + gen.knobX, ty, 25f, paint)
        }
    }

    private fun drawHUD(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 70f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = when {
            match.isLMS -> Color.parseColor("#FF00E5")
            match.timeLeft < 30f -> Color.RED
            else -> Color.WHITE
        }
        canvas.drawText(match.formattedTime(), w / 2f, 90f, paint)

        paint.textSize = 28f
        paint.color = Color.MAGENTA
        canvas.drawText("Malice: ${match.malice}  |  Gens: ${match.generatorsDone}/${match.totalGenerators}",
            w / 2f, 130f, paint)

        if (match.isLMS) {
            paint.color = Color.parseColor("#FF00E5")
            paint.textSize = 26f
            canvas.drawText("♪ ${match.currentLMSTheme} ♪", w / 2f, 175f, paint)
            paint.color = Color.WHITE
            paint.textSize = 18f
            canvas.drawText("LAST MAN STANDING", w / 2f, 205f, paint)
        }

        drawBar(canvas, 40f, 40f, 300f, 30f, playerHp / playerChar.hp, Color.RED, "HP")
        drawBar(canvas, 40f, 90f, 300f, 20f, playerStamina / 100f,
            if (isExhausted) Color.GRAY else Color.CYAN,
            if (isExhausted) "EXHAUSTED" else "STAMINA")

        paint.color = Color.argb(100, 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawCircle(joystick.centerX, joystick.centerY, joystick.baseRadius, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(200, 200, 200, 200)
        canvas.drawCircle(joystick.knobX, joystick.knobY, 50f, paint)

        val sprintX = 250f
        val sprintY = joystick.centerY - 200f
        paint.color = if (sprintPointer != -1 && playerStamina > 0)
            Color.parseColor("#00E5FF") else Color.argb(120, 255, 255, 255)
        canvas.drawCircle(sprintX, sprintY, 75f, paint)
        paint.color = Color.BLACK
        paint.textSize = 40f
        canvas.drawText(">>", sprintX, sprintY + 14f, paint)

        val skillStartX = w - 150f
        val skillStartY = h - 150f
        for ((i, skill) in playerSkills.withIndex()) {
            drawSkillButton(canvas, skill, skillStartX - i * 180f, skillStartY)
        }

        if (match.isGameOver) {
            paint.color = Color.argb(200, 0, 0, 0)
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.color = Color.GREEN
            paint.textSize = 100f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("SURVIVOR WIN", w / 2f, h / 2f, paint)
            paint.color = Color.WHITE
            paint.textSize = 40f
            canvas.drawText("Tap to restart", w / 2f, h / 2f + 100f, paint)
        }
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawBar(c: Canvas, x: Float, y: Float, w: Float, h: Float,
                        ratio: Float, color: Int, label: String) {
        paint.color = Color.argb(150, 0, 0, 0)
        c.drawRect(x, y, x + w, y + h, paint)
        paint.color = color
        c.drawRect(x, y, x + w * ratio.coerceIn(0f, 1f), y + h, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        c.drawRect(x, y, x + w, y + h, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 16f
        c.drawText(label, x + 8f, y + h - 6f, paint)
    }

    private fun drawSkillButton(c: Canvas, skill: Skill, cx: Float, cy: Float) {
        val r = 60f
        paint.color = Color.argb(180, 50, 50, 50)
        c.drawCircle(cx, cy, r, paint)

        if (skill.cooldown > 0) {
            val sweep = 360f * (skill.cooldown / skill.maxCooldown)
            paint.color = Color.argb(200, 0, 0, 0)
            c.drawArc(RectF(cx - r, cy - r, cx + r, cy + r), -90f, sweep, true, paint)
            paint.color = Color.WHITE
            paint.textSize = 30f
            paint.textAlign = Paint.Align.CENTER
            c.drawText(String.format("%.1f", skill.cooldown), cx, cy + 10f, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.color = skill.color
            paint.strokeWidth = 5f
            c.drawCircle(cx, cy, r, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.textAlign = Paint.Align.CENTER
            c.drawText(skill.name.take(6), cx, cy + 8f, paint)
        }
        paint.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (match.isGameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) (context as? GameActivity)?.recreate()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                handlePointerDown(event.getX(idx), event.getY(idx), event.getPointerId(idx))
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    if (id == joystickPointer) {
                        val dx = x - joystick.centerX
                        val dy = y - joystick.centerY
                        val dist = hypot(dx, dy)
                        if (dist > joystick.baseRadius) {
                            joystick.knobX = joystick.centerX + (dx / dist) * joystick.baseRadius
                            joystick.knobY = joystick.centerY + (dy / dist) * joystick.baseRadius
                        } else {
                            joystick.knobX = x
                            joystick.knobY = y
                        }
                    }
                    if (id == genPointer && activeGen != null) {
                        val gen = activeGen!!
                        val worldX = x + cameraX
                        gen.knobX = (worldX - (gen.x - gen.TRACK_WIDTH / 2f))
                            .coerceIn(0f, gen.TRACK_WIDTH)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == joystickPointer) { joystickPointer = -1; joystick.reset() }
                if (id == sprintPointer) sprintPointer = -1
                if (id == genPointer) { activeGen?.resetDrag(); activeGen = null; genPointer = -1 }
                skillPointers.remove(id)
            }
        }
        return true
    }

    private fun handlePointerDown(x: Float, y: Float, id: Int) {
        val worldX = x + cameraX
        val worldY = y + cameraY
        for (gen in generators) {
            if (!gen.isCompleted && hypot(worldX - gen.x, worldY - gen.y) < gen.radius + 80f) {
                activeGen = gen
                gen.isBeingRepaired = true
                genPointer = id
                return
            }
        }

        val w = width.toFloat()
        val h = height.toFloat()
        val skillStartX = w - 150f
        val skillStartY = h - 150f
        for ((i, _) in playerSkills.withIndex()) {
            if (hypot(x - (skillStartX - i * 180f), y - skillStartY) < 80f) {
                if (playerSkills[i].trigger()) applySkillEffect(i)
                skillPointers[id] = i
                return
            }
        }

        if (hypot(x - 250f, y - (joystick.centerY - 200f)) < 90f) {
            sprintPointer = id
            return
        }

        if (x < w / 2f) {
            joystickPointer = id
            joystick.centerX = x
            joystick.centerY = y
            joystick.knobX = x
            joystick.knobY = y
            joystick.isActive = true
        }
    }

    private fun applySkillEffect(index: Int) {
        val abilityName = playerChar.abilities.getOrNull(index)?.name ?: return
        when (abilityName) {
            "Hatchet", "Slash", "Punch", "One Shot" -> {
                if (hypot(px - kx, py - ky) < 150f) killerStunTimer = 3f
            }
            "Sacrificial Dagger" -> {
                if (hypot(px - kx, py - ky) < 100f) killerStunTimer = 3.5f
            }
        }
    }
}
