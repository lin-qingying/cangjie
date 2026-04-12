// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

func computePosition(h: Int64) {
    h
}

var bits = Array<Int64>(3) {i => i}

func set(value: Int64) {
    let h = value
    let idx = 0
    if (idx >= bits.size) {
        false
    } else if (let i <- computePosition(h) && let r <- ((bits[idx] & i) != 0) && r) {
        print(i)
        true
    } else if (let i <- computePosition(h) && i != 0 && let r <- ((bits[idx] & i) != 0) && r) {
        print(i)
        false
    } else {
        false
    }
}

main() {
}
