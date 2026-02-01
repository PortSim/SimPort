import androidx.compose.ui.graphics.Color

class ColorScale(
    val _1: Color,
    val _2: Color,
    val _3: Color,
    val _4: Color,
    val _5: Color,
    val _6: Color,
    val _7: Color,
    val _8: Color,
    val _9: Color,
)

class ColorPalette(
    val grays: ColorScale,
    val primaries: ColorScale,
    val reds: ColorScale,
    val greens: ColorScale,
    val yellows: ColorScale,
)

val DefaultColorPalette =
    ColorPalette(
        grays =
            ColorScale(
                _1 = Color(0xFF0E0F13),
                _2 = Color(0xFF46474B),
                _3 = Color(0xFF5C5D66),
                _4 = Color(0xFF7B7E89),
                _5 = Color(0xFF8A8C9E),
                _6 = Color(0xFFB9BDD3),
                _7 = Color(0xFFC7D0E4),
                _8 = Color(0xFFDCE6F9),
                _9 = Color(0xFFEDF0FF),
            ),
        primaries =
            ColorScale(
                _1 = Color(0xFF210082),
                _2 = Color(0xFF3008A0),
                _3 = Color(0xFF3100BE),
                _4 = Color(0xFF511AF5),
                _5 = Color(0xFF4849FF),
                _6 = Color(0xFF8C66FF),
                _7 = Color(0xFFB295FF),
                _8 = Color(0xFFD6C9FF),
                _9 = Color(0xFFE7E0FF),
            ),
        reds =
            ColorScale(
                _1 = Color(0xFF550000),
                _2 = Color(0xFF920000),
                _3 = Color(0xFFB30000),
                _4 = Color(0xFFE50000),
                _5 = Color(0xFFFF0000),
                _6 = Color(0xFFFA9481),
                _7 = Color(0xFFFFB7B7),
                _8 = Color(0xFFFFE0E0),
                _9 = Color(0xFFFFEDED),
            ),
        greens =
            ColorScale(
                _1 = Color(0xFF0D4813),
                _2 = Color(0xFF096317),
                _3 = Color(0xFF1F732A),
                _4 = Color(0xFF0C801C),
                _5 = Color(0xFF149E25),
                _6 = Color(0xFF6BD38E),
                _7 = Color(0xFFA6F5C1),
                _8 = Color(0xFFC9FFDC),
                _9 = Color(0xFFE6FFEA),
            ),
        yellows =
            ColorScale(
                _1 = Color(0xFF70560E),
                _2 = Color(0xFF7D660F),
                _3 = Color(0xFFA58613),
                _4 = Color(0xFFD9B019),
                _5 = Color(0xFFF6E946),
                _6 = Color(0xFFECDF16),
                _7 = Color(0xFFE7F280),
                _8 = Color(0xFFF4FEA9),
                _9 = Color(0xFFFBFFD1),
            ),
    )
