package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.util.PrefUtil;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.mikepenz.fastadapter.listeners.ClickEventHook;
import com.mikepenz.fastadapter.select.SelectExtension;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class LocaleItem extends AbstractItem<LocaleItem.ViewHolder> {

    private Locale locale;
    private boolean checked = false;

    public LocaleItem(Locale locale) {
        this.locale = locale;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_two_line_action;
    }

    @NotNull
    @Override
    public ViewHolder getViewHolder(@NotNull View view) {
        return new ViewHolder(view);
    }

    @Override
    public int getType() {
        return R.id.fastadapter_item;
    }

    @Override
    public boolean isSelectable() {
        return true;
    }

    public static class ViewHolder extends FastAdapter.ViewHolder<LocaleItem> {
        @BindView(R.id.checkbox)
        MaterialCheckBox checkbox;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull LocaleItem item, @NotNull List<?> list) {
            final Locale locale = item.getLocale();
            final String lastSpoofLocale = PrefUtil.getString(context, Constants.PREFERENCE_SPOOF_LOCALE);
            line1.setText(locale.getDisplayLanguage());
            line2.setText(locale.getDisplayCountry().isEmpty()
                    ? locale.getDisplayLanguage()
                    : locale.getDisplayCountry());
            checkbox.setChecked(item.isChecked() || lastSpoofLocale.equals(item.getLocale().toString()));
            checkbox.setClickable(false);
        }

        @Override
        public void unbindView(@NotNull LocaleItem item) {
            line1.setText(null);
            line2.setText(null);
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<LocaleItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof ViewHolder
                    ? ((ViewHolder) viewHolder).itemView
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<LocaleItem> fastAdapter, @NotNull LocaleItem item) {
            SelectExtension<LocaleItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
