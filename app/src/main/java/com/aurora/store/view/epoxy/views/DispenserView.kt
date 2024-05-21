package com.aurora.store.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.aurora.store.R
import com.aurora.store.databinding.ViewDispenserBinding

@ModelView(
    autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT,
    baseModelClass = BaseView::class
)
class DispenserView : RelativeLayout {

    private lateinit var binding: ViewDispenserBinding

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        val view = inflate(context, R.layout.view_dispenser, this)
        binding = ViewDispenserBinding.bind(view)
    }

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
