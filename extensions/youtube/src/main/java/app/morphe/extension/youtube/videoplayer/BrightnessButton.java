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

    /**
     * Window brightness set by each tap: 25%, 50%, 75%, 100%, and the lowest level.
     * The lowest level is 1% and not exactly 0, since some devices turn the screen off at 0.
     */
    private static final float[] LEVELS = {0.25f, 0.50f, 0.75f, 1.0f, 0.01f};

    private static final String[] LEVEL_ICONS = {
            "morphe_brightness_button_25",
            "morphe_brightness_button_50",
            "morphe_brightness_button_75",
            "morphe_brightness_button_100",
            "morphe_brightness_button_0"
    };

    private static final String SYSTEM_ICON = "morphe_brightness_button_off";

    /** Index of the current level, or -1 while the system brightness is used. */
    private static int level = -1;
    private static float brightnessBeforeChange = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    @Nullable
    private static LegacyPlayerControlButton legacy;

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
                    BrightnessButton::onTap,
                    BrightnessButton::onLongPress
            ));
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    /**
     * Injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            if (!LegacyPlayerControlsPatch.RESTORE_OLD_PLAYER_BUTTONS) {
                return;
            }

            legacy = new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_brightness_button",
                    null,
                    getIconName(),
                    Settings.BRIGHTNESS_BUTTON,
                    BrightnessButton::onTap,
                    BrightnessButton::onLongPress
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initializeLegacyButton failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * The brightness is per video, so the system brightness is restored when a new video starts.
     */
    public static void resetBrightnessButton() {
        if (level < 0) return;

        Utils.runOnMainThread(() -> {
            try {
                restoreSystemBrightness(activityRef.get());
                updateButtonIcon();
            } catch (Exception ex) {
                Logger.printException(() -> "resetBrightnessButton failure", ex);
            }
        });
    }

    private static void onTap(View view) {
        try {
            Activity activity = findActivity(view.getContext());
            if (activity == null) {
                Logger.printDebug(() -> "Brightness button: no activity found");
                return;
            }
            activityRef = new WeakReference<>(activity);

            if (level < 0) {
                brightnessBeforeChange = activity.getWindow().getAttributes().screenBrightness;
            }

            level = (level + 1) % LEVELS.length;
            setWindowBrightness(activity, LEVELS[level]);
            updateButtonIcon();
        } catch (Exception ex) {
            Logger.printException(() -> "onTap failure", ex);
        }
    }

    private static boolean onLongPress(View view) {
        try {
            restoreSystemBrightness(findActivity(view.getContext()));
            updateButtonIcon();
        } catch (Exception ex) {
            Logger.printException(() -> "onLongPress failure", ex);
        }
        return true;
    }

    private static void restoreSystemBrightness(@Nullable Activity activity) {
        if (activity != null && level >= 0) {
            setWindowBrightness(activity, brightnessBeforeChange);
        }
        level = -1;
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

        final int icon = PlayerIcons.id(getIconBaseName());

        ImageView overlayButton = overlayButtonRef.get();
        if (overlayButton != null) {
            overlayButton.setImageResource(icon);
        }

        if (legacy != null) {
            legacy.setIcon(icon);
        }
    }

    private static String getIconName() {
        return PlayerIcons.name(getIconBaseName());
    }

    private static String getIconBaseName() {
        return level < 0 ? SYSTEM_ICON : LEVEL_ICONS[level];
    }
}
