package com.hpwy.sqlite;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.hpwy.sqlite.db.BaseDaoFactory;
import com.hpwy.sqlite.db.IBaseDao;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private IBaseDao<UserInfo> dao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dao = BaseDaoFactory.getInstance().getDataHelper(UserInfoDAO.class,UserInfo.class);
    }

    public void insertDAO(View view){
        for (int i = 1;i <= 30 ;i++){
            UserInfo user = new UserInfo(i,"sa","svse",1000+i);
            dao.insertDB(user);
        }
    }

    public void updateDAO(View view){
       //修改数据
        UserInfo user = new UserInfo();
        user.setUserName("wangyin");
        UserInfo userWhere = new UserInfo();
        userWhere.setUserId(20);
        dao.updateDB(user,userWhere);
    }

    public void deleteDAO(View view){
        UserInfo userWhere = new UserInfo();
        userWhere.setUserName("admin");
        dao.deleteDB(userWhere);
    }

    public void selectDB1(View view){
        UserInfo userWhere = new UserInfo();
        userWhere.setUserId(20);
        List<UserInfo> mList = dao.query(userWhere);

        for (int i = 0;i < mList.size();i++){
            System.out.print("aaaaaaaaaaa1"+mList.get(i).toString());
        }
    }

    public void selectDB2(View view){
        UserInfo userWhere = new UserInfo();
        //userWhere.setUserId(20);
        List<UserInfo> mList = dao.query(userWhere,null,2,10);

        for (int i = 0;i < mList.size();i++){
            System.out.print("aaaaaaaaaaa1"+mList.get(i).toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
