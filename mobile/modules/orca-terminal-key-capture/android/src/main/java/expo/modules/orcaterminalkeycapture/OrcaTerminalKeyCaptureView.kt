package expo.modules.orcaterminalkeycapture

import android.content.Context
import android.view.KeyCharacterMap
import android.view.KeyEvent
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.viewevent.EventDispatcher
import expo.modules.kotlin.views.ExpoView

/**
 * Wraps the terminal's hidden TextInput on the focus path. React Native's TextInput reports no key
 * event with modifiers, so a Ctrl chord that an IME sends through `InputConnection.sendKeyEvent`
 * (Unexpected Keyboard does) or that a hardware keyboard sends would reach TextView, which drops
 * it or runs a copy/paste shortcut. Key dispatch reaches this view before TextView and before
 * shortcut dispatch.
 */
class OrcaTerminalKeyCaptureView(context: Context, appContext: AppContext) :
  ExpoView(context, appContext) {
  private val onTerminalKey by EventDispatcher<Map<String, Any>>()

  // Why: a taken chord's key-up must not reach the field either; ReactEditText hides the keyboard
  // on Enter's key-up, and a hardware Escape key-up would fall back to Back.
  private val takenKeyCodes = mutableSetOf<Int>()

  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    when (event.action) {
      KeyEvent.ACTION_DOWN -> {
        val chord = readChord(event)
        if (chord != null) {
          takenKeyCodes.add(event.keyCode)
          onTerminalKey(chord.toPayload())
          return true
        }
      }
      KeyEvent.ACTION_UP -> if (takenKeyCodes.remove(event.keyCode)) return true
    }
    return super.dispatchKeyEvent(event)
  }

  private fun readChord(event: KeyEvent): TerminalKeyChord? {
    if (KeyEvent.isModifierKey(event.keyCode)) {
      return null
    }
    val chordModifiers = KeyEvent.META_CTRL_MASK or KeyEvent.META_ALT_MASK or KeyEvent.META_META_MASK
    val altGr = (event.metaState and KeyEvent.META_ALT_RIGHT_ON) != 0
    return TerminalKeyChord.from(
      keyCode = event.keyCode,
      baseChar = event.getUnicodeChar(event.metaState and chordModifiers.inv()) and
        KeyCharacterMap.COMBINING_ACCENT_MASK,
      altGrChar = if (altGr) {
        event.getUnicodeChar(event.metaState) and KeyCharacterMap.COMBINING_ACCENT_MASK
      } else {
        0
      },
      ctrl = event.isCtrlPressed,
      alt = event.isAltPressed,
      shift = event.isShiftPressed,
      meta = event.isMetaPressed
    )
  }
}
