package org.example.aop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.annotation.DataPermission;
import org.example.annotation.FieldPermission;
import org.example.annotation.IgnorePermission;
import org.example.core.DataInformation;
import org.example.core.DataPermissionProvider;
import org.example.properties.DataInformationProperties;
import org.example.util.GsonUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * controller返回值时的切面，动态处理数据权限字段
 *
 * @author lihui
 */
@Aspect
public class DataPermissionAop {
    @Resource
    private DataInformationProperties dataInformationProperties;
    @Resource
    private DataPermissionProvider dataPermissionProvider;

    @Around("execution(* *.*Controller.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object targetObject = joinPoint.proceed();
        // this point是一个method对象
        Object thisPoint = joinPoint.getThis();
        if (thisPoint == null) {
            return targetObject;
        }
        Class<?> thisPointClass = thisPoint.getClass();
        IgnorePermission ignorePermission = thisPointClass.getAnnotation(IgnorePermission.class);
        if (ignorePermission != null) {
            return targetObject;
        }
        if (!dataInformationProperties.isEnable()) {
            return targetObject;
        }
        if (!checkTargetClass(targetObject)) {
            return targetObject;
        }
        List<DataInformation> currentDataPermissions = dataPermissionProvider.getCurrentDataPermissions();
        Map<String, DataInformation> dataInformationMap = currentDataPermissions.stream().collect(Collectors.toMap(DataInformation::getDataName, o -> o));
        // 提取出目标变量中的数据
        Field targetField = targetObject.getClass().getDeclaredField(dataInformationProperties.getTargetField());
        targetField.setAccessible(true);
        return targetObjectReset(targetObject, handleAnnotation(targetField.get(targetObject), new ArrayList<>(), dataInformationMap));
    }

    /**
     * 校验是否是目标类
     *
     * @param targetObject 被校验的对象
     * @return 正确返回true，其他返回false
     */
    private boolean checkTargetClass(Object targetObject) {
        try {
            Class<?> targetClass = Class.forName(dataInformationProperties.getTargetClass());
            if (targetObject.getClass() != targetClass) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 处理对象及对象的字段注解
     *
     * @param object 待处理的对象
     * @param objects 已经处理过的实例信息，避免递归循环校验导致栈溢出
     * @param dataInformationMap 当前权限所拥有的数据数据，key为数据名
     * @return 权限处理后的对象
     */
    private Object handleAnnotation(Object object, List<Object> objects, Map<String, DataInformation> dataInformationMap) {
        // 单列集合类型
        if (object instanceof Collection) {
            handleCollectionType(object, objects, dataInformationMap);
        }
        // 双列集合类型
        if (object instanceof Map) {
            handleMapType(object, objects, dataInformationMap);
        }
        // 数组类型
        if (object.getClass().isArray()) {
            handleArrayType(object, objects, dataInformationMap);
        }
        // 校验对象是否是JsonObject类型
        if (checkConvertJsonObject(object)) {
            object = handleObjectType(object, objects, dataInformationMap);
        }
        return object;
    }

    /**
     * 处理单列集合类型对象
     *
     * @param object 待处理的对象
     * @param objects 已经处理过的实例信息，避免递归循环校验导致栈溢出
     * @param dataInformationMap 当前权限所拥有的数据数据，key为数据名
     */
    private void handleCollectionType(Object object, List<Object> objects, Map<String, DataInformation> dataInformationMap) {
        Collection<Object> collection = (Collection<Object>) object;
        List<Object> resultList = new ArrayList<>(collection.size());
        for (Object o : collection) {
            resultList.add(handleAnnotation(o, objects, dataInformationMap));
        }
        collection.clear();
        collection.addAll(resultList);
    }

    /**
     * 处理双列集合类型对象
     *
     * @param object 待处理的对象
     * @param objects 已经处理过的实例信息，避免递归循环校验导致栈溢出
     * @param dataInformationMap 当前权限所拥有的数据数据，key为数据名
     */
    private void handleMapType(Object object, List<Object> objects, Map<String, DataInformation> dataInformationMap) {
        // 使用有序的Map，保证key的顺序不变，不影响原有逻辑
        LinkedHashMap<Object, Object> map = new LinkedHashMap<>((Map<?, ?>) object);
        map.replaceAll((k, v) -> handleAnnotation(map.get(k), objects, dataInformationMap));
    }

    /**
     * 处理数组类型对象
     *
     * @param object 待处理的对象
     * @param objects 已经处理过的实例信息，避免递归循环校验导致栈溢出
     * @param dataInformationMap 当前权限所拥有的数据数据，key为数据名
     */
    private void handleArrayType(Object object, List<Object> objects, Map<String, DataInformation> dataInformationMap) {
        int length = Array.getLength(object);
        for (int i = 0; i < length; i++) {
            Array.set(object, i, handleAnnotation(Array.get(object, i), objects, dataInformationMap));
        }
    }

    /**
     * 处理普通类型对象
     *
     * @param object 待处理的目标对象
     * @param objects 已经处理过的实例信息，避免递归循环校验导致栈溢出
     * @param dataInformationMap 该操作拥有的权限集合，key为数据名
     * @return 权限处理后的对象
     */
    private Object handleObjectType(Object object, List<Object> objects, Map<String, DataInformation> dataInformationMap) {
        // 已经处理过这种类型的对象了，不需要再处理，避免递归栈溢出
        Class<?> clazz = object.getClass();
        if (objects.contains(object)) {
            return object;
        }
        objects.add(object);
        // 非集合类型
        List<Field> fields = new ArrayList<>();
        recursionSuperClassField(fields, object.getClass());
        boolean isHandle = false;
        JsonObject jsonObject = JsonParser.parseString(GsonUtils.gson().toJson(object)).getAsJsonObject();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                IgnorePermission ignorePermission = field.getAnnotation(IgnorePermission.class);
                if (ignorePermission != null) {
                    continue;
                }
                Object fieldObject = field.get(object);
                handleAnnotation(fieldObject, objects, dataInformationMap);
                DataPermission dataPermission = clazz.getAnnotation(DataPermission.class);
                FieldPermission fieldPermission = field.getAnnotation(FieldPermission.class);
                // 只有类和其下的字段都加了注解才有效
                if (dataPermission == null || fieldPermission == null) {
                    continue;
                }
                String dataName = dataPermission.value();
                String fieldName = fieldPermission.value();
                DataInformation dataInformation = dataInformationMap.get(dataName);
                if (dataInformation != null && !dataInformation.getFieldNames().contains(fieldName)) {
                    // 没有找到对应权限，舍弃字段
                    jsonObject.remove(field.getName());
                    isHandle = true;
                }
            } catch (Exception ignored) {
            }
        }
        // 字段没有被处理过则不做改动
        if (isHandle) {
            // 处理后的对象再次赋值给源对象
            object = jsonObject;
        }
        return object;
    }

    /**
     * 校验参数能否被转为JsonObject
     *
     * @param object 待处理的对象
     * @return
     */
    private boolean checkConvertJsonObject(Object object) {
        try {
            JsonParser.parseString(GsonUtils.gson().toJson(object)).getAsJsonObject();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 处理后的结果重新set到目标类的目标成员变量上
     *
     * @param targetObject 被切面方法中指定的目标类
     * @param result 权限处理后的返回值
     * @return 新的目标类
     */
    private Object targetObjectReset(Object targetObject, Object result) {
        try {
            Field targetField = targetObject.getClass().getDeclaredField(dataInformationProperties.getTargetField());
            targetField.setAccessible(true);
            targetField.set(targetObject, result);
            return targetObject;
        } catch (Exception e) {
            // 出现异常，直接返回目标类，不能影响之后的逻辑
            return targetObject;
        }
    }

    private static void recursionSuperClassField(List<Field> fields, Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        Class<?> superclass = clazz.getSuperclass();
        recursionSuperClassField(fields, superclass);
    }
}