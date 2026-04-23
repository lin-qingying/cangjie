// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class C {
    let <!UNABLE_TO_INFER_DECL!>v<!> = f
}

func f() {
    let c = C()
    c.v
}

main() {}
