// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func test(b: Bool) {
    if (b) {
        0
    } else {
        1
    }
}

main() {
    let r1 = r'🥝'
    let r2 = r"🍉"
    let r3: Rune = '🍑'
    let r4: Rune = "🍒"
    let r5: Rune
    r5 = '🍅'
    let r6: Rune
    r6 = "🥕"
    let r7: Rune = '''
🥥'''
    let r8: Rune = """
🌰"""

    test(r1 is Rune && r2 is Rune && r3 is Rune && r4 is Rune && r5 is Rune && r6 is Rune && r7 is Rune && r8 is Rune)
}
