// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    let arr1 = <!NO_VALUE_FOR_PARAMETER!>Array<Int64>() {i => i}<!>

    let arr2 = Array<Int64>(1) {<!UNRESOLVED_REFERENCE!>i<!>}

    let arr3 = <!TOO_MANY_ARGUMENTS!>Array<Int64>(1, 2) {i => i}<!>
    return 0
}
