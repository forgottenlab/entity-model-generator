package io.github.forgottenlab.emg.processor.resolver;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将 javac 的 {@link TypeMirror} 转换为可直接写入 Java 源码的类型表达式。
 */
final class TypeMirrorTypeResolver {

    ResolvedType resolve(TypeMirror typeMirror) {
        Set<String> referencedTypes = new LinkedHashSet<>();
        String qualifiedName = render(typeMirror, true, referencedTypes);
        String simpleName = render(typeMirror, false, new LinkedHashSet<>());
        return new ResolvedType(qualifiedName, simpleName, new ArrayList<>(referencedTypes));
    }

    private String render(TypeMirror typeMirror,
                          boolean qualified,
                          Set<String> referencedTypes) {
        TypeKind kind = typeMirror.getKind();
        return switch (kind) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE -> typeMirror.toString();
            case ARRAY -> renderArray((ArrayType) typeMirror, qualified, referencedTypes);
            case DECLARED -> renderDeclared((DeclaredType) typeMirror, qualified, referencedTypes);
            case WILDCARD -> renderWildcard((WildcardType) typeMirror, qualified, referencedTypes);
            case ERROR, TYPEVAR, INTERSECTION, UNION, NONE, NULL, VOID, EXECUTABLE,
                    PACKAGE, MODULE, OTHER -> throw unsupported(typeMirror);
        };
    }

    private String renderArray(ArrayType arrayType,
                               boolean qualified,
                               Set<String> referencedTypes) {
        return render(arrayType.getComponentType(), qualified, referencedTypes) + "[]";
    }

    private String renderDeclared(DeclaredType declaredType,
                                  boolean qualified,
                                  Set<String> referencedTypes) {
        TypeElement typeElement = (TypeElement) declaredType.asElement();
        referencedTypes.add(topLevelQualifiedName(typeElement));

        StringBuilder result = new StringBuilder(
                declaredBaseName(declaredType, typeElement, qualified, referencedTypes)
        );
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            result.append("<");
            for (int index = 0; index < typeArguments.size(); index++) {
                if (index > 0) {
                    result.append(", ");
                }
                result.append(render(typeArguments.get(index), qualified, referencedTypes));
            }
            result.append(">");
        }
        return result.toString();
    }

    private String declaredBaseName(DeclaredType declaredType,
                                    TypeElement typeElement,
                                    boolean qualified,
                                    Set<String> referencedTypes) {
        TypeMirror enclosingType = declaredType.getEnclosingType();
        if (enclosingType.getKind() == TypeKind.DECLARED) {
            return render(enclosingType, qualified, referencedTypes)
                    + "." + typeElement.getSimpleName();
        }
        if (!qualified && typeElement.getEnclosingElement() instanceof TypeElement enclosingElement) {
            return nestedSimpleName(enclosingElement) + "." + typeElement.getSimpleName();
        }
        return qualified
                ? typeElement.getQualifiedName().toString()
                : typeElement.getSimpleName().toString();
    }

    private String nestedSimpleName(TypeElement typeElement) {
        if (typeElement.getEnclosingElement() instanceof TypeElement enclosingElement) {
            return nestedSimpleName(enclosingElement) + "." + typeElement.getSimpleName();
        }
        return typeElement.getSimpleName().toString();
    }

    private String renderWildcard(WildcardType wildcardType,
                                  boolean qualified,
                                  Set<String> referencedTypes) {
        TypeMirror extendsBound = wildcardType.getExtendsBound();
        if (extendsBound != null) {
            return "? extends " + render(extendsBound, qualified, referencedTypes);
        }

        TypeMirror superBound = wildcardType.getSuperBound();
        if (superBound != null) {
            return "? super " + render(superBound, qualified, referencedTypes);
        }
        return "?";
    }

    private String topLevelQualifiedName(TypeElement typeElement) {
        TypeElement topLevel = typeElement;
        Element enclosing = topLevel.getEnclosingElement();
        while (enclosing instanceof TypeElement enclosingType) {
            topLevel = enclosingType;
            enclosing = topLevel.getEnclosingElement();
        }
        if (!(enclosing instanceof PackageElement)) {
            throw new IllegalArgumentException("无法解析声明类型的顶层类: " + typeElement);
        }
        return topLevel.getQualifiedName().toString();
    }

    private IllegalArgumentException unsupported(TypeMirror typeMirror) {
        return new IllegalArgumentException(
                "不支持的字段类型 " + typeMirror.getKind() + ": " + typeMirror
        );
    }

    record ResolvedType(String qualifiedName,
                        String simpleName,
                        List<String> referencedTypeNames) {
    }
}
