package com.herman.castreceiver.base

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class BaseActivity : AppCompatActivity() {
  companion object {
    private const val TAG = "BaseActivity"
  }

  override fun finish() {
    if (!isFinishing) {
      Log.d(TAG, "finish: " + javaClass.name)
      super.finish()
    }
  }

  protected fun showToast(msg: String?) {
    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
  }

  protected fun showToast(msg: Int) {
    Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
  }

  protected fun setFragment(contentId: Int, fragment: Fragment?) {
    if (!isFinishing) {
      val manager = supportFragmentManager
      val transaction = manager.beginTransaction()
      transaction.replace(contentId, fragment!!)
      transaction.commitAllowingStateLoss()
    }
  }

  protected fun removeFragment(fragment: Fragment?) {
    if (!isFinishing) {
      val manager = supportFragmentManager
      val transaction = manager.beginTransaction()
      transaction.remove(fragment!!)
      transaction.commitAllowingStateLoss()
    }
  }
}