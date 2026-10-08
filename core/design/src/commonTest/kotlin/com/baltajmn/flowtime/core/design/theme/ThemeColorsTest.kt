package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Los colores de cada tema tal como salían al pasar a Compose Multiplatform (#61): primary,
 * onPrimary, primaryContainer, onPrimaryContainer, secondary, secondaryContainer, tertiary,
 * tertiaryContainer, background, surface, surfaceVariant, surfaceContainerHighest, outline, onSurface
 * y onSurfaceVariant. Si una librería nueva los cambia, la app cambia de aspecto: que sea a propósito.
 */
class ThemeColorsTest {

    private val expected = mapOf(
        "Blue light" to "136682 ffffff bee9ff 004d65 4d616c d0e6f2 5d5b7d e3dfff f6fafe f6fafe dce4e9 dfe3e7 70787d 171c1f 40484c",
        "Blue dark" to "8ccff0 003546 004d65 bee9ff b4cad6 354a54 c7c2ea 464364 0f1417 0f1417 40484c 303538 8a9297 dfe3e7 c0c8cd",
        "Pink light" to "8f4a4e ffffff ffdada 733337 765657 ffdada 76592f ffddb1 fff8f7 fff8f7 f4dddd f0dede 857373 221919 524343",
        "Pink dark" to "ffb3b5 561d22 733337 ffdada e6bdbd 5d3f40 e6c18d 5c421a 1a1111 1a1111 524343 3d3232 9f8c8c f0dede d7c1c1",
        "Grey light" to "5e5e5d ffffff e3e2e0 464745 5e5e5d e3e2e0 466270 c9e7f7 faf9f7 faf9f7 e3e2e0 e3e2e0 777775 1b1c1b 464745",
        "Grey dark" to "c7c6c4 30312f 464745 e3e2e0 c7c6c4 464745 adcbdb 2e4a58 121413 121413 464745 343534 91918f e3e2e0 c7c6c4",
        "Beige light" to "765a0b ffffff ffdf98 5a4300 6a5d3f f4e0bb 496548 cbebc6 fff8f2 fff8f2 ece1cf ebe1d4 7e7667 1f1b13 4d4639",
        "Beige dark" to "e7c26c 3f2e00 5a4300 ffdf98 d7c5a0 52452a afcfab 324d32 17130b 17130b 4d4639 39342b 999080 ebe1d4 d0c5b4",
        "Brown light" to "845416 ffffff ffddbb 673d00 725a41 feddbd 56633b d9e9b6 fff8f4 fff8f4 f1dfd0 eee0d5 827568 211a14 50453a",
        "Brown dark" to "faba73 482900 673d00 ffddbb e0c1a3 58432c bdcc9c 3f4b26 18120c 18120c 50453a 3b332c 9d8e81 eee0d5 d4c4b5",
        "Olive light" to "666014 ffffff eee58c 4d4800 635f42 e9e4be 3f6654 c1ecd5 fef9eb fef9eb e7e3d0 e7e2d5 7a7768 1d1c14 49473a",
        "Olive dark" to "d1c973 353100 4d4800 eee58c cdc8a3 4a482c a6d0b9 274e3d 14140c 14140c 49473a 36352c 949181 e7e2d5 cbc7b5",
        "Marine light" to "136682 ffffff bee9ff 004d64 4d616c d0e6f2 5d5b7d e3dfff f6fafe f6fafe dce4e9 dfe3e7 70787d 171c1f 40484c",
        "Marine dark" to "8bd0f0 003546 004d64 bee9ff b4cad6 354a54 c6c2ea 454364 0f1417 0f1417 40484c 303538 8a9297 dfe3e7 c0c8cd",
        "Green light" to "36693e ffffff b8f1b9 1d5128 516351 d4e8d1 39656c bdeaf3 f7fbf2 f7fbf2 dde5d9 e0e4db 727970 181d18 424940",
        "Green dark" to "9cd49f 003913 1d5128 b8f1b9 b8ccb5 3a4b3a a1ced6 1f4d54 101510 101510 424940 313630 8b9389 e0e4db c1c9be",
        "Purple light" to "7a4f80 ffffff fed6ff 613767 6b596b f4dbf2 82524b ffdad5 fff7fa fff7fa ecdfe9 eadfe6 7e747d 1f1a1f 4d444c",
        "Purple dark" to "eab5ee 48204f 613767 fed6ff d7bfd5 524153 f5b8af 673b35 171217 171217 4d444c 393338 998d96 eadfe6 d0c3cd",
        "Orange light" to "8d4d2d ffffff ffdbcc 703718 765749 ffdbcc 655f31 ede4a9 fff8f6 fff8f6 f4ded5 f0dfd8 85736c 221a16 52443d",
        "Orange dark" to "ffb693 542104 703718 ffdbcc e6beac 5c4033 d0c890 4d481c 1a120e 1a120e 52443d 3d332e a08d85 f0dfd8 d7c2b9",
        "Black light" to "000000 ffffff e4e1eb 47464e 5e5d66 e4e1eb 466270 c9e7f7 fcf8ff fcf8ff e4e1eb e4e1eb 77767f 1b1b22 47464e",
        "Black dark" to "ffffff 000000 47464e e4e1eb c8c5cf 47464e adcbdb 2e4a58 13131a 13131a 47464e 34343c 918f99 e4e1eb c8c5cf",
        "Supporter light" to "7a590c ffffff ffdea5 5d4200 6c5c3f f6e0bb 4c6545 ceebc2 fff8f3 fff8f3 eee1cf ebe1d4 7f7667 201b13 4e4639",
        "Supporter dark" to "ecc06c 412d00 5d4200 ffdea5 d9c4a0 53452a b2cfa7 354d2f 17130b 17130b 4e4639 3a342b 9a8f80 ebe1d4 d1c5b4",
        "Lavender light" to "63568f ffffff e8deff 4b3e76 615b71 e7def8 7d5261 ffd9e4 fdf7ff fdf7ff e6e0ec e6e1e9 79757f 1c1b20 48454e",
        "Lavender dark" to "cdbdff 34275e 4b3e76 e8deff cbc3dc 494458 eeb8ca 633b4a 141318 141318 48454e 36343a 938f99 e6e1e9 cac4cf",
        "Mint light" to "186b52 ffffff a5f2d3 00513d 4c6359 cee9db 3f6375 c2e8fd f5fbf6 f5fbf6 dbe5de dee4df 707974 171d1a 404944",
        "Mint dark" to "8ad6b8 003829 00513d a5f2d3 b3ccc0 354c42 a7cce1 264b5c 0f1512 0f1512 404944 303633 89938d dee4df bfc9c3",
        "Coral light" to "8f4b38 ffffff ffdbd1 723523 77574e ffdbd1 6c5d2f f6e1a6 fff8f6 fff8f6 f5ded8 f1dfda 85736e 231917 53433f",
        "Coral dark" to "ffb5a0 561f0f 723523 ffdbd1 e7bdb2 5d4037 d9c58d 534619 1a110f 1a110f 53433f 3d322f a08c87 f1dfda d8c2bc",
        "Sand light" to "765a0b ffffff ffdf9a 5a4300 6b5d3f f4e0bb 496548 cbebc5 fff8f2 fff8f2 ece1cf ebe1d4 7e7667 1f1b13 4d4639",
        "Sand dark" to "e7c26c 3f2e00 5a4300 ffdf9a d7c4a0 52452a afcfab 324d31 17130b 17130b 4d4639 39342b 999080 ebe1d4 d0c5b4",
        "Night light" to "485d92 ffffff dae2ff 304578 585e71 dce2f9 735572 fed7fa faf8ff faf8ff e1e2ec e2e2e9 757780 1a1b21 44464f",
        "Night dark" to "b1c5ff 172e60 304578 dae2ff c0c6dc 404659 e0bbdd 593d59 121318 121318 44464f 33343a 8f9099 e2e2e9 c5c6d0",
        "Cherry light" to "8e4954 ffffff ffd9dd 72333d 76565a ffd9dd 785831 ffddb8 fff8f7 fff8f7 f4dddf f0dedf 847374 22191a 524344",
        "Cherry dark" to "ffb2bb 561d28 72333d ffd9dd e5bdc0 5c3f43 e9bf8f 5e411c 1a1112 1a1112 524344 3d3233 9f8c8e f0dedf d7c1c3"
    )

    private fun ColorScheme.snapshot() = listOf(
        primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, secondaryContainer, tertiary,
        tertiaryContainer, background, surface, surfaceVariant, surfaceContainerHighest, outline, onSurface,
        onSurfaceVariant
    ).joinToString(" ") { it.toArgb().toUInt().toString(16).padStart(8, '0').drop(2) }

    @Test
    fun theColorsOfEveryThemeStayTheSame() {
        val actual = AppTheme.entries.flatMap { theme ->
            listOf(false, true).map { dark -> "${theme.name} ${if (dark) "dark" else "light"}" to theme.colorScheme(dark).snapshot() }
        }.toMap()

        assertEquals(expected, actual)
    }
}
