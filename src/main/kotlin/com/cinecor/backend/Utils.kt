package com.cinecor.backend

import com.cinecor.backend.model.Movie
import com.vivekpanyam.iris.Bitmap
import com.vivekpanyam.iris.Color
import com.vivekpanyam.iris.Palette
import java.io.IOException
import java.net.URL
import javax.imageio.ImageIO

object Utils {

    fun getMovieColorsFromUrl(url: String?): HashMap<String, String>? {
        try {
            val palette = Palette.Builder(Bitmap(ImageIO.read(URL(url)))).generate()
            if (palette != null) {
                var swatch: Palette.Swatch? = palette.vibrantSwatch
                if (swatch == null) swatch = palette.mutedSwatch
                if (swatch == null) return null

                val colors = HashMap<String, String>()
                colors.put(Movie.Colors.MAIN.name, formatColor(swatch.rgb))
                colors.put(Movie.Colors.TITLE.name, formatColor(swatch.titleTextColor))
                return colors
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun formatColor(color: Int): String {
        return String.format("#%02x%02x%02x", Color.red(color), Color.green(color), Color.blue(color))
    }
}
