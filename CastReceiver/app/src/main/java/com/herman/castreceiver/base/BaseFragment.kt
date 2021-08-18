package com.herman.castreceiver.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {
  protected var mIsVisible = false
  protected var mIsVisibled = false
  protected var mIsPrepared = false

  companion object {
    private const val TAG = "BaseFragment"
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    Log.i(TAG, "onCreateView: $javaClass")
    return inflater.inflate(layoutResId, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    onLayoutInflated(savedInstanceState)
    Log.i(TAG, "onLayoutInflated: $javaClass")
    mIsPrepared = true
    if (mIsVisible) {
      mIsVisibled = true
      onVisible()
    }
  }

  override fun setUserVisibleHint(isVisibleToUser: Boolean) {
    super.setUserVisibleHint(isVisibleToUser)
    Log.i(TAG, "setUserVisibleHint: isVisibleToUser=$isVisibleToUser, $javaClass")
    mIsVisible = isVisibleToUser
    if (mIsPrepared) {
      if (isVisibleToUser && !mIsVisibled) {
        mIsVisibled = true
        onVisible()
      } else {
        mIsVisibled = false
        onInVisible()
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    Log.i(TAG, "onAttach: $javaClass")
  }

  override fun onDetach() {
    super.onDetach()
    Log.i(TAG, "onDetach: $javaClass")
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    Log.i(TAG, "onHiddenChanged: hidden=$hidden, $javaClass")
  }

  override fun onResume() {
    super.onResume()
    if (mIsVisible && mIsPrepared && !mIsVisibled) {
      mIsVisibled = true
      onVisible()
    }
    Log.i(TAG, "onResume: $javaClass")
  }

  override fun onPause() {
    super.onPause()
    if (mIsVisible && mIsPrepared) {
      mIsVisibled = false
      onInVisible()
    }
    Log.i(TAG, "onPause: $javaClass")
  }

  fun setFocus(focus: Boolean) {
    if (focus) {
      onFocused()
    } else {
      if (view != null) {
        requireView().clearFocus()
      }
    }
  }

  protected fun onVisible() {
    Log.i(TAG, "onVisible: $javaClass")
  }

  protected fun onInVisible() {
    Log.i(TAG, "onInVisible: $javaClass")
  }

  protected fun onFocused() {}
  protected abstract val layoutResId: Int
  protected abstract fun onLayoutInflated(savedInstanceState: Bundle?)

  protected fun startActivity(cls: Class<out Activity?>?) {
    startActivity(Intent(requireContext(), cls))
  }

  protected fun showToast(msg: String?) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
  }
}