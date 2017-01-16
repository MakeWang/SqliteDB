# SqliteDB
封装Sqlite数据库</br>
优点：高扩展性，性能与市场上现在比较流行的ormLitem框架速度差不多快，使用简单<br>
技术点：反射，注解，泛型。</br>
框架设计模式：单例模式，简单工程模式，模板模式。</br>
注意： 1、修饰符必须是public，不然反射不到</br>
      2、数据必须用对象，如果说int类型数据必须写成Integer，不然在加载的时候int是有默认值为0</br>
# 使用
```java
  IBaseDao<UserInfo> dao = BaseDaoFactory.getInstance().getDataHelper(UserInfoDAO.class,UserInfo.class);
  UserInfo user = new UserInfo(i,"sa","svse",1);
  dao.insertDB(user);
```

# 框架代码
# IBaseDao 定义公共的接口
```java
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

```

# BaseDao 接口的实现类

```java
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

```

# UserInfoDAO表的创建

```java
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
```

# UserInfo 数据封装实体类

```java
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

```

# BaseDaoFactory 工厂类

```java
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

```

# DbTable 注解表名

```java
  /**
 * User : wy
 * Date : 2017/1/10
 * 作用域在类名
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbTable {
    String value();
}
```

# DbFiled 注解字段名

```java
  /**
 * User : wy
 * Date : 2017/1/10
 * 作用域在方法民
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DbFiled {
    String value();
}
```

# MainActivity

```java
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

```
