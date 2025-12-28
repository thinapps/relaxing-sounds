package top.thinapps.relaxingsounds.core

import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import top.thinapps.relaxingsounds.R

data class SoundItem(
    val key: String,
    @RawRes val rawResId: Int,
    @StringRes val titleResId: Int,
    @StringRes val subtitleResId: Int,
    @DrawableRes val backgroundResId: Int,
    val homeCardId: Int? = null
)

object SoundCatalog {

    val sounds = listOf(
        SoundItem(
            key = "ocean",
            rawResId = R.raw.ocean_waves,
            titleResId = R.string.sound_ocean_title,
            subtitleResId = R.string.sound_ocean_subtitle,
            backgroundResId = R.drawable.bg_sound_ocean,
            homeCardId = R.id.card_ocean_waves
        ),
        SoundItem(
            key = "rain",
            rawResId = R.raw.rain,
            titleResId = R.string.sound_rain_title,
            subtitleResId = R.string.sound_rain_subtitle,
            backgroundResId = R.drawable.bg_sound_rain,
            homeCardId = R.id.card_rain
        ),
        SoundItem(
            key = "waterfall",
            rawResId = R.raw.waterfall,
            titleResId = R.string.sound_waterfall_title,
            subtitleResId = R.string.sound_waterfall_subtitle,
            backgroundResId = R.drawable.bg_sound_ocean,
            homeCardId = R.id.card_waterfall
        ),
        SoundItem(
            key = "brown",
            rawResId = R.raw.brown_noise,
            titleResId = R.string.sound_brown_title,
            subtitleResId = R.string.sound_brown_subtitle,
            backgroundResId = R.drawable.bg_sound_brown,
            homeCardId = R.id.card_brown_noise
        )
    )

    fun getByKey(key: String): SoundItem? =
        sounds.firstOrNull { it.key == key }
}
