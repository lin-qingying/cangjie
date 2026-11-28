// MODULE: friendSource
// FILE: friend.cj
package g4friend

public class FriendReceiver {
    public init() {}
}

extend FriendReceiver {
    public func friendValue(): Int64 {
        17
    }
}

// MODULE: context()(friendSource)
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package g4context

private import g4friend.FriendReceiver

public func useContextExtension(): Int64 {
    ContextReceiver().contextValue()
}

public func useFriendExtension(): Int64 {
    FriendReceiver().friendValue()
}

// FILE: context.cj
package g4context

public class ContextReceiver {
    public init() {}
}

extend ContextReceiver {
    public func contextValue(): Int64 {
        29
    }
}
