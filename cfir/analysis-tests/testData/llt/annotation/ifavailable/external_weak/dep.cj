package dep

import ohos.labels.APILevel

@!APILevel[since: "19"]
public func func1(a!: Int64 = 1) {println(19)}

@!APILevel[since: "18"]
public func func2() {println(18)}

@!APILevel[since: "19"]
public class A {
	@!APILevel[since: "19"]
	public prop a: Int64 {
		get() {1}
	}
}



// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
