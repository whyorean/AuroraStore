/*
 * Aurora Store
 * Copyright (C) 2018  Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store (a fork of Yalp Store )is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.dragons.aurora;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Preference} that displays a list of entries as a dialog.
 * <p>
 * This preference will store a set of strings into the SharedPreferences. This
 * set will contain one or more values from the
 * {@link #setEntryValues(CharSequence[])} array.
 */
public class MultiSelectListPreference extends DialogPreference {
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private Set<String> mValues = new HashSet<>();
    private Set<String> mNewValues;
    private boolean mPreferenceChanged;

    public MultiSelectListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.MultiSelectListPreference);

        final int n = a.getIndexCount();

        for (int i = 0; i < n; i++) {
            final int index = a.getIndex(i);
            if (index == R.styleable.MultiSelectListPreference_android_entries) {
                mEntries = a.getTextArray(index);
            } else if (index == R.styleable.MultiSelectListPreference_android_entryValues) {
                mEntryValues = a.getTextArray(index);
            }
        }
        a.recycle();
    }

    /**
     * Sets the human-readable entries to be shown in the list. This will be
     * shown in subsequent dialogs.
     * <p>
     * Each entry must have a corresponding index in
     * {@link #setEntryValues(CharSequence[])}.
     *
     * @param entries The entries.
     * @see #setEntryValues(CharSequence[])
     */
    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    /**
     * The array to find the value to save for a preference when an entry from
     * entries is selected. If a user clicks on the second item in entries, the
     * second item in this array will be saved to the preference.
     *
     * @param entryValues The array to be used as values to save for the
     *                    preference.
     */
    public void setEntryValues(CharSequence[] entryValues) {
        mEntryValues = entryValues;
    }

    /**
     * Sets the value of the key. This should contain entries in
     * {@link #getEntryValues()}.
     *
     * @param values The values to set for the key.
     */
    public void setValues(Set<String> values) {
        mValues.clear();
        mValues.addAll(values);

        // we shouldn't re-use the hash set, because
        // persistStringSet() method does not copy the passed
        // arguments.
        final HashSet<String> clonedValues = new HashSet<>(values);
        persistStringSetCompat(clonedValues);
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (mEntries == null || mEntryValues == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference requires an entries array and "
                            + "an entryValues array.");
        }

        if (mNewValues == null) {
            mNewValues = new HashSet<>();
            mNewValues.addAll(mValues);
            mPreferenceChanged = false;
        }

        final boolean[] checkedItems = getSelectedItems(mNewValues);
        builder.setMultiChoiceItems(mEntries, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    public void onClick(DialogInterface dialog, int which,
                                        boolean isChecked) {
                        if (isChecked) {
                            mPreferenceChanged |= mNewValues
                                    .add(mEntryValues[which].toString());
                        } else {
                            mPreferenceChanged |= mNewValues
                                    .remove(mEntryValues[which].toString());
                        }
                    }
                });
    }

    private boolean[] getSelectedItems(final Set<String> values) {
        final CharSequence[] entries = mEntryValues;
        final int entryCount = entries.length;
        boolean[] result = new boolean[entryCount];

        for (int i = 0; i < entryCount; i++) {
            result[i] = values.contains(entries[i].toString());
        }

        return result;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && mPreferenceChanged) {
            final Set<String> values = mNewValues;
            if (callChangeListener(values)) {
                setValues(values);
            }
        }
        mNewValues = null;
        mPreferenceChanged = false;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        final CharSequence[] defaultValues = a.getTextArray(index);
        final Set<String> result = new HashSet<>();

        if (null != defaultValues) {
            for (CharSequence defaultValue : defaultValues) {
                result.add(defaultValue.toString());
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValues(restoreValue ? getPersistedStringSetCompat(mValues)
                : (Set<String>) defaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.values = mValues;
        myState.newValues = mNewValues;
        myState.preferenceChanged = mPreferenceChanged;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            final SavedState myState = (SavedState) state;
            if (myState.values != null) {
                mValues = myState.values;
            }
            if (myState.newValues != null) {
                mNewValues = myState.newValues;
            }
            mPreferenceChanged = myState.preferenceChanged;

            super.onRestoreInstanceState(myState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    protected boolean persistStringSetCompat(Set<String> values) {
        BlackWhiteListManager manager = new BlackWhiteListManager(this.getContext());
        return manager.set(values);
    }

    protected Set<String> getPersistedStringSetCompat(Set<String> defaultReturnValue) {
        BlackWhiteListManager manager = new BlackWhiteListManager(this.getContext());
        Set<String> result = manager.get();
        return null == result ? defaultReturnValue : result;
    }

    private static class SavedState extends BaseSavedState {
        @SuppressWarnings("unused")
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public Set<String> values;
        public Set<String> newValues;
        public boolean preferenceChanged;

        public SavedState(Parcel source) {
            super(source);
            values = readStringSet(source);
            newValues = readStringSet(source);
            preferenceChanged = readBoolean(source);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private static Set<String> readStringSet(Parcel source) {
            final int n = source.readInt();
            final String[] strings = new String[n];
            final Set<String> values = new HashSet<>(n);

            source.readStringArray(strings);

            Collections.addAll(values, strings);

            return values;
        }

        private static void writeStringSet(Parcel dest, Set<String> values) {
            final int n = (values == null) ? 0 : values.size();
            final String[] arrayValues = new String[n];

            if (values != null) {
                values.toArray(arrayValues);
            }

            dest.writeInt(n);
            dest.writeStringArray(arrayValues);
        }

        private static boolean readBoolean(Parcel source) {
            return source.readInt() != 0;
        }

        private static void writeBoolean(Parcel dest, boolean value) {
            dest.writeInt((value) ? 1 : 0);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            writeStringSet(dest, values);
            writeStringSet(dest, newValues);
            writeBoolean(dest, preferenceChanged);
        }
    }
}