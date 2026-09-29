package expo.modules.orcaterminalkeycapture

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalKeyDispatchTrackerTest {
  private val tracker = TerminalKeyDispatchTracker()

  @Test
  fun aTakenChordConsumesItsMatchingKeyUp() {
    tracker.onChordTaken(KeyEvent.KEYCODE_C)
    assertTrue(tracker.onKeyUp(KeyEvent.KEYCODE_C))
    // Once released, another key-up for it belongs to the field's own press.
    assertFalse(tracker.onKeyUp(KeyEvent.KEYCODE_C))
  }

  @Test
  fun repeatsOfATakenKeyAreConsumedWhilePlainPressesPass() {
    assertFalse(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_C, repeatCount = 1))
    tracker.onChordTaken(KeyEvent.KEYCODE_C)
    assertFalse(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_C, repeatCount = 0))
    assertTrue(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_C, repeatCount = 3))
    // A different key's repeats are its own.
    assertFalse(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_D, repeatCount = 3))
  }

  @Test
  fun repeatsStopBeingConsumedAfterTheKeyUp() {
    tracker.onChordTaken(KeyEvent.KEYCODE_C)
    assertTrue(tracker.onKeyUp(KeyEvent.KEYCODE_C))
    assertFalse(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_C, repeatCount = 2))
  }

  @Test
  fun focusLossDropsTakenKeysSoLaterRepeatsPass() {
    tracker.onChordTaken(KeyEvent.KEYCODE_C)
    tracker.onFocusLoss()
    assertFalse(tracker.onKeyUp(KeyEvent.KEYCODE_C))
    assertFalse(tracker.shouldConsumeRepeat(KeyEvent.KEYCODE_C, repeatCount = 2))
  }
}
