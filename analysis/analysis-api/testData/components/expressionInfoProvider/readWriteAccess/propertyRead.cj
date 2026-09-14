// FILE: propertyRead.cj
// TARGET_NAME: total
package sample.resolver.access

class Account {
    public var total: Int64
    public const init() {
        total = 0
    }
}

func readProperty(): Int64 {
    let account = Account()
    return account.total
}
