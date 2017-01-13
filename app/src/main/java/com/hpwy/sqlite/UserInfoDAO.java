package com.hpwy.sqlite;

import com.hpwy.sqlite.db.BaseDao;

/**
 * User : wy
 * Date : 2017/1/12
 */
public class UserInfoDAO extends BaseDao {
    @Override
    protected String createTable() {
        return "create table if not exists tb_user(u_id Integer,u_name varchar(20),u_pwd varchar(10),u_age Integer)";
    }
}
