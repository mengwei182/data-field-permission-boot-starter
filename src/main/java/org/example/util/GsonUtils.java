package org.example.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author lihui
 * @since 2023/4/3
 */
public class GsonUtils {
    private static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

    private GsonUtils() {
    }

    /**
     * 返回全局GSON对象
     *
     * @return 全局GSON对象
     */
    public static Gson gson() {
        return GSON;
    }
}