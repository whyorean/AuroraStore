/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.extensions

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.bumptech.glide.request.transition.Transition
import java.io.File

fun ImageView.load(
    bitmap: Bitmap?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(bitmap, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    byteArray: ByteArray?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(byteArray, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    drawable: Drawable?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(drawable, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    @RawRes @DrawableRes resourceId: Int?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(resourceId, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    uri: Uri?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(uri, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    string: String?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(string, transitionOptions, requestOptions)

@JvmSynthetic
fun ImageView.load(
    file: File?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> = loadAny(file, transitionOptions, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    bitmap: Bitmap?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(bitmap, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    byteArray: ByteArray?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(byteArray, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    drawable: Drawable?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(drawable, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    @RawRes @DrawableRes resourceId: Int?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(resourceId, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    uri: Uri?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(uri, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    string: String?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(string, requestOptions)

@JvmSynthetic
inline fun ImageView.load(
    file: File?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> = loadAny(file, requestOptions)

@JvmSynthetic
fun ImageView.loadAny(
    data: Any?,
    transitionOptions: TransitionOptions<*, Drawable>? = null,
    requestOptions: RequestOptions? = null
): CustomViewTarget<ImageView, Drawable> {
    return Glide.with(this)
        .asDrawable()
        .load(data)
        .apply {
            transitionOptions?.let { transition(it) }
            requestOptions?.let { apply(it) }
        }
        .into(object : CustomViewTarget<ImageView, Drawable>(this) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
            }

            override fun onResourceCleared(placeholder: Drawable?) {
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                this.view.setImageDrawable(resource)
            }
        })
}

@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    requestOptions: RequestOptions.() -> Unit
): CustomViewTarget<ImageView, Drawable> {
    val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
    return Glide.with(this)
        .asDrawable()
        .load(data)
        .transition(withCrossFade(factory))
        .apply(RequestOptions().apply(requestOptions))
        .into(object : CustomViewTarget<ImageView, Drawable>(this) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
            }

            override fun onResourceCleared(placeholder: Drawable?) {
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                this.view.setImageDrawable(resource)
            }
        })}

@JvmSynthetic
fun ImageView.clear() {
    //Glide.with(this).clear(this)
}
