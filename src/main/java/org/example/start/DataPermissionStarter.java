package org.example.start;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.DataPermission;
import org.example.annotation.FieldPermission;
import org.example.annotation.IgnorePermission;
import org.example.properties.DataInformationProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 数据权限启动器，扫描已配置的注解
 *
 * @author lihui
 */
@Slf4j
@Getter
public class DataPermissionStarter implements ApplicationRunner {
    /**
     * 数据权限注解集合，key为类的全限定名
     */
    private final ConcurrentHashMap<String, DataPermission> dataPermissionMap = new ConcurrentHashMap<>();
    /**
     * 数据字段权限注解集合，key为类的全限定名，value为field的全限定名和注解信息的映射
     */
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, FieldPermission>> fieldPermissionsMap = new ConcurrentHashMap<>();
    /**
     * 类和所标记的数据权限注解集合，key为被注解注释的类的class
     */
    private final ConcurrentHashMap<Class<?>, DataPermission> classDataPermissionMap = new ConcurrentHashMap<>();
    @Resource
    private DataInformationProperties dataInformationProperties;

    @Override
    public void run(ApplicationArguments args) {
        if (!dataInformationProperties.isEnable()) {
            return;
        }
        List<Class<?>> classes = new ArrayList<>();
        log.info("数据权限组件启动中...");
        String scanBasePackages = dataInformationProperties.getScanBasePackages();
        if (!StringUtils.hasLength(scanBasePackages)) {
            log.info("数据权限组件启动失败：scanBasePackages参数为空");
            return;
        }
        String[] packages = scanBasePackages.split(",");
        for (String path : packages) {
            classes.addAll(getClassesFromPackage(path.trim()));
        }
        for (Class<?> object : classes) {
            try {
                DataPermission dataPermission = object.getAnnotation(DataPermission.class);
                if (dataPermission == null) {
                    continue;
                }
                log.info(dataPermission.value());
                classDataPermissionMap.put(object, dataPermission);
                dataPermissionMap.put(object.getName(), dataPermission);
                Field[] fields = object.getDeclaredFields();
                for (Field field : fields) {
                    FieldPermission fieldPermission = field.getAnnotation(FieldPermission.class);
                    if (fieldPermission == null) {
                        continue;
                    }
                    ConcurrentHashMap<String, FieldPermission> fieldPermissions = fieldPermissionsMap.computeIfAbsent(object.getName(), o -> new ConcurrentHashMap<>());
                    fieldPermissions.put(object.getName() + "." + field.getName(), fieldPermission);
                }
                // 校验父类是否有注解，父类的注解也会被归为当前类的数据
                handleSuperclassAnnotation(object);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        log.info("数据权限组件启动成功");
    }

    /**
     * 处理父类的注解，且只校验父类中的FieldPermission注解，默认可以继承FieldPermission注解
     *
     * @param clazz
     */
    private void handleSuperclassAnnotation(Class<?> clazz) {
        try {
            // 校验父类是否有注解，父类的注解也会被归为当前类的数据
            Class<?> superclass = clazz.getSuperclass();
            if (superclass == null) {
                return;
            }
            Field[] fields = superclass.getDeclaredFields();
            for (Field field : fields) {
                FieldPermission fieldPermission = field.getAnnotation(FieldPermission.class);
                if (fieldPermission == null) {
                    continue;
                }
                ConcurrentHashMap<String, FieldPermission> fieldPermissions = fieldPermissionsMap.computeIfAbsent(clazz.getName(), o -> new ConcurrentHashMap<>());
                fieldPermissions.put(clazz.getName() + "." + field.getName(), fieldPermission);
            }
            // 递归父类
            handleSuperclassAnnotation(superclass);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 获得包下面的所有的class
     *
     * @param packageName package完整名称
     * @return 包含所有class的实例集合
     */
    public List<Class<?>> getClassesFromPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        // 包名对应的路径名称
        String packageDirName = packageName.replace('.', '/');
        try {
            Enumeration<URL> enumeration = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                log.debug("扫描到的目标包：" + url.getPath());
                String protocol = url.getProtocol();
                // 不是jar包类型直接跳过
                if (!ResourceUtils.URL_PROTOCOL_JAR.equals(protocol)) {
                    continue;
                }
                URLConnection urlConnection = url.openConnection();
                if (urlConnection instanceof JarURLConnection) {
                    JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                    JarFile jarFile = jarURLConnection.getJarFile();
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        // 判断是否是class类文件
                        if (!jarEntry.getName().endsWith(".class")) {
                            continue;
                        }
                        Class<?> clazz = loadClass(jarEntry.getName().replace(".class", "").replace("/", "."));
                        if (clazz != null) {
                            // class上设置了IgnorePermission注解则过滤掉
                            IgnorePermission ignorePermission = clazz.getAnnotation(IgnorePermission.class);
                            if (ignorePermission != null) {
                                continue;
                            }
                            classes.add(clazz);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return classes;
    }

    private Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}