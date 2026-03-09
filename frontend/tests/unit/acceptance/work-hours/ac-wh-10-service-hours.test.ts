/**
 * AC-WH-10: サービス時間外操作
 * Given: サービス時間外（23:31 JST）を模擬
 * When: 操作を試みる
 * Then: CZ-102 フルスクリーンオーバーレイ表示、全操作ブロック
 */
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import ServiceTimeOverlay from '~/components/common/ServiceTimeOverlay.vue'

function mountOverlay(props: Record<string, unknown> = {}) {
  return mount(ServiceTimeOverlay, {
    props: {
      visible: true,
      ...props,
    },
  })
}

describe('AC-WH-10: サービス時間外操作', () => {
  it('Given: visible=true → Then: オーバーレイが表示される', () => {
    const wrapper = mountOverlay({ visible: true })
    expect(wrapper.find('.service-time-overlay').exists()).toBe(true)
  })

  it('Given: visible=false → Then: オーバーレイが非表示', () => {
    const wrapper = mountOverlay({ visible: false })
    expect(wrapper.find('.service-time-overlay').exists()).toBe(false)
  })

  it('Then: サービス時間外メッセージが含まれる', () => {
    const wrapper = mountOverlay({ visible: true })
    expect(wrapper.text()).toContain('サービス提供時間外')
    expect(wrapper.text()).toContain('6:00')
    expect(wrapper.text()).toContain('23:30')
  })

  it('Then: カスタムメッセージを表示できる', () => {
    const wrapper = mountOverlay({ visible: true, message: 'CZ-102: テスト' })
    expect(wrapper.text()).toContain('CZ-102: テスト')
  })
})
