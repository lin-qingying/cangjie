// RUN_PIPELINE_TILL: FRONTEND
@Annotation
public class <!ANNOTATION_NO_CONST_INIT!>BadAnnotation<!> {
    init(value: String) {
        let _ = value
    }
}

@Annotation
public class GoodAnnotation {
    const init(value: String) {
        let _ = value
    }
}
