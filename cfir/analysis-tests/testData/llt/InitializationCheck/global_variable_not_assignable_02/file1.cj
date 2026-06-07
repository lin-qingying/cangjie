// FILE: file2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    static let a = 1
    static const b = 1
}

// FILE: file1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A.a++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A.b<!> += 1
}
