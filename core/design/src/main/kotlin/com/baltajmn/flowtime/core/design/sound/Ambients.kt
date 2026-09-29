package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.design.service.PlayerType

/**
 * Un sonido ambiental generado en tiempo real. No hay ficheros de audio: nada que descargar ni que
 * licenciar, funciona sin conexion y suena sin fin y sin cortes.
 */
internal interface Ambient {
    /** Escribe [frames] muestras en [left] y [right], sin volumen: lo aplica el mezclador. */
    fun render(left: FloatArray, right: FloatArray, frames: Int)
}

internal fun ambientFor(type: PlayerType): Ambient = when (type) {
    PlayerType.RAIN -> Rain(dropsPerSecond = 320f)
    PlayerType.FIRE -> Fire()
    PlayerType.WAVE -> Waves()
    PlayerType.THUNDER -> Storm()
    PlayerType.BIRDS -> Birds()
    PlayerType.HEAT -> Heat()
    PlayerType.COFFEE_HOUSE -> CoffeeHouse()
    PlayerType.MEDITATION -> Meditation()
    PlayerType.WIND -> Wind()
    PlayerType.BROWN -> BrownNoise()
    PlayerType.PINK -> PinkNoise()
    PlayerType.WHITE -> WhiteNoise()
}

private class WhiteNoise : Ambient {
    private val l = Rng(1)
    private val r = Rng(2)

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            left[i] = l.white() * 0.12f
            right[i] = r.white() * 0.12f
        }
    }
}

private class PinkNoise : Ambient {
    private val l = Pink(Rng(3))
    private val r = Pink(Rng(4))

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            left[i] = l.next() * 0.45f
            right[i] = r.next() * 0.45f
        }
    }
}

private class BrownNoise : Ambient {
    private val l = Brown(Rng(5))
    private val r = Brown(Rng(6))

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            left[i] = l.next() * 0.45f
            right[i] = r.next() * 0.45f
        }
    }
}

/** Un fondo de ruido suave, muchos golpecitos de gotas y alguna que cae en un charco. */
private class Rain(private val dropsPerSecond: Float) : Ambient {
    private val rng = Rng(11)
    private val bedLeft = Pink(Rng(12))
    private val bedRight = Pink(Rng(13))
    private val bedHighLeft = HighPass(350f)
    private val bedHighRight = HighPass(350f)
    private val bedLowLeft = LowPass(4500f)
    private val bedLowRight = LowPass(4500f)
    private val tapHighLeft = HighPass(1400f)
    private val tapHighRight = HighPass(1400f)
    private val tapLowLeft = LowPass(8000f)
    private val tapLowRight = LowPass(8000f)
    private val intensity = Drift(rng, 0.7f, 1f, 2f, 7f)
    private val taps = Grains(128)
    private val plinks = Grains(24)
    private var untilTap = 1
    private var untilPlink = 1

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val k = intensity.next()
            if (--untilTap <= 0) {
                untilTap = rng.wait(dropsPerSecond * k)
                val size = rng.unit()
                taps.add(
                    amplitude = 0.03f + 0.45f * size * size * size * size,
                    seconds = rng.range(0.0004f, 0.002f),
                    pan = rng.unit()
                )
            }
            if (--untilPlink <= 0) {
                untilPlink = rng.wait(dropsPerSecond * 0.03f)
                // Una gota en el agua suena como una burbuja: un tono corto que sube.
                plinks.add(
                    amplitude = rng.range(0.015f, 0.06f),
                    seconds = rng.range(0.006f, 0.016f),
                    pan = rng.unit(),
                    hz = rng.range(1300f, 3600f),
                    glide = 1.25f
                )
            }
            taps.next(rng)
            plinks.next(rng)
            val bedL = bedLowLeft.process(bedHighLeft.process(bedLeft.next())) * k
            val bedR = bedLowRight.process(bedHighRight.process(bedRight.next())) * k
            left[i] = (bedL * 0.55f + tapLowLeft.process(tapHighLeft.process(taps.left)) + plinks.left) * 1.4f
            right[i] = (bedR * 0.55f + tapLowRight.process(tapHighRight.process(taps.right)) + plinks.right) * 1.4f
        }
    }
}

/** Lluvia fuerte y, cada poco, un trueno que retumba y se aleja. */
private class Storm : Ambient {
    private val rain = Rain(dropsPerSecond = 900f)
    private val rng = Rng(21)
    private val rumbleLeft = Brown(Rng(22))
    private val rumbleRight = Brown(Rng(23))
    private val rumbleLowLeft = LowPass(400f)
    private val rumbleLowRight = LowPass(400f)
    private val crack = Pink(Rng(24))
    private val crackLow = LowPass(2500f)
    private val roll = Drift(rng, 0.3f, 1f, 0.1f, 0.45f)
    private var untilThunder = rng.waitSeconds(3f, 10f)
    private var t = 0
    private var length = 0
    private var attack = 1
    private var loudness = 0f
    private var close = false
    private var pan = 0.5f

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        rain.render(left, right, frames)
        for (i in 0 until frames) {
            if (length == 0) {
                if (--untilThunder <= 0) strike()
                continue
            }
            val x = t.toFloat() / length
            val env = if (t < attack) t.toFloat() / attack else (1f - x) * (1f - x)
            if ((t and 63) == 0) {
                val cutoff = 110f + 450f * (1f - x)
                rumbleLowLeft.cutoff(cutoff)
                rumbleLowRight.cutoff(cutoff)
            }
            val body = env * roll.next() * loudness
            var l = rumbleLowLeft.process(rumbleLeft.next()) * body * 2.2f
            var r = rumbleLowRight.process(rumbleRight.next()) * body * 2.2f
            if (close && t < SAMPLE_RATE / 2) {
                // El chasquido del rayo cuando cae cerca.
                val c = crackLow.process(crack.next()) * (1f - t * 2f / SAMPLE_RATE) * loudness * 0.9f
                l += c * panLeft(pan)
                r += c * panRight(pan)
            }
            left[i] += l
            right[i] += r
            if (++t >= length) {
                length = 0
                untilThunder = rng.waitSeconds(15f, 45f)
            }
        }
    }

    private fun strike() {
        close = rng.unit() < 0.3f
        t = 0
        length = rng.waitSeconds(4f, 9f)
        attack = if (close) rng.waitSeconds(0.02f, 0.08f) else rng.waitSeconds(0.2f, 0.6f)
        loudness = if (close) rng.range(0.8f, 1f) else rng.range(0.4f, 0.8f)
        pan = rng.unit()
    }
}

/** Tres olas desfasadas: suben, rompen y se retiran arrastrando arena. */
private class Waves : Ambient {
    private val a = Swell(31)
    private val b = Swell(41)
    private val c = Swell(51)

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val x = a.next()
            val y = b.next()
            val z = c.next()
            left[i] = (x * 0.8f + z * 0.55f + y * 0.25f) * 0.9f
            right[i] = (y * 0.8f + z * 0.55f + x * 0.25f) * 0.9f
        }
    }

    private class Swell(seed: Int) {
        private val rng = Rng(seed)
        private val pink = Pink(Rng(seed + 1))
        private val brown = Brown(Rng(seed + 2))
        private val sand = Rng(seed + 3)
        private val low = LowPass(300f)
        private val sandHigh = HighPass(3000f)
        private var pos = rng.unit()
        private var step = 0f
        private var peak = 1f
        private var counter = 0

        init {
            nextWave()
        }

        private fun nextWave() {
            step = 1f / (rng.range(7f, 12f) * SAMPLE_RATE)
            peak = rng.range(0.55f, 1f)
        }

        fun next(): Float {
            pos += step
            if (pos >= 1f) {
                pos -= 1f
                nextWave()
            }
            val env = shape(pos) * peak
            if ((counter++ and 63) == 0) low.cutoff(180f + 2600f * env * env)
            val body = low.process(pink.next() * 0.6f + brown.next() * 0.4f) * (0.12f + env)
            val wash = if (pos > 0.3f) sandHigh.process(sand.white()) * env * 0.05f else 0f
            return body + wash
        }

        /** Sube durante un tercio del ciclo, rompe y se retira despacio. */
        private fun shape(p: Float): Float = if (p < 0.3f) {
            val t = p / 0.3f
            t * t * (3f - 2f * t)
        } else {
            val t = (p - 0.3f) / 0.7f
            (1f - t) * (1f - t)
        }
    }
}

/** Rafagas que cambian de fuerza y de tono, con algun silbido. */
private class Wind : Ambient {
    private val rng = Rng(61)
    private val noiseLeft = Pink(Rng(62))
    private val noiseRight = Pink(Rng(63))
    private val bandLeft = BandPass(400f, 0.9f)
    private val bandRight = BandPass(420f, 0.9f)
    private val howl = BandPass(900f, 10f)
    private val gust = Drift(rng, 0.2f, 1f, 1.2f, 4f)
    private val tone = Drift(rng, 250f, 700f, 1.5f, 5f)
    private val whistle = Drift(rng, 700f, 1400f, 2f, 6f)
    private var counter = 0

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val g = gust.next()
            val f = tone.next()
            val w = whistle.next()
            if ((counter++ and 31) == 0) {
                bandLeft.tune(f * (0.8f + 0.4f * g), 0.9f)
                bandRight.tune(f * (0.85f + 0.4f * g), 0.9f)
                howl.tune(w * (0.9f + 0.2f * g), 10f)
            }
            val nl = noiseLeft.next()
            val nr = noiseRight.next()
            val h = howl.process(nl + nr) * g * g * g * 0.9f
            left[i] = bandLeft.process(nl) * g * 1.4f + h
            right[i] = bandRight.process(nr) * g * 1.4f + h
        }
    }
}

/** Las llamas de fondo, un siseo y chasquidos de la madera que saltan en grupos. */
private class Fire : Ambient {
    private val rng = Rng(71)
    private val roarLeft = Brown(Rng(72))
    private val roarRight = Brown(Rng(73))
    private val roarLowLeft = LowPass(300f)
    private val roarLowRight = LowPass(300f)
    private val hissLeft = Rng(74)
    private val hissRight = Rng(75)
    private val hissHighLeft = HighPass(2500f)
    private val hissHighRight = HighPass(2500f)
    private val crackleHighLeft = HighPass(1200f)
    private val crackleHighRight = HighPass(1200f)
    private val flicker = Drift(rng, 0.45f, 1f, 0.06f, 0.35f)
    private val flare = Drift(rng, 0.6f, 1f, 1f, 4f)
    private val crackles = Grains(64)
    private var untilBurst = 1
    private var burstLeft = 0
    private var untilPop = 0
    private var burstPan = 0.5f

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val f = flicker.next() * flare.next()
            if (--untilBurst <= 0) {
                untilBurst = rng.wait(5f)
                burstLeft = 1 + (rng.unit() * 6).toInt()
                burstPan = rng.unit()
                untilPop = 0
            }
            if (burstLeft > 0 && --untilPop <= 0) {
                burstLeft--
                untilPop = rng.waitSeconds(0.004f, 0.05f)
                val u = rng.unit()
                crackles.add(
                    amplitude = 0.08f + 0.7f * u * u * u,
                    seconds = rng.range(0.0003f, 0.0025f),
                    pan = (burstPan + rng.range(-0.1f, 0.1f)).coerceIn(0f, 1f)
                )
            }
            crackles.next(rng)
            left[i] = roarLowLeft.process(roarLeft.next()) * f * 0.9f +
                hissHighLeft.process(hissLeft.white()) * f * 0.03f +
                crackleHighLeft.process(crackles.left)
            right[i] = roarLowRight.process(roarRight.next()) * f * 0.9f +
                hissHighRight.process(hissRight.white()) * f * 0.03f +
                crackleHighRight.process(crackles.right)
        }
    }
}

/** Cuatro pajaros con su tono y su sitio, que cantan frases cortas sobre un rumor de hojas. */
private class Birds : Ambient {
    private val rng = Rng(81)
    private val leavesLeft = Pink(Rng(82))
    private val leavesRight = Pink(Rng(83))
    private val leavesLowLeft = LowPass(1800f)
    private val leavesLowRight = LowPass(1800f)
    private val breeze = Drift(rng, 0.3f, 1f, 2f, 6f)
    private val birds = Array(4) { Bird(Rng(90 + it)) }

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val b = breeze.next() * 0.06f
            var l = leavesLowLeft.process(leavesLeft.next()) * b
            var r = leavesLowRight.process(leavesRight.next()) * b
            for (bird in birds) {
                val s = bird.next()
                l += s * bird.left
                r += s * bird.right
            }
            left[i] = l
            right[i] = r
        }
    }

    private class Bird(private val rng: Rng) {
        private val base = rng.range(2200f, 5200f)
        private val pan = rng.unit()
        val left = panLeft(pan)
        val right = panRight(pan)
        private val loudness = rng.range(0.06f, 0.16f)
        private var wait = rng.waitSeconds(0.5f, 6f)
        private var syllables = 0
        private var shape = 0
        private var pitch = 1f
        private var t = 0
        private var length = 0
        private var phase = 0f

        fun next(): Float {
            if (length == 0) {
                if (--wait > 0) return 0f
                if (syllables == 0) {
                    syllables = 2 + (rng.unit() * 6).toInt()
                    shape = (rng.unit() * 4).toInt()
                    pitch = rng.range(0.85f, 1.15f)
                }
                syllables--
                t = 0
                length = rng.waitSeconds(0.04f, 0.14f)
                wait = if (syllables == 0) rng.waitSeconds(1.5f, 8f) else rng.waitSeconds(0.03f, 0.09f)
            }
            val x = t.toFloat() / length
            val ratio = when (shape) {
                0 -> 0.8f + 0.5f * x
                1 -> 1.3f - 0.5f * x
                2 -> 1f + 0.12f * Sine.at(t * 32f / SAMPLE_RATE)
                else -> 1.2f - 0.9f * x + 0.9f * x * x
            }
            phase += base * pitch * ratio / SAMPLE_RATE
            if (phase >= 1f) phase -= 1f
            val env = Sine.at(x * 0.5f)
            if (++t >= length) length = 0
            return Sine.at(phase) * env * env * loudness
        }
    }
}

/** Una tarde de calor: el zumbido de las chicharras, que va y viene, y una brisa floja. */
private class Heat : Ambient {
    private val rng = Rng(101)
    private val buzzLeft = Rng(102)
    private val buzzRight = Rng(103)
    private val bandLeft = BandPass(4600f, 5f)
    private val bandRight = BandPass(5100f, 5f)
    private val swellLeft = Drift(rng, 0.1f, 1f, 3f, 9f)
    private val swellRight = Drift(rng, 0.1f, 1f, 3f, 9f)
    private val air = Pink(Rng(104))
    private val airLow = LowPass(700f)
    private var pulseLeft = 0f
    private var pulseRight = 0.37f

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            pulseLeft += 190f / SAMPLE_RATE
            pulseRight += 213f / SAMPLE_RATE
            if (pulseLeft >= 1f) pulseLeft -= 1f
            if (pulseRight >= 1f) pulseRight -= 1f
            // Pulsos cortos y seguidos: el tic del abdomen de la chicharra.
            val sl = Sine.at(pulseLeft)
            val sr = Sine.at(pulseRight)
            val breeze = airLow.process(air.next()) * 0.08f
            left[i] = bandLeft.process(buzzLeft.white()) * sl * sl * sl * sl * swellLeft.next() * 1.3f + breeze
            right[i] = bandRight.process(buzzRight.white()) * sr * sr * sr * sr * swellRight.next() * 1.3f + breeze
        }
    }
}

/** Conversaciones de fondo que no se entienden, alguna taza y el rumor de la sala. */
private class CoffeeHouse : Ambient {
    private val rng = Rng(111)
    private val voices = Array(7) { Voice(Rng(120 + it)) }
    private val room = Brown(Rng(112))
    private val roomLow = LowPass(180f)
    private val murmurLowLeft = LowPass(2600f)
    private val murmurLowRight = LowPass(2600f)
    private val clinks = Grains(24)
    private var untilClink = rng.waitSeconds(1f, 4f)

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var l = 0f
            var r = 0f
            for (voice in voices) {
                val s = voice.next()
                l += s * voice.left
                r += s * voice.right
            }
            if (--untilClink <= 0) {
                untilClink = rng.wait(0.6f)
                clink()
            }
            clinks.next(rng)
            val hum = roomLow.process(room.next()) * 0.25f
            left[i] = murmurLowLeft.process(l) * 0.9f + clinks.left + hum
            right[i] = murmurLowRight.process(r) * 0.9f + clinks.right + hum
        }
    }

    /** Una cucharilla contra una taza: tonos que no son armonicos y se apagan rapido. */
    private fun clink() {
        val hz = rng.range(1800f, 3200f)
        val pan = rng.unit()
        val amp = rng.range(0.015f, 0.05f)
        clinks.add(amp, rng.range(0.08f, 0.2f), pan, hz)
        clinks.add(amp * 0.6f, rng.range(0.05f, 0.12f), pan, hz * 2.32f)
        clinks.add(amp * 0.35f, rng.range(0.03f, 0.08f), pan, hz * 4.25f)
    }

    /** Una voz lejana: un tono con aire que pasa por dos formantes que cambian en cada silaba. */
    private class Voice(private val rng: Rng) {
        private val pitch = rng.range(95f, 230f)
        private val pan = rng.unit()
        val left = panLeft(pan)
        val right = panRight(pan)
        private val loudness = rng.range(0.25f, 1f)
        private val breath = Pink(rng)
        private val firstFormant = BandPass(500f, 5f)
        private val secondFormant = BandPass(1500f, 8f)
        private val intonation = Drift(rng, 0.85f, 1.15f, 0.15f, 0.6f)
        private var talking = rng.unit() < 0.5f
        private var untilSwitch = rng.waitSeconds(1f, 5f)
        private var syllable = 0
        private var syllableLength = 1
        private var firstTarget = 500f
        private var secondTarget = 1500f
        private var first = 500f
        private var second = 1500f
        private var level = 0f
        private var phase = 0f
        private var counter = 0

        fun next(): Float {
            if (--untilSwitch <= 0) {
                talking = !talking
                untilSwitch = if (talking) rng.waitSeconds(1.5f, 6f) else rng.waitSeconds(0.8f, 4f)
            }
            if (++syllable >= syllableLength) {
                syllable = 0
                syllableLength = rng.waitSeconds(0.12f, 0.3f)
                firstTarget = rng.range(300f, 850f)
                secondTarget = rng.range(900f, 2300f)
            }
            if ((counter++ and 63) == 0) {
                first += (firstTarget - first) * 0.3f
                second += (secondTarget - second) * 0.3f
                firstFormant.tune(first, 5f)
                secondFormant.tune(second, 8f)
            }
            val target = if (talking) Sine.at(syllable * 0.5f / syllableLength) * loudness else 0f
            level += (target - level) * 0.002f
            phase += pitch * intonation.next() / SAMPLE_RATE
            if (phase >= 1f) phase -= 1f
            val source = (phase * 2f - 1f) * 0.6f + breath.next() * 0.4f
            return (firstFormant.process(source) + secondFormant.process(source) * 0.6f) * level
        }
    }
}

/** Un acorde grave que respira y, de vez en cuando, un cuenco tibetano. */
private class Meditation : Ambient {
    private val rng = Rng(131)
    private val breath = Drift(rng, 0.5f, 1f, 5f, 10f)
    private val phaseLeft = FloatArray(PARTIALS.size)
    private val phaseRight = FloatArray(PARTIALS.size)
    private val bowls = Grains(32)
    private var untilBowl = rng.waitSeconds(1f, 4f)

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var l = 0f
            var r = 0f
            for (p in PARTIALS.indices) {
                val step = DRONE_HZ * (p + 1) / SAMPLE_RATE
                // Un poco desafinado entre oidos: late despacio.
                phaseLeft[p] += step * 1.0015f
                phaseRight[p] += step * 0.9985f
                if (phaseLeft[p] >= 1f) phaseLeft[p] -= 1f
                if (phaseRight[p] >= 1f) phaseRight[p] -= 1f
                l += Sine.at(phaseLeft[p]) * PARTIALS[p]
                r += Sine.at(phaseRight[p]) * PARTIALS[p]
            }
            if (--untilBowl <= 0) {
                untilBowl = rng.waitSeconds(14f, 28f)
                strike()
            }
            bowls.next(rng)
            val b = breath.next() * 0.09f
            left[i] = l * b + bowls.left
            right[i] = r * b + bowls.right
        }
    }

    private fun strike() {
        val hz = NOTES[(rng.unit() * NOTES.size).toInt()]
        val pan = rng.range(0.3f, 0.7f)
        val amp = rng.range(0.07f, 0.12f)
        for (k in BOWL_RATIOS.indices) {
            // Cada parcial, dos veces y casi igual: el vaiven del cuenco.
            bowls.add(amp * BOWL_AMPS[k], BOWL_SECONDS[k], pan, hz * BOWL_RATIOS[k] - 0.4f)
            bowls.add(amp * BOWL_AMPS[k], BOWL_SECONDS[k], pan, hz * BOWL_RATIOS[k] + 0.4f)
        }
    }

    private companion object {
        const val DRONE_HZ = 110f
        val PARTIALS = floatArrayOf(1f, 0.45f, 0.3f, 0.16f, 0.1f, 0.06f)
        val NOTES = floatArrayOf(196f, 220f, 261.63f, 293.66f, 329.63f)
        val BOWL_RATIOS = floatArrayOf(1f, 2.76f, 5.4f, 8.93f)
        val BOWL_AMPS = floatArrayOf(1f, 0.5f, 0.25f, 0.12f)
        val BOWL_SECONDS = floatArrayOf(4f, 2.5f, 1.4f, 0.8f)
    }
}
