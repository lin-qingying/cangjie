import sys, zipfile, os

jar = sys.argv[1]
out = sys.argv[2]
os.makedirs(out, exist_ok=True)
want = [
    'com/intellij/icons/AllIcons.java',
    'com/intellij/ui/components/panels/VerticalLayout.java',
    'com/intellij/openapi/ui/popup/ComponentPopupBuilder.java',
    'com/intellij/ui/components/JBScrollPane.java',
    'com/intellij/ui/components/JBPanel.java',
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
