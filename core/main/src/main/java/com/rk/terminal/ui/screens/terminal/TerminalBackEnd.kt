package com.rk.terminal.ui.screens.terminal

import android.content.res.Configuration
import android.content.res.Resources
import android.media.MediaPlayer
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import com.blankj.utilcode.util.ClipboardUtils
import com.rk.libcommons.child
import com.rk.settings.Settings
import com.rk.terminal.ui.activities.terminal.MainActivity
import com.rk.terminal.ui.screens.terminal.virtualkeys.SpecialButton
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class TerminalBackEnd(
    private val terminal: TerminalView,
    private val activity: MainActivity
) : TerminalViewClient() {
    private val terminalViewModel = ViewModelProvider(activity)[TerminalViewModel::class.java]

    override fun onSingleTapUp(e: MotionEvent) {
        if (isHardwareKeyboardConnected && Settings.hide_soft_keyboard_if_hwd) return

        val imm = activity.getSystemService(InputMethodManager::class.java)
        val isKeyboardAlreadyVisible = imm?.isActive(terminal) == true

        if (!isKeyboardAlreadyVisible) {
            showSoftInput()
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = Settings.input_mode != 1
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = true
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean {
        if (KeyShortcutHandler.handle(keyCode, e, activity)) return true

        if (keyCode == KeyEvent.KEYCODE_ENTER && !session.isRunning) {
            val binder = activity.viewModel.sessionBinder ?: return false
            val service = binder.getService()
            val currentId = service.currentSession.value.first

            binder.terminateSession(currentId)

            if (service.sessionList.isEmpty()) {
                activity.finish()
            } else {
                terminalViewModel.changeSession(activity, binder, service.sessionList.keys.first())
            }
            return true
        }
        return false
    }

    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean =
        terminalViewModel.virtualKeysView?.readSpecialButton(SpecialButton.CTRL, true) == true

    override fun readAltKey(): Boolean =
        terminalViewModel.virtualKeysView?.readSpecialButton(SpecialButton.ALT, true) == true

    override fun readShiftKey(): Boolean =
        terminalViewModel.virtualKeysView?.readSpecialButton(SpecialButton.SHIFT, true) == true

    override fun readFnKey(): Boolean =
        terminalViewModel.virtualKeysView?.readSpecialButton(SpecialButton.FN, true) == true

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false

    override fun onEmulatorSet() {
        if (terminal.mEmulator != null) {
            terminal.setTerminalCursorBlinkerState(true, true)
        }
    }

    private fun showSoftInput() {
        terminal.requestFocus()
        val imm = activity.getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(terminal, InputMethodManager.SHOW_IMPLICIT)
    }
}
