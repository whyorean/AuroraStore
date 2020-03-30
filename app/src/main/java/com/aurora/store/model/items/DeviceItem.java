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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Properties;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DeviceItem extends AbstractItem<DeviceItem.ViewHolder> {

    private Properties properties;
    private boolean checked = false;

    public DeviceItem(Properties properties) {
        this.properties = properties;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_three_line_action;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<DeviceItem> {
        @BindView(R.id.checkbox)
        MaterialCheckBox checkbox;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;
        @BindView(R.id.line3)
        TextView line3;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull DeviceItem item, @NotNull List<?> list) {
            final Properties properties = item.getProperties();
            final String lastSpoofDevice = PrefUtil.getString(context, Constants.PREFERENCE_SPOOF_DEVICE);
            line1.setText(properties.getProperty("Build.MODEL"));
            line2.setText(StringUtils.joinWith(" \u2022 ",
                    properties.getProperty("Build.MANUFACTURER"),
                    "API " + properties.getProperty("Build.VERSION.SDK_INT")));
            line3.setText(properties.getProperty("Platforms"));
            checkbox.setChecked(item.isChecked() || lastSpoofDevice.equals(item.properties.getProperty("CONFIG_NAME")));
            checkbox.setClickable(false);
        }

        @Override
        public void unbindView(@NotNull DeviceItem item) {
            line1.setText(null);
            line2.setText(null);
            line3.setText(null);
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<DeviceItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof ViewHolder
                    ? ((ViewHolder) viewHolder).itemView
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<DeviceItem> fastAdapter, @NotNull DeviceItem item) {
            SelectExtension<DeviceItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
