// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj ./testb.cj
import a.*
import b.*

main() {
    A().f1
    A().<!NOT_MEMBER_OF!>f2<!> // error
    A().f3
}
