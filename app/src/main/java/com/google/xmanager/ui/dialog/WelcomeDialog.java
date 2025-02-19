/*
 * This file is part of DAndroid.
 *
 * DAndroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DAndroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DAndroid.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2022 DAndroid Contributors
 */

package com.google.xmanager.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.xmanager.App;
import com.google.xmanager.ConfigManager;
import com.google.xmanager.R;
import com.google.xmanager.ui.fragment.BaseFragment;
import com.google.xmanager.util.ShortcutUtil;

public class WelcomeDialog extends DialogFragment {
    private static boolean shown = false;

    private Dialog parasiticDialog(BlurBehindDialogBuilder builder) {
        var shortcutSupported = ShortcutUtil.isRequestPinShortcutSupported(requireContext());
        builder
                .setTitle(R.string.parasitic_welcome)
                .setMessage(shortcutSupported ? R.string.parasitic_welcome_summary :
                        R.string.parasitic_welcome_summary_no_shortcut_support)
                .setNegativeButton(R.string.never_show, (dialog, which) ->
                        App.getPreferences().edit().putBoolean("never_show_welcome", true).apply())
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.create_shortcut, (dialog, which) -> {
                    var home = (BaseFragment) getParentFragment();
                    if (!ShortcutUtil.requestPinLaunchShortcut(() -> {
                        App.getPreferences().edit().putBoolean("never_show_welcome", true).apply();
                        if (home != null) {
                            home.showHint(R.string.settings_shortcut_pinned_hint, false);
                        }
                    })) {
                        if (home != null) {
                            home.showHint(R.string.settings_unsupported_pin_shortcut_summary, false);
                        }
                    }
                });
        return builder.create();
    }

    private Dialog appDialog(BlurBehindDialogBuilder builder) {

        return builder
                .setTitle(R.string.app_welcome)
                .setMessage(R.string.app_welcome_summary)
                .setNegativeButton(R.string.never_show, (d, w) ->
                        App.getPreferences().edit().putBoolean("never_show_welcome", true).apply())
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        var builder = new BlurBehindDialogBuilder(requireContext(),
                R.style.ThemeOverlay_MaterialAlertDialog_Centered_FullWidthButtons);
        if (App.isParasitic) {
            return parasiticDialog(builder);
        } else {
            return appDialog(builder);
        }
    }

    public static void showIfNeed(FragmentManager fm) {
        if (shown) return;
        if (!ConfigManager.isBinderAlive() ||
                App.getPreferences().getBoolean("never_show_welcome", false) ||
                (App.isParasitic && ShortcutUtil.isLaunchShortcutPinned())) {
            shown = true;
            return;
        }
        new WelcomeDialog().show(fm, "welcome");
        shown = true;
    }
}
