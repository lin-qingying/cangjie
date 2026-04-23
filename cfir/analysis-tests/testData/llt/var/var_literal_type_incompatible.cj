// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    let a: Int64 = <!TYPE_MISMATCH!>1.2<!>
    let b: Int64 = <!TYPE_MISMATCH!>r'c'<!>
    let c: Int64 = <!TYPE_MISMATCH!>true<!>
    let d: Bool = <!TYPE_MISMATCH!>1<!>
    let e: Float64 = <!TYPE_MISMATCH!>2<!>
    let f: Rune = <!TYPE_MISMATCH!>3<!>
    return 1
}
