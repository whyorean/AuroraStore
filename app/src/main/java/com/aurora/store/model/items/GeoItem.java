package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class GeoItem extends AbstractItem<GeoItem.ViewHolder> {

    private String location;
    private boolean checked = false;

    public GeoItem(String location) {
        this.location = location;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<GeoItem> {
        @BindView(R.id.checkbox)
        MaterialCheckBox checkbox;
        @BindView(R.id.img)
        ImageView img;
        @BindView(R.id.line1)
        TextView line1;
        @BindView(R.id.line2)
        TextView line2;

        private Context context;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            context = itemView.getContext();
        }

        @Override
        public void bindView(@NotNull GeoItem item, @NotNull List<?> list) {
            final String[] locationData = item.getLocation().split(",");
            final String lastSpoofLocation = PrefUtil.getString(context, Constants.PREFERENCE_SPOOF_GEOLOCATION);
            img.setImageDrawable(context.getDrawable(R.drawable.ic_map_marker));
            line1.setText(locationData[0]);
            line2.setText(locationData[1]);
            checkbox.setChecked(item.isChecked() || lastSpoofLocation.equals(item.getLocation()));
            checkbox.setClickable(false);
        }

        @Override
        public void unbindView(@NotNull GeoItem item) {
            line1.setText(null);
            line2.setText(null);
        }
    }

    public static final class CheckBoxClickEvent extends ClickEventHook<GeoItem> {
        @Nullable
        public View onBind(@NotNull RecyclerView.ViewHolder viewHolder) {
            return viewHolder instanceof ViewHolder
                    ? ((ViewHolder) viewHolder).itemView
                    : null;
        }

        @Override
        public void onClick(@NotNull View view, int position, @NotNull FastAdapter<GeoItem> fastAdapter, @NotNull GeoItem item) {
            SelectExtension<GeoItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
            if (selectExtension != null) {
                selectExtension.toggleSelection(position);
                item.checked = !item.checked;
            }
        }
    }
}
