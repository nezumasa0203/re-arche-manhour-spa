/**
 * CZ 独自バイト長計算
 *
 * 現行 ApCheck.js の getByteLength() を移植。
 *   半角 (U+0000〜U+007F): 1バイト
 *   半角カタカナ (U+FF61〜U+FF9F): 2バイト
 *   全角 (U+0080以上、半角カタカナ除く): 2バイト
 */
export function calculateByteLength(str: string): number {
  let length = 0
  for (const char of str) {
    const code = char.codePointAt(0)!
    if (code <= 0x7f) {
      length += 1
    } else {
      length += 2
    }
  }
  return length
}
