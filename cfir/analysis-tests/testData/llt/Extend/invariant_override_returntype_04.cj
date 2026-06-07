// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


public interface Cloneable {
    func clone(): Cloneable
}

public interface LiteralValue <: Cloneable {
    func <!OVERRIDING_RETURN_TYPE_MISMATCH!>clone<!>(): LiteralValue
}

extend String <: LiteralValue {}

main() {}
