// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func test() {
    var a: String
    var b: Int64
    var c: String
    var d: Int64
    var f = {=> ("abce", (2, 3.0))}
    <!TYPE_MISMATCH!>(a, (b, c)) = "TEST"<!>
    <!TYPE_MISMATCH!>(a, (b, c)) = f()<!>
    <!TYPE_MISMATCH!>(a, (b, c), d) = f()<!>
    <!TYPE_MISMATCH!>(a, (b, c, d)) = f()<!>
    <!TYPE_MISMATCH!>(a, b) = f()<!>
}

main() {
}
