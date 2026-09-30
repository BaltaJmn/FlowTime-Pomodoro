package com.baltajmn.flowtime.core.design.sound

/*
 * Los sonidos de Pro (#44), generados como los demás: sin ficheros ni licencias.
 */

/** Agua que corre entre piedras: un rumor de fondo y muchas burbujas cortas que suben de tono. */
internal class Stream : Ambient {
    private val rng = Rng(201)
    private val bedLeft = Pink(Rng(202))
    private val bedRight = Pink(Rng(203))
    private val bandLeft = BandPass(900f, 0.7f)
    private val bandRight = BandPass(1100f, 0.7f)
    private val flow = Drift(rng, 0.6f, 1f, 0.3f, 1.5f)
    private val bubbles = Grains(96)
    private var untilBubble = 1

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (--untilBubble <= 0) {
                untilBubble = rng.wait(70f)
                bubbles.add(
                    amplitude = rng.range(0.02f, 0.09f),
                    seconds = rng.range(0.006f, 0.03f),
                    pan = rng.unit(),
                    hz = rng.range(350f, 1400f),
                    glide = rng.range(1.3f, 2.2f)
                )
            }
            bubbles.next(rng)
            val f = flow.next()
            left[i] = bandLeft.process(bedLeft.next()) * f * 0.55f + bubbles.left
            right[i] = bandRight.process(bedRight.next()) * f * 0.55f + bubbles.right
        }
    }
}

/** Una noche de verano: grillos que cantan cada uno a su ritmo sobre un fondo casi callado. */
internal class Crickets : Ambient {
    private val rng = Rng(211)
    private val nightLeft = Brown(Rng(212))
    private val nightRight = Brown(Rng(213))
    private val nightLowLeft = LowPass(500f)
    private val nightLowRight = LowPass(500f)
    private val crickets = Array(5) { Cricket(Rng(220 + it)) }

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            var l = nightLowLeft.process(nightLeft.next()) * 0.25f
            var r = nightLowRight.process(nightRight.next()) * 0.25f
            for (cricket in crickets) {
                val s = cricket.next()
                l += s * cricket.left
                r += s * cricket.right
            }
            left[i] = l
            right[i] = r
        }
    }

    /** Un grillo: trinos de 3 o 4 pulsos muy seguidos, a su tono, y una pausa. */
    private class Cricket(private val rng: Rng) {
        private val tone = rng.range(3900f, 5200f) / SAMPLE_RATE
        private val pan = rng.unit()
        val left = panLeft(pan)
        val right = panRight(pan)
        private val loudness = rng.range(0.03f, 0.09f)
        private val pulse = (rng.range(0.012f, 0.018f) * SAMPLE_RATE).toInt()
        private val pause = rng.range(0.35f, 0.9f)
        private val swell = Drift(rng, 0.3f, 1f, 3f, 10f)
        private var phase = 0f
        private var t = 0
        private var pulses = 0
        private var wait = rng.waitSeconds(0f, 1f)

        fun next(): Float {
            val s = swell.next()
            if (pulses == 0) {
                if (--wait > 0) return 0f
                pulses = 3 + (rng.unit() * 2).toInt()
                t = 0
            }
            phase += tone
            if (phase >= 1f) phase -= 1f
            // Cada pulso dura [pulse] y le sigue un silencio igual.
            val inPulse = t % (2 * pulse)
            val env = if (inPulse < pulse) Sine.at(inPulse * 0.5f / pulse) else 0f
            if (++t >= 2 * pulse * pulses) {
                pulses = 0
                wait = rng.waitSeconds(pause * 0.8f, pause * 1.2f)
            }
            return Sine.at(phase) * env * loudness * s
        }
    }
}

/** Un ventilador de pie: aire constante, el zumbido del motor y el paso de las aspas. */
internal class Fan : Ambient {
    private val rng = Rng(231)
    private val airLeft = Pink(Rng(232))
    private val airRight = Pink(Rng(233))
    private val airLowLeft = LowPass(1400f)
    private val airLowRight = LowPass(1400f)
    private val wobble = Drift(rng, 0.9f, 1f, 2f, 6f)
    private var motor = 0f
    private var blades = 0f

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            motor += 118f / SAMPLE_RATE
            if (motor >= 1f) motor -= 1f
            blades += 21f / SAMPLE_RATE
            if (blades >= 1f) blades -= 1f
            val sweep = (0.85f + 0.15f * Sine.at(blades)) * wobble.next()
            val hum = (Sine.at(motor) + 0.4f * Sine.at(motor * 2f % 1f)) * 0.012f
            left[i] = airLowLeft.process(airLeft.next()) * sweep * 0.5f + hum
            right[i] = airLowRight.process(airRight.next()) * sweep * 0.5f + hum
        }
    }
}

/** Dentro de un tren: el rumor grave del vagón y el traqueteo de las ruedas en las juntas de la vía. */
internal class Train : Ambient {
    private val rng = Rng(241)
    private val rumbleLeft = Brown(Rng(242))
    private val rumbleRight = Brown(Rng(243))
    private val rumbleLowLeft = LowPass(220f)
    private val rumbleLowRight = LowPass(220f)
    private val clackBand = BandPass(700f, 1.5f)
    private val sway = Drift(rng, 0.8f, 1f, 1.5f, 5f)
    private val clacks = Grains(16)
    private var untilJoint = 1
    private var untilSecond = 0

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            // Dos golpes seguidos por junta (los dos ejes del bogie), más o menos cada segundo.
            if (--untilJoint <= 0) {
                untilJoint = rng.waitSeconds(0.95f, 1.1f)
                untilSecond = rng.waitSeconds(0.13f, 0.16f)
                clack()
            }
            if (untilSecond > 0 && --untilSecond == 0) clack()
            clacks.next(rng)
            val s = sway.next()
            val clack = clackBand.process(clacks.left + clacks.right)
            left[i] = rumbleLowLeft.process(rumbleLeft.next()) * s * 0.6f + clack
            right[i] = rumbleLowRight.process(rumbleRight.next()) * s * 0.6f + clack
        }
    }

    private fun clack() = clacks.add(amplitude = rng.range(0.25f, 0.4f), seconds = 0.02f, pan = 0.5f)
}

/** Alguien que escribe a buen ritmo: ráfagas de teclas, alguna barra espaciadora y pausas para pensar. */
internal class Typing : Ambient {
    private val rng = Rng(251)
    private val roomLeft = Pink(Rng(252))
    private val roomRight = Pink(Rng(253))
    private val roomLowLeft = LowPass(600f)
    private val roomLowRight = LowPass(600f)
    private val keyBand = BandPass(2600f, 1.2f)
    private val spaceBand = BandPass(900f, 1.2f)
    private val keys = Grains(24)
    private val spaces = Grains(8)
    private var untilKey = 1
    private var word = 0

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            if (--untilKey <= 0) {
                if (word == 0) {
                    // Entre palabras la barra espaciadora; de vez en cuando, una pausa larga.
                    spaces.add(amplitude = rng.range(0.3f, 0.45f), seconds = 0.006f, pan = 0.5f)
                    word = 2 + (rng.unit() * 8).toInt()
                    untilKey = if (rng.unit() < 0.12f) rng.waitSeconds(1.5f, 4f) else rng.waitSeconds(0.12f, 0.25f)
                } else {
                    word--
                    keys.add(amplitude = rng.range(0.25f, 0.5f), seconds = rng.range(0.002f, 0.004f), pan = rng.range(0.35f, 0.65f))
                    untilKey = rng.waitSeconds(0.07f, 0.18f)
                }
            }
            keys.next(rng)
            spaces.next(rng)
            val key = keyBand.process(keys.left + keys.right)
            val space = spaceBand.process(spaces.left + spaces.right)
            left[i] = roomLowLeft.process(roomLeft.next()) * 0.3f + key * 2f + space * 1.5f
            right[i] = roomLowRight.process(roomRight.next()) * 0.3f + key * 2f + space * 1.5f
        }
    }
}

/** La lluvia sobre la lona de una tienda de campaña: golpes sordos y cercanos, y el agua que resbala. */
internal class TentRain : Ambient {
    private val rng = Rng(261)
    private val bedLeft = Pink(Rng(262))
    private val bedRight = Pink(Rng(263))
    private val bedLowLeft = LowPass(900f)
    private val bedLowRight = LowPass(900f)
    private val tapLowLeft = LowPass(1500f)
    private val tapLowRight = LowPass(1500f)
    private val tapHighLeft = HighPass(150f)
    private val tapHighRight = HighPass(150f)
    private val intensity = Drift(rng, 0.6f, 1f, 2f, 8f)
    private val taps = Grains(96)
    private var untilTap = 1

    override fun render(left: FloatArray, right: FloatArray, frames: Int) {
        for (i in 0 until frames) {
            val n = intensity.next()
            if (--untilTap <= 0) {
                untilTap = rng.wait(110f * n)
                val u = rng.unit()
                taps.add(amplitude = 0.15f + 0.6f * u * u, seconds = rng.range(0.004f, 0.012f), pan = rng.unit())
            }
            taps.next(rng)
            left[i] = bedLowLeft.process(bedLeft.next()) * n * 0.35f + tapHighLeft.process(tapLowLeft.process(taps.left))
            right[i] = bedLowRight.process(bedRight.next()) * n * 0.35f + tapHighRight.process(tapLowRight.process(taps.right))
        }
    }
}
