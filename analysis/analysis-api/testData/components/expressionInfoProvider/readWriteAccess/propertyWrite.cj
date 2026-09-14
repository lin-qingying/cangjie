// FILE: propertyWrite.cj
// TARGET_NAME: total
package sample.resolver.access

class Account {
    public var total: Int64
    public const init() {
        total = 0
    }
}

func writeProperty() {
    let account = Account()
    account.total = 5
}
