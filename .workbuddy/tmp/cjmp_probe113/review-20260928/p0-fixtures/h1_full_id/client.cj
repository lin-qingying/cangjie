package p0_h1_full_id_client

import p0_h1_full_id.*

public func use(value: String, objectValue: Object): String {
    let envelope = Envelope<String>()
    retainObject(objectValue)
    envelope.echo(retainString(value))
}
