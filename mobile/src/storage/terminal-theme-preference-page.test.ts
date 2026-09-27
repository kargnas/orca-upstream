import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

// The session page's AsyncStorage, as the bundler aliases it over the real module.
vi.mock('@react-native-async-storage/async-storage', async () => ({
  default: (await import('../mobile-web-shell/bridge/page-async-storage')).default
}))

const HOST_ID = 'host-1'
const SESSION_ROUTE = '/h/host-1/session/wt-1'
const KEY = 'orca:terminalThemeMode'

const posted: { key: string; value: string | null }[] = []

/** Seats a fresh page as the shell would: `init` carries only the route's allowlisted keys. */
async function openPage(held: Record<string, string>) {
  const { publishPageStorage } = await import('../mobile-web-shell/bridge/page-async-storage')
  const { pageStorageKeysForRoute } = await import('../mobile-web-shell/page-storage-keys')
  const allowed = new Set(pageStorageKeysForRoute(HOST_ID, SESSION_ROUTE))
  posted.length = 0
  publishPageStorage(
    Object.fromEntries(Object.entries(held).filter(([key]) => allowed.has(key))),
    (key, value) => {
      posted.push({ key, value })
      return true
    },
    HOST_ID,
    SESSION_ROUTE
  )
  return import('./terminal-theme-preference')
}

describe('terminal appearance inside the hybrid-shell session page', () => {
  let warn: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    vi.resetModules()
    warn = vi.spyOn(console, 'warn').mockImplementation(() => {})
  })

  afterEach(() => {
    warn.mockRestore()
  })

  it('follows the phone when the page is not handed the saved appearance', async () => {
    const preference = await openPage({ [KEY]: 'light' })
    expect(await preference.loadMobileTerminalThemeMode()).toBe('system')
  })

  it('applies a choice for the page lifetime and drops the write without rejecting', async () => {
    const preference = await openPage({})
    await preference.loadMobileTerminalThemeMode()
    await expect(preference.saveMobileTerminalThemeMode('light')).resolves.toBeUndefined()
    expect(preference.getMobileTerminalThemeMode()).toBe('light')
    expect(posted).toEqual([])
    expect(warn).toHaveBeenCalledWith('[page-bridge] storage-write-dropped', {
      key: KEY,
      refusal: 'not-allowed'
    })
  })
})
