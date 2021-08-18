package com.herman.castreceiver.base

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import com.herman.castreceiver.R
import kotlinx.android.synthetic.main.fragment_loading.*

open class LoadingFragment : BaseFragment() {

  private var mMessage: String? = null

  companion object {
    fun newInstance(): LoadingFragment {
      val args = Bundle()
      val fragment = LoadingFragment()
      fragment.arguments = args
      return fragment
    }
  }

  override val layoutResId = R.layout.fragment_loading

  override fun onLayoutInflated(savedInstanceState: Bundle?) {
    val animation = AnimationUtils.loadAnimation(requireContext(), R.anim.progressbar_waiting)
    animation.interpolator = LinearInterpolator()
    imgLoading.setAnimation(animation)
    if (TextUtils.isEmpty(mMessage)) {
      tvLoading.setVisibility(View.GONE)
    } else {
      tvLoading.setText(mMessage)
      tvLoading.setVisibility(View.VISIBLE)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
  }

  fun setLoadingText(text: String?) {
    mMessage = text
    if (tvLoading != null) {
      tvLoading.setText(text)
      tvLoading.setVisibility(View.VISIBLE)
    }
  }
}