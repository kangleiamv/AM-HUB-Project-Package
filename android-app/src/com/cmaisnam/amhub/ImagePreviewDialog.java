package com.cmaisnam.amhub;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImagePreviewDialog {

    public static void show(Context context, Bitmap bitmap, String fileName, Runnable onDownload) {
        if (bitmap == null) return;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_image_preview);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        TextView titleView = dialog.findViewById(R.id.dialog_img_title);
        ImageView imgView = dialog.findViewById(R.id.dialog_img_view);
        TextView btnClose = dialog.findViewById(R.id.dialog_btn_close);
        Button btnDownload = dialog.findViewById(R.id.dialog_btn_download);

        titleView.setText(fileName != null ? fileName : "Image Preview");
        imgView.setImageBitmap(bitmap);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnDownload.setOnClickListener(v -> {
            if (onDownload != null) {
                onDownload.run();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
