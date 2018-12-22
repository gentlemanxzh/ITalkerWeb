package net.qiujuer.web.italker.push.factory;

import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.TextUtil;
import org.hibernate.Session;

public class UserFactory {

    public static User findByPhone(String phone) {
        return Hib.query(session -> (User) session.createQuery("from User where phone=:inPhone")
                .setParameter("inPhone", phone)
                .uniqueResult());
    }

    public static User findByName(String name) {
        return Hib.query(session -> (User) session.createQuery("from User where name=:inName")
                .setParameter("inName", name)
                .uniqueResult());
    }


    /**
     * 用户注册
     *
     * @param account  账户
     * @param password 密码
     * @param name     用户名
     * @return User
     */
    public static User register(String account, String password, String name) {
        //去除空格
        account = account.trim();
        //密码加密
        password = encodePassword(password);

        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setPhone(account);

        //数据库操作
        //创建会话
        Session session = Hib.session();
        //开启事务
        session.beginTransaction();
        //保存
        try {
            session.save(user);
            //提交
            session.getTransaction().commit();
            return user;
        } catch (Exception e) {
            session.getTransaction().rollback();
            return null;
        }
    }

    private static String encodePassword(String password) {
        password = password.trim();
        //MD5加密
        password = TextUtil.getMD5(password);
        //Base64加密
        return TextUtil.encodeBase64(password);
    }
}
