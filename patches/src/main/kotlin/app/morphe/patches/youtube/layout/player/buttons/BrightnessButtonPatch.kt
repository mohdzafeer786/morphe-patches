package app.morphe.patches.youtube.layout.player.buttons

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.layout.buttons.overlay.addPlayerOverlayPreferences
import app.morphe.patches.youtube.layout.buttons.overlay.playerOverlayButtonsSettingsPatch
import app.morphe.patches.youtube.layout.player.icons.copyPlayerButtonIcons
import app.morphe.patches.youtube.misc.extension.sharedExtensionPatch
import app.morphe.patches.youtube.misc.playercontrols.legacyPlayerControlsPatch
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.shared.hookVideoStarted
import app.morphe.patches.youtube.shared.startVideoInformerPatch

private const val EXTENSION_BUTTON =
    "Lapp/morphe/extension/youtube/videoplayer/BrightnessButton;"

private val brightnessButtonResourcePatch = patch {
    dependsOn(
        settingsPatch,
        legacyPlayerControlsPatch
    )

    execute {
        copyPlayerButtonIcons("brightnessbutton", "morphe_brightness_button_off", "morphe_brightness_button_on")
    }
}

val brightnessButtonPatch = patch(
    name = "Brightness button",
    description = "Adds an option to show a player button that dims the screen while watching.",
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
        startVideoInformerPatch,
        playerOverlayButtonsSettingsPatch,
        playerOverlayButtonsHookPatch,
        legacyPlayerControlsPatch,
        brightnessButtonResourcePatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        addPlayerOverlayPreferences(
            SwitchPreference("morphe_brightness_button")
        )

        addPlayerBottomButton(EXTENSION_BUTTON)

        hookVideoStarted("$EXTENSION_BUTTON->resetBrightnessButton()V")
    }
}
