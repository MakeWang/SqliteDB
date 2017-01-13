package com.hpwy.sqlite.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * User : wy
 * Date : 2017/1/10
 * 简单工厂类
 */
public class BaseDaoFactory {

    //数据库
    private SQLiteDatabase sqLiteDatabase;
    //数据库路劲
    private String sqlPath;
    private static BaseDaoFactory baseDaoFactory = new BaseDaoFactory();

    public BaseDaoFactory(){
        sqlPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/wangyin.db";
        openDatabase();//打开数据库
    }

    public static BaseDaoFactory getInstance(){
        return baseDaoFactory;
    }

    /**
     * 打开数据库
     */
    private void openDatabase(){
        this.sqLiteDatabase = SQLiteDatabase.openOrCreateDatabase(sqlPath,null);
    }

    public synchronized <T extends BaseDao<M>,M> T getDataHelper(Class<T> clazz,Class<M> entityClass){
        BaseDao baseDao = null;
        try {
            baseDao = clazz.newInstance();
            baseDao.init(entityClass,sqLiteDatabase);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return (T) baseDao;
    }

}
