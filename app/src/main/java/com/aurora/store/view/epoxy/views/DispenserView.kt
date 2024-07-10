package com.aurora.store.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.databinding.ViewDispenserBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseModel::class
)
class DispenserView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseView<ViewDispenserBinding>(context, attrs, defStyleAttr) {

    @ModelProp
    fun url(url: String) {
        binding.url.text = url
    }

    @CallbackProp
    fun copy(onClickListener: OnClickListener?) {
        binding.url.setOnClickListener(onClickListener)
    }

    @CallbackProp
    fun clear(onClickListener: OnClickListener?) {
        binding.btnAction.setOnClickListener(onClickListener)
    }
}
