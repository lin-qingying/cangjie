// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-0: %compiler %cmp_opt %f -o %output | compare %f
// ASSERT: scan-not Semantic check should ensure only one candidate will be chosen.

//    Symbol
//      /\
//     B  H
//    /\   \
//   A  E   G
//  /\     /
// C  F   D
// src classdecl order: H A C D E B F G
// dst classdecl order: H G D, B A C E F

public interface Symbol <: Equatable<Symbol> {
    operator override func ==(that: Symbol): Bool {
        return true
    }

    operator override func !=(that: Symbol): Bool {
        return !(this == that)
    }
}

public abstract class H <: Symbol {}

public interface MemberSymbol <: Symbol {}

public abstract class A <: B & MemberSymbol {}

public open class C <: A {}

public open class D <: G & MemberSymbol {}

public open class E <: B {}

public abstract class B <: Symbol {}

public open class F <: A {}

public open class G <: H {}

main() {
    0
}
