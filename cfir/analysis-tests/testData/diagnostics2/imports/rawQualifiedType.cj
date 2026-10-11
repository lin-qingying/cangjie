// RUN_PIPELINE_TILL: FRONTEND
// FILE: socket.cj
package std.net

public struct CfirRawQualifiedSockAddr {}

// FILE: typeUsages.cj
package raw.qualified

public struct Wrapper<T> {}

public interface SockAddrTypeUses {
    func returnSockAddr(): <!CANNOT_REF_TO_PKG_NAME!>std.net.CfirRawQualifiedSockAddr<!>
    func acceptSockAddr(value: <!CANNOT_REF_TO_PKG_NAME!>std.net.CfirRawQualifiedSockAddr<!>): Unit
    func nestedSockAddr(): Wrapper<<!CANNOT_REF_TO_PKG_NAME!>std.net.CfirRawQualifiedSockAddr<!>>
}
