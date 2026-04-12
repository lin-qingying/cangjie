// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler  --error-count-limit=all  %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

func mm() {
    this()
    this
}

class m {
    public var a = this
    public var b = this()
    public var c : m = this()
    public var f : Int64 = this
    public static var d = this()
    public static var e : Int64 = this()
    public init() {
        super() // ok
    }
    public init(a:Int64) {
        this
    }
    public init(a:Int64, b: Int64) {
        this().a
        this().c
    }
    public init(a:Int64, b: Int64, c: Int64) {
        this().c++
    }
    public init(a:Int64, b: Int64, c: Int64, d: Int64) {
        if (a == 1) {
            this()
        }
        match (b) {
            case _ => this()
        }
        while(c == 1) {
            this()
        }
    }
    public func fff() {
        init().c++ // ASSERT: regex 'init' is not allowed in block, expected expression or declaration here
    }
    public static func ff() {
        init().c++ // ASSERT: regex 'init' is not allowed in block, expected expression or declaration here
    }
}

struct k {
    public var a = this
    public var b = this()
    public var c : m = this()
    public var f : Int64 = this
    public static var d = this()
    public static var e : Int64 = this()
    public init() {

    }
    public init(a:Int64) {
        this
    }
    public init(a:Int64, b: Int64) {
        this().a
        this().c
    }
    public init(a:Int64, b: Int64, c: Int64) {
        init().c++ // ASSERT: regex 'init' is not allowed in block, expected expression or declaration here
    }
    public func fff() {
        init // ASSERT: regex 'init' is not allowed in block, expected expression or declaration here
        init() // ASSERT: regex 'init' is not allowed in block, expected expression or declaration here
        init().c
        init().c++
    }
    public static func ff() {
        init
        init()
        init().c
        init().c++
    }
}
