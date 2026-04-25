package com.pokemontcg.tracker.ui.components

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.pokemontcg.tracker.R

private const val ASSET_URI_PREFIX = "file:///android_asset/"

fun assetImageUri(assetPath: String): String {
    return if (assetPath.startsWith(ASSET_URI_PREFIX)) {
        assetPath
    } else {
        "$ASSET_URI_PREFIX${Uri.encode(assetPath, "/")}"
    }
}

fun ImageView.loadCardAsset(assetPath: String?) {
    if (assetPath.isNullOrBlank()) {
        setImageResource(R.drawable.bg_card_image_placeholder)
        return
    }

    Glide.with(this)
        .load(assetImageUri(assetPath))
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.bg_card_image_placeholder)
        .error(R.drawable.bg_card_image_placeholder)
        .into(this)
}
