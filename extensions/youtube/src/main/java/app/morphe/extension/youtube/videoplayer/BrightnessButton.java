package app.morphe.extension.youtube.videoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.youtube.patches.LegacyPlayerControlsPatch;
import app.morphe.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public class BrightnessButton {

    /** Window brightness while dimmed, from 0.0 to 1.0. */
    private static final float DIMMED_BRIGHTNESS = 0.2f;

    private static boolean dimmed;
    private static float brightnessBeforeDim = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    private static WeakReference<ImageView> overlayButtonRef = new WeakReference<>(null);
    private static WeakReference<Activity> activityRef = new WeakReference<>(null);

    /**
     * Injection point.
     */
    public static void initializeButton(View controlsView) {
        try {
            if (LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS
                    || !Settings.BRIGHTNESS_BUTTON.get()) {
                return;
            }

            overlayButtonRef = new WeakReference<>(PlayerOverlayButton.addButton(
                    controlsView,
                    getIconName(),
                    BrightnessButton::toggleBrightness,
                    null
            ));
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * The dimmed brightness is per video, so it is restored when a new video starts.
     */
    public static void resetBrightnessButton() {
        if (!dimmed) return;

        Utils.runOnMainThread(() -> {
            try {
                Activity activity = activityRef.get();
                if (activity != null) {
                    setWindowBrightness(activity, brightnessBeforeDim);
                }
                dimmed = false;
                updateButtonIcon();
            } catch (Exception ex) {
                Logger.printException(() -> "resetBrightnessButton failure", ex);
            }
        });
    }

    private static void toggleBrightness(View view) {
        try {
            Activity activity = findActivity(view.getContext());
            if (activity == null) {
                Logger.printDebug(() -> "Brightness button: no activity found");
                return;
            }
            activityRef = new WeakReference<>(activity);

            if (dimmed) {
                setWindowBrightness(activity, brightnessBeforeDim);
                dimmed = false;
            } else {
                brightnessBeforeDim = activity.getWindow().getAttributes().screenBrightness;
                setWindowBrightness(activity, DIMMED_BRIGHTNESS);
                dimmed = true;
            }
            updateButtonIcon();
        } catch (Exception ex) {
            Logger.printException(() -> "toggleBrightness failure", ex);
        }
    }

    private static void setWindowBrightness(Activity activity, float brightness) {
        Window window = activity.getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = brightness;
        window.setAttributes(params);
    }

    @Nullable
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper wrapper) {
            if (wrapper instanceof Activity activity) {
                return activity;
            }
            context = wrapper.getBaseContext();
        }
        return null;
    }

    private static void updateButtonIcon() {
        Utils.verifyOnMainThread();

        ImageView overlayButton = overlayButtonRef.get();
        if (overlayButton != null) {
            overlayButton.setImageResource(PlayerIcons.id(getIconBaseName()));
        }
    }

    private static String getIconName() {
        return PlayerIcons.name(getIconBaseName());
    }

    private static String getIconBaseName() {
        return dimmed
                ? "morphe_brightness_button_on"
                : "morphe_brightness_button_off";
    }
}
