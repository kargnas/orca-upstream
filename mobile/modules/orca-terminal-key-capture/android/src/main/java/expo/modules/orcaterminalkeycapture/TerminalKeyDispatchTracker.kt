package expo.modules.orcaterminalkeycapture

/**
 * Tracks which key codes the capture view consumed on their way down. A key that was taken must
 * keep being consumed — its repeats and its key-up — or the field below sees a press it never
 * took: `ReactEditText` hides the keyboard on Enter's key-up, and a hardware Escape key-up would
 * fall back to Back. A focus loss between down and up drops the record, or the first plain
 * repeat after focus returns would be swallowed with no matching down.
 */
internal class TerminalKeyDispatchTracker {
  private val takenKeyCodes = mutableSetOf<Int>()

  fun onChordTaken(keyCode: Int) {
    takenKeyCodes.add(keyCode)
  }

  /**
   * A repeat ACTION_DOWN whose original press was taken as a chord must not reach the field:
   * Android keeps the original key-down's modifiers out of repeats once the chord no longer
   * matches, so the field would see a key-down whose key-up is consumed here.
   */
  fun shouldConsumeRepeat(keyCode: Int, repeatCount: Int): Boolean =
    repeatCount > 0 && keyCode in takenKeyCodes

  /** True when this key-up pairs with a taken key-down and must be swallowed with it. */
  fun onKeyUp(keyCode: Int): Boolean = takenKeyCodes.remove(keyCode)

  fun onFocusLoss() {
    takenKeyCodes.clear()
  }
}
