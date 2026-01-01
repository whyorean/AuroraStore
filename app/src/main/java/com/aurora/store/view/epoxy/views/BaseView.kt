package com.aurora.store.view.epoxy.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

abstract class BaseView<ViewBindingType : ViewBinding> : RelativeLayout {

    private lateinit var _binding: ViewBindingType
    protected val binding get() = _binding

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
        if (context != null) {
            _binding = inflateViewBinding(LayoutInflater.from(context))
            addView(_binding.root)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun inflateViewBinding(inflater: LayoutInflater): ViewBindingType {
        val type = (javaClass.genericSuperclass as ParameterizedType)
            .actualTypeArguments[0] as Class<ViewBindingType>
        val method = type.getMethod(
            "inflate",
            LayoutInflater::class.java,
            ViewGroup::class.java,
            Boolean::class.java
        )
        return method.invoke(null, inflater, this, false) as ViewBindingType
    }
}
