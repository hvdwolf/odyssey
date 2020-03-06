/*
 * Copyright (C) 2020 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/odyssey/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.odyssey.dialogs;


import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import org.gateshipone.odyssey.R;
import org.gateshipone.odyssey.playbackservice.PlaybackServiceConnection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;


public class SeekBackwardsStepSizeDialog extends DialogFragment implements SeekBar.OnSeekBarChangeListener {

    private TextView mDialogLabel;

    private int mStepSize;

    private PlaybackServiceConnection mPBSConnection;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final LayoutInflater inflater = getActivity().getLayoutInflater();
        final View seekView = inflater.inflate(R.layout.resume_step_size_dialog, null);

        SeekBar seekBar = seekView.findViewById(R.id.volume_seekbar);
        mDialogLabel = seekView.findViewById(R.id.dialog_text);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        mStepSize = sharedPreferences.getInt(getString(R.string.pref_seek_backwards_key), getResources().getInteger(R.integer.pref_seek_backwards_default));

        seekBar.setProgress(mStepSize);
        seekBar.setOnSeekBarChangeListener(this);

        updateLabels();

        builder.setView(seekView);

        builder.setPositiveButton(R.string.error_dialog_ok_action, ((dialog, which) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(R.string.pref_seek_backwards_key), mStepSize);
            editor.apply();

            try {
                mPBSConnection.getPBS().changeAutoBackwardsSeekAmount(mStepSize);
            } catch (RemoteException ignored) {
            }

            dismiss();
        }));
        builder.setNegativeButton(R.string.dialog_action_cancel, (dialog, which) -> dismiss());

        mPBSConnection = new PlaybackServiceConnection(getContext());
        mPBSConnection.openConnection();

        return builder.create();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mStepSize = progress;
        updateLabels();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void updateLabels() {
        mDialogLabel.setText(getString(R.string.preference_resume_step_size_dialog_title, mStepSize));
    }
}
