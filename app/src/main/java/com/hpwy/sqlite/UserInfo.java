package com.hpwy.sqlite;

import com.hpwy.sqlite.db.annotion.DbFiled;
import com.hpwy.sqlite.db.annotion.DbTable;

/**
 * User : wy
 * Date : 2017/1/10
 * 坑：1.修饰符必须是public，不然反射不到
 *     2.数据必须用对象，如果说int类型数据必须写成Integer，不然在加载的时候int是有默认值为0
 */
@DbTable("tb_user")
public class UserInfo {

    @DbFiled("u_id")
    public Integer userId;
    @DbFiled("u_name")
    public String userName;
    @DbFiled("u_pwd")
    public String userPwd;
    @DbFiled("u_age")
    public Integer age;

    public UserInfo(){

    }
    public UserInfo(Integer userId,String userName, String userPwd, Integer age) {
        this.userId = userId;
        this.userName = userName;
        this.userPwd = userPwd;
        this.age = age;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserPwd(String userPwd) {
        this.userPwd = userPwd;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "userId="+userId + "      userName="+userName + "     userPwd="+userPwd+"          userAge="+age;
    }
}
