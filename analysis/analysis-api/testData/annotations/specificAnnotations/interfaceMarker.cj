// FILE: interfaceMarker.cj
// CLASS_ID: sample.annotations/Marker
package sample.annotations

@Annotation
public class Marker {
    public const init() {}
}

@Marker
interface <caret>Runnable {
}
