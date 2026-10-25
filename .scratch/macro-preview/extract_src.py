import sys, zipfile, os

jar = sys.argv[1]
out = sys.argv[2]
os.makedirs(out, exist_ok=True)
want = [
    'com/intellij/ui/components/ActionLink.java',
    'com/intellij/ui/components/labels/LinkLabel.java',
    'com/intellij/ui/SeparatorComponent.java',
    'com/intellij/ui/OnePixelDivider.java',
    'com/intellij/ui/components/breadcrumbs/Crumb.java',
    'com/intellij/ui/components/breadcrumbs/BreadcrumbsComponent.java',
    'com/intellij/ui/JBColor.java',
    'com/intellij/ui/components/JBLabel.java',
    'com/intellij/ui/UIUtil.java',
    'com/intellij/util/ui/JBUI.java',
    'com/intellij/util/ui/UIUtil.java',
]
z = zipfile.ZipFile(jar)
names = set(z.namelist())
for w in want:
    if w in names:
        data = z.read(w)
        p = os.path.join(out, w)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, 'wb').write(data)
        print('extracted', w, len(data))
    else:
        print('missing', w)
