/**
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.starry.myne.ui.screens.reader.main.viewmodel

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.Keep
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.starry.myne.R
import com.starry.myne.ui.theme.poppinsFont

@Keep
sealed class ReaderFont(val id: String, val name: String, val fontFamily: FontFamily) {

    companion object {
        private val fontMap by lazy {
            ReaderFont::class.sealedSubclasses
                .mapNotNull { it.objectInstance }
                .associateBy { it.id }
        }

        fun getAllFonts() = fontMap.values.toList()
        fun getFontById(id: String) = fontMap[id]!!
        fun getFontByName(name: String) = getAllFonts().find { it.name == name }!!
    }



    @Keep
    data object System : ReaderFont("system", "System Default", FontFamily.Default)

    @Keep
    data object Serif : ReaderFont("serif", "Serif", FontFamily.Serif)

    @Keep
    data object Cursive : ReaderFont("cursive", "Cursive", FontFamily.Cursive)

    @Keep
    data object SansSerif : ReaderFont("sans-serif", "SansSerif", FontFamily.SansSerif)

    @Keep
    data object Inter : ReaderFont("inter", "Inter", FontFamily(Font(R.font.reader_inter_font)))

    @Keep
    data object Dyslexic :
        ReaderFont("dyslexic", "OpenDyslexic", FontFamily(Font(R.font.reader_inter_font)))

    @Keep
    data object Lora : ReaderFont("poppins", "Poppins", poppinsFont)

    /**
     * Converts the FontFamily to an Android Typeface for use in Paint and PdfDocument.
     */
    fun toTypeface(context: Context): Typeface {
        return when (this) {
            System -> Typeface.DEFAULT
            Serif -> Typeface.SERIF
            Cursive -> Typeface.create("cursive", Typeface.NORMAL)
            SansSerif -> Typeface.SANS_SERIF
            Inter -> ResourcesCompat.getFont(context, R.font.reader_inter_font) ?: Typeface.DEFAULT
            Dyslexic -> ResourcesCompat.getFont(context, R.font.reader_inter_font) ?: Typeface.DEFAULT
            Lora -> ResourcesCompat.getFont(context, R.font.poppins_regular) ?: Typeface.DEFAULT
        }
    }
}
