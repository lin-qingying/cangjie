// LANGUAGE_VERSION: 1.1.0
// MODULE: isolated
// FILE: isolated.cj
package cjmp_graph_isolated
public func isolatedValue(): Int64 { 0 }

// MODULE: common
// FILE: common.cj
package cjmp_graph_common
public func baseValue(): Int64 { 1 }

// MODULE: bridge()()(common)
// FILE: bridge.cj
package cjmp_graph_bridge
public func bridgeValue(): Int64 { 2 }

// MODULE: specific()()(bridge)
// MAIN_MODULE
// FILE: specific.cj
package cjmp_graph_specific
public func specificValue(): Int64 { 3 }
