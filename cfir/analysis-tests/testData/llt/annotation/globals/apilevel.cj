// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package a

@Annotation
public class APILevel {
    public let level: UInt8
    public let atomicservice: Bool
    public let stagemodeonly: Bool
    public let syscap: String

    public const init(l: UInt8, atomicservice!: Bool = false, stagemodeonly!: Bool = false, syscap!: String = "") {
        level = l
        this.stagemodeonly = stagemodeonly
        this.syscap = syscap
        this.atomicservice = atomicservice
    }
}
