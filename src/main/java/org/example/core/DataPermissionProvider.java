package org.example.core;

import java.util.List;
import java.util.Map;

/**
 * 权限数据提供者
 *
 * @author lihui
 */
public interface DataPermissionProvider {
    /**
     * 该方法提供权限和数据及数据字段的对应关系
     *
     * @return key为权限唯一标致，如id或者code，value为数据和数据列集合
     */
    default Map<Object, List<DataInformation>> getDataPermissionMapping() {
        return null;
    }

    /**
     * 获取当前方法调用者的数据权限
     *
     * @return
     */
    default List<DataInformation> getCurrentDataPermissions() {
        return null;
    }
}