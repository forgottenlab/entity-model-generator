package io.github.forgottenlab.emg.processor.resolver;

import io.github.forgottenlab.emg.annotations.AutoModel;
import io.github.forgottenlab.emg.annotations.AutoView;
import io.github.forgottenlab.emg.core.model.AutoViewMetadata;
import io.github.forgottenlab.emg.core.model.FieldMetadata;
import io.github.forgottenlab.emg.core.model.SourceMetadata;
import io.github.forgottenlab.emg.core.util.NameUtils;
import io.github.forgottenlab.emg.processor.support.PackageResolver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 根据共享来源元数据一次性建立 group 索引并解析全部 View。
 */
public class AutoViewMetadataResolver {

    public List<AutoViewMetadata> resolve(SourceMetadata sourceMetadata,
                                          AutoModel autoModel,
                                          List<AutoView> annotations) {
        Map<String, List<FieldMetadata>> fieldsByGroup = indexFieldsByGroup(sourceMetadata.getFields());
        String baseName = autoModel == null
                ? NameUtils.removeEntitySuffix(sourceMetadata.getClassName())
                : autoModel.value();
        String packageName = PackageResolver.resolveViewPackage(sourceMetadata.getPackageName());

        List<AutoViewMetadata> views = new ArrayList<>();
        for (AutoView annotation : annotations) {
            String group = annotation.value();
            String className = annotation.name().isEmpty()
                    ? baseName + capitalizeGroup(group) + "View"
                    : annotation.name();

            AutoViewMetadata metadata = new AutoViewMetadata();
            metadata.setSourceQualifiedName(sourceMetadata.getQualifiedName());
            metadata.setGroup(group);
            metadata.setViewPackageName(packageName);
            metadata.setViewClassName(className);
            List<FieldMetadata> selectedFields = fieldsByGroup.get(group);
            if (selectedFields != null) {
                metadata.getFields().addAll(selectedFields);
            }
            views.add(metadata);
        }
        return views;
    }

    private Map<String, List<FieldMetadata>> indexFieldsByGroup(List<FieldMetadata> fields) {
        Map<String, List<FieldMetadata>> fieldsByGroup = new LinkedHashMap<>();
        for (FieldMetadata field : fields) {
            for (String group : field.getViewGroups()) {
                fieldsByGroup.computeIfAbsent(group, ignored -> new ArrayList<>()).add(field);
            }
        }
        return fieldsByGroup;
    }

    private String capitalizeGroup(String group) {
        if (group == null || group.isEmpty()) {
            return group;
        }
        int firstCodePoint = group.codePointAt(0);
        int firstLength = Character.charCount(firstCodePoint);
        return new String(Character.toChars(Character.toUpperCase(firstCodePoint)))
                + group.substring(firstLength);
    }
}
