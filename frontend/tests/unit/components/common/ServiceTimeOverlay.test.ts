import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { ref } from 'vue'
import ServiceTimeOverlay from '~/components/common/ServiceTimeOverlay.vue'

describe('ServiceTimeOverlay', () => {
  // ─── 表示制御 ──────────────────────────────────────────

  it('visible=false → オーバーレイは非表示', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: { visible: false },
    })

    expect(wrapper.find('.service-time-overlay').exists()).toBe(false)
  })

  it('visible=true → オーバーレイが表示される', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: { visible: true },
    })

    expect(wrapper.find('.service-time-overlay').exists()).toBe(true)
  })

  // ─── メッセージ表示 ───────────────────────────────────

  it('CZ-102 のデフォルトメッセージが表示される', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: { visible: true },
    })

    expect(wrapper.text()).toContain('サービス提供時間外')
  })

  it('カスタムメッセージを表示できる', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: {
        visible: true,
        message: 'カスタムメッセージ',
      },
    })

    expect(wrapper.text()).toContain('カスタムメッセージ')
  })

  // ─── 操作不可状態 ─────────────────────────────────────

  it('オーバーレイは画面全体を覆う', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: { visible: true },
    })

    const overlay = wrapper.find('.service-time-overlay')
    expect(overlay.exists()).toBe(true)
  })

  // ─── サービス時間帯表示 ──────────────────────────────

  it('サービス提供時間帯が表示される', () => {
    const wrapper = mount(ServiceTimeOverlay, {
      props: { visible: true },
    })

    expect(wrapper.text()).toContain('6:00')
    expect(wrapper.text()).toContain('23:30')
  })
})
