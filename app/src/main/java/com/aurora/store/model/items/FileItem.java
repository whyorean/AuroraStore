package com.aurora.store.model.items;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aurora.store.R;
import com.aurora.store.util.Util;
import com.dragons.aurora.playstoreapiv2.FileMetadata;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import lombok.Getter;
import lombok.Setter;

public class FileItem extends AbstractItem<FileItem.ViewHolder> {

    @Getter
    @Setter
    private FileMetadata fileMetadata;

    public FileItem(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_three_rows;
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

    public static class ViewHolder extends FastAdapter.ViewHolder<FileItem> {
        @BindView(R.id.img)
        ImageView img;
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
        public void bindView(@NotNull FileItem item, @NotNull List<?> list) {
            final FileMetadata fileMetadata = item.getFileMetadata();
            if (fileMetadata.hasSplitId()) {
                line1.setText(fileMetadata.getSplitId());
                img.setImageDrawable(context.getDrawable(R.drawable.ic_file_patch));
            } else if (fileMetadata.hasFileType()) {
                switch (fileMetadata.getFileType()) {
                    case 0:
                        line1.setText("Base");
                        img.setImageDrawable(context.getDrawable(R.drawable.ic_file_apk));
                        break;
                    case 1:
                        line1.setText("Obb");
                        img.setImageDrawable(context.getDrawable(R.drawable.ic_file_obb));
                        break;
                    default:
                        line1.setText("Patch");
                        img.setImageDrawable(context.getDrawable(R.drawable.ic_file_patch));
                }
            }
            line2.setText(String.valueOf(fileMetadata.getVersionCode()));
            line3.setText(Util.humanReadableByteValue(fileMetadata.getCompressedSize(), true));
        }

        @Override
        public void unbindView(@NotNull FileItem item) {
            img.setImageDrawable(null);
            line1.setText(null);
            line2.setText(null);
            line3.setText(null);
        }
    }
}
