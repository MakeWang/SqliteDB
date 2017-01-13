package com.hpwy.sqlite.db;

import java.util.List;

/**
 * User : wy
 * Date : 2017/1/10
 * 接口类，定位数据库
 */
public interface IBaseDao<T> {

    /**
     * 添加数据
     * @param entity 添加的实体类
     * @return
     */
    Long insertDB(T entity);

    /**
     * 修改数据
     * @param entity
     * @param where
     * @return
     */
    int updateDB(T entity,T where);

    /**
     * 删除
     * @param where
     * @return
     */
    int deleteDB(T where);

    /**
     * 条件查询
     * @param where
     * @return
     */
    List<T> query(T where);

    /**
     * 分页查询
     * @param where  条件
     * @param orderBy 是否排序
     * @param startIndex 开始下标
     * @param limit 多少行数据
     * @return
     */
    List<T> query(T where,String orderBy,Integer startIndex,Integer limit);

}
