// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


<!INHERITANCE_CYCLE!>i<!>nterface A <: B {}

interface C <: A {}

interface B <: C {}

extend <!ILLEGAL_EXTENDED_TYPE!>A<!> <: C {}

main() {
    0
}
