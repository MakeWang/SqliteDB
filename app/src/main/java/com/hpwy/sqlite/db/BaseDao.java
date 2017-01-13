package com.hpwy.sqlite.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.hpwy.sqlite.db.annotion.DbFiled;
import com.hpwy.sqlite.db.annotion.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User : wy
 * Date : 2017/1/10
 */
public abstract class BaseDao<T> implements IBaseDao<T>{

    //数据库的引用
    private SQLiteDatabase sqLiteDatabase;
    //是否初始化过一次
    private boolean isInit = false;
    //持有操作数据库表对应的java类型
    private Class<T> entityClass;



    @Override
    public Long insertDB(T entity) {
        //将对象转化成Map
        Map<String,String> map = getValue(entity);
        ContentValues contentValues =  getContentValues(map);
        long result = sqLiteDatabase.insert(tableName,null,contentValues);
        return result;
    }

    @Override
    public int updateDB(T entity, T where) {
        int result = -1;
        Map value = getValue(entity);
        Map whereValue = getValue(where);
        //拼接where条件语句
        Condition condition = new Condition(whereValue);
        ContentValues contentValues = getContentValues(value);

        result = sqLiteDatabase.update(tableName,contentValues,condition.getWhereClause(),condition.getWhereArgs());

        return result;
    }

    @Override
    public int deleteDB( T where) {
        Map<String,String> map = getValue(where);
        //拼接条件语句
        Condition condition = new Condition(map);
        int result  = sqLiteDatabase.delete(tableName,condition.getWhereClause(),condition.whereArgs);
        return result;
    }

    @Override
    public List<T> query(T where){
        return query(where,null,null,null);
    }

    @Override
    public List<T> query(T where,String orderBy,Integer startIndex,Integer limit){

        Map map = getValue(where);
        String limitString = null;
        if(startIndex != null && limit != null){
            limitString = startIndex + " , " + limit;
        }

        Condition condition = new Condition(map);
        Cursor cursor = sqLiteDatabase.query(tableName,null,condition.getWhereClause(),condition.getWhereArgs(),null,null,orderBy,limitString);
        List<T> result = getResult(cursor,where);
        cursor.close();
        return result;
    }

    /**
     * 维护表字段与成员之间的关系
     * String ->  表名
     * Field  ->  成员
     */
    private HashMap<String,Field> cacheMap;

    private String tableName;//表名

    /**
     * 初始化实例
     * @param entity
     * @param sqLiteDatabase
     * @return
     */
    protected synchronized boolean init(Class<T> entity, SQLiteDatabase sqLiteDatabase){
        if(!isInit){//是否已经初始化过
            this.entityClass = entity;
            this.sqLiteDatabase = sqLiteDatabase;
            //类名是否有注解
            if(entity.getAnnotation(DbTable.class) == null){
                //这是直接获取到当前类的类名
                tableName = entity.getClass().getSimpleName();
            }else{
                //这是获取到注解的类名
                tableName = entity.getAnnotation(DbTable.class).value();
            }

            //检查是否打开数据库
            if(!sqLiteDatabase.isOpen()){ //如果是false
                return false;
            }

            //判断表是否为空
            if(!TextUtils.isEmpty(createTable())){ //如果不为空
                sqLiteDatabase.execSQL(createTable());//创建表
            }

            cacheMap = new HashMap<>();
            initCacheMap();
            isInit = true;
        }

        return isInit;
    }

    /**
     * 映射表与与字段的关系
     */
    private void initCacheMap(){
        //查询表第一行开始，0条数据（这里就是为了查询没表的每一列数据）
        String sql = "select * from " + this.tableName + " limit 1,0";
        Cursor cursor = null;
        try{
            cursor = sqLiteDatabase.rawQuery(sql,null);
            //获取表的列名的数组(表的所以列的名称)
            String[] columNames = cursor.getColumnNames();
            //拿到Field数组(所以带有参数的字段)
            Field[] colmunFields = entityClass.getFields();
            for (Field field : colmunFields){
                field.setAccessible(true);
            }


            //查找对应关系相匹配,循环表字段
            for (String curName : columNames){
                Field colmunFiled = null;

                //字段临时变量
                String fieldName = null;
                for(Field field : colmunFields){//参数字段
                    //判断字段是否有注解
                    if(field.getAnnotation(DbFiled.class) != null){
                        fieldName = field.getAnnotation(DbFiled.class).value();
                    }else{//如果没加注解,就直接拿到字段的名称
                        fieldName = field.getName();
                    }

                    //如果表列的名词和字段名词一致
                    if(curName.equals(fieldName)){
                        colmunFiled = field;
                        break;
                    }
                }

                if(colmunFiled != null){
                    //表中列名，字段名
                    cacheMap.put(curName,colmunFiled);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            cursor.close();
        }

    }

    public Map<String,String> getValue(T entity){
        Map<String,String> map = new HashMap<>();
        Iterator<Field> iteratorField = cacheMap.values().iterator();
        //遍历表列名字段
        while(iteratorField.hasNext()){
            Field field = iteratorField.next();
            String cacheKey = null;
            String cacheValue = null;

            if(field.getAnnotation(DbFiled.class) != null){
                cacheKey = field.getAnnotation(DbFiled.class).value();
            }else{
                cacheKey = field.getName();
            }

            try {
                if(field.get(entity) == null){
                    continue;
                }
                cacheValue = field.get(entity).toString();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            map.put(cacheKey,cacheValue);
        }
        return map;
    }

    /**
     * 将map 转换成ContentValues
     * @param map
     * @return
     */
    public ContentValues getContentValues(Map<String,String> map){
        ContentValues contentValues = new ContentValues();
        Set keys = map.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = map.get(key);
            if (value != null) {
                contentValues.put(key, value);
            }
        }
        return contentValues;
    }

    class Condition{

        //查询条件 name=? && pwd=?
        private String whereClause;

        private String[] whereArgs;

        public Condition(Map<String,String> whereClause){
            ArrayList list = new ArrayList();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" 1=1 ");
            Set keys = whereClause.keySet();
            Iterator iterator = keys.iterator();
            while(iterator.hasNext()){
                String key = (String) iterator.next();
                String value = whereClause.get(key);

                if(value != null){
                    //拼接条件语句-
                    stringBuilder.append(" and "+key+" =? ");
                    list.add(value);
                }
            }

            this.whereClause = stringBuilder.toString();
            this.whereArgs = (String[]) list.toArray(new String[list.size()]);

        }

        public String[] getWhereArgs(){
            return whereArgs;
        }

        public String getWhereClause(){
            return whereClause;
        }

    }

    private List<T> getResult(Cursor cursor,T where){

        ArrayList arrayList = new ArrayList();
        Object item;

        while(cursor.moveToNext()){
            try {
                item = where.getClass().newInstance();
                /**
                 * 列名  name
                 * 成员变量  Filed
                 */
                Iterator iterator = cacheMap.entrySet().iterator();
                while(iterator.hasNext()){
                    Map.Entry entry = (Map.Entry) iterator.next();
                    //得到列名
                    String colomunName = (String) entry.getKey();
                    //通过列名拿到列名的游标
                    Integer colomunIndex = cursor.getColumnIndex(colomunName);

                    Field field = (Field) entry.getValue();

                    Class type = field.getType();
                    if(colomunIndex != -1){
                        if(type == String.class){
                            //反射方式赋值
                            field.set(item,cursor.getString(colomunIndex));
                        }else if(type == Double.class){
                            field.set(item,cursor.getDouble(colomunIndex));
                        }else if(type == Long.class){
                            field.set(item,cursor.getLong(colomunIndex));
                        }else if(type == byte[].class){
                            field.set(item,cursor.getBlob(colomunIndex));
                        }else if(type == Short.class){
                            field.set(item,cursor.getShort(colomunIndex));
                        }else if(type == Integer.class){
                            field.set(item,cursor.getInt(colomunIndex));
                        }else{ //不支持的类型
                            continue;
                        }
                    }

                }

                arrayList.add(item);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return arrayList;
    }

    /**
     * 创建表
     * @return
     */
    protected abstract String createTable();
}
