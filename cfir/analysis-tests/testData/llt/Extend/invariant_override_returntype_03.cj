// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


public interface I {
    func <!RETURN_TYPE_INVARIANCE!>append<!>(_: String): Any
}

extend StringBuilder <: I {}

main() {}
