// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

@Annotation[target: [GlobalFunction]]
class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
func foo() {}

@Annotation[target: [GlobalVariable]]
class APMLevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APMLevel[11]
const c = 1

@APMLevel[11]
let d = 2

main() {}
