package eu.jameshamilton.classfile;

import java.io.IOException;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.constant.ClassDesc;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarClassHierarchyResolver implements ClassHierarchyResolver {
    private final ClassHierarchyResolver resourceClassHierarchyResolver;

    public JarClassHierarchyResolver(JarFile jarFile) {
        this.resourceClassHierarchyResolver = ClassHierarchyResolver.ofResourceParsing(
            classDesc -> {
                String internalName = toInternalName(classDesc);
                JarEntry jarEntry = jarFile.getJarEntry(internalName + ".class");
                if (jarEntry == null) return null;

                try {
                    return jarFile.getInputStream(jarEntry);
                } catch (IOException e) {
                    return null;
                }
            }
        );
    }

    private static String toInternalName(ClassDesc cd) {
        var desc = cd.descriptorString();
        if (desc.charAt(0) == 'L')
            return desc.substring(1, desc.length() - 1);
        throw new IllegalArgumentException(desc);
    }

    @Override
    public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
        return resourceClassHierarchyResolver.getClassInfo(classDesc);
    }
}
