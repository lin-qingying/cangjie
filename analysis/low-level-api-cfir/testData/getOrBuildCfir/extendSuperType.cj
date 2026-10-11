// FILE: common.cj
package lowlevel.getorbuild.extend.common

class Host {}

// FILE: extendSuperType.cj
package lowlevel.getorbuild.extend

import lowlevel.getorbuild.extend.common.Host

interface Marker {}

// getOrBuildCfir 落在 extend 头里的跨包接收者引用上：与 IDE 内置库崩溃同一条路径。
extend <expr>Host</expr> <: Marker {
    func describe(): String {
        return "host"
    }
}