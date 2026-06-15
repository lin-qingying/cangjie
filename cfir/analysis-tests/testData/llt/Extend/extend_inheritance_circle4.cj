// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class D {}

interface A <: B {}

interface C <: A {}

interface B <: C {}

<!INHERITANCE_CYCLE!>e<!>xtend D <: C {}

main() {
    0
}
