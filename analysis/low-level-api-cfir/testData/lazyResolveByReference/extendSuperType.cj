// MODULE: main
// FILE: common.cj
package sample.lowlevel.lazy.ref.extend.common

public class Host {}

// FILE: main.cj
package sample.lowlevel.lazy.ref.extend

import sample.lowlevel.lazy.ref.extend.common.Host

interface Marker {}

// extend 头里的跨包接收者：解析它必须先建立 extend 容器上下文（与 IDE 内置库崩溃同一条路径）。
extend Host <: Mar<caret>ker {
    func describe(): String {
        return "host"
    }
}