package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.TextUtil;

import java.util.List;
import java.util.UUID;

public class UserFactory {

    public static User findByToken(String token) {
        return Hib.query(session -> (User) session.createQuery("from User where token=:inToken")
                .setParameter("inToken", token)
                .uniqueResult());
    }

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
     * 更新用户信息到数据库
     *
     * @param user User
     * @return User
     */
    public static User update(User user) {
        return Hib.query(session -> {
            session.saveOrUpdate(user);
            return user;
        });
    }

    /**
     * 给当前的账户绑定PushId
     *
     * @param user   User
     * @param pushId PushId
     * @return User
     */
    public static User bindPushId(User user, String pushId) {
        if (Strings.isNullOrEmpty(pushId)) {
            return null;
        }

        //第一步查询是否有其他账户
        //取消绑定，避免推送混乱
        //查询的列表不包括自己
        Hib.queryOnly(session -> {
            @SuppressWarnings("unchecked")
            List<User> userList = (List<User>) session.createQuery("from User where lower(pushId)=:pushId and id!=userId")
                    .setParameter("pushId", pushId.toLowerCase())
                    .setParameter("userId", user.getId())
                    .list();

            for (User u : userList) {
                u.setPushId(null);
                //更新到数据库
                session.saveOrUpdate(u);
            }
        });

        if (pushId.equalsIgnoreCase(user.getPushId())) {
            //如果之前已经绑定了 不需要额外绑定
            return user;
        } else {
            //如果当前账户之前的设备Id和需要绑定的不同
            //那么需要单点登录，让之前的设备退出账户
            //给之前的设备推送一条退出消息
            if (Strings.isNullOrEmpty(user.getPhone())) {
                //TODO 推送一个退出消息
            }
            //更新新的pushId
            user.setPushId(pushId);
            return update(user);
        }

    }

    public static User login(String account, String password) {
        final String accountStr = account.trim();
        final String encodePassword = encodePassword(password);

        User user = Hib.query(session ->
                (User) session.createQuery("from User where phone=:inPhone and password=:inPassword")
                        .setParameter("inPhone", accountStr)
                        .setParameter("inPassword", encodePassword)
                        .uniqueResult());
        if (user != null) {
            //登录，更新token
            user = login(user);
        }
        return user;
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

        User user = createUser(account, password, name);
        if (user != null) {
            user = login(user);
        }
        return user;
    }

    /**
     * 注册部分的新建用户逻辑
     *
     * @param account  账号
     * @param password 密码
     * @param name     名字
     * @return User
     */
    private static User createUser(String account, String password, String name) {
        User user = new User();
        user.setName(name);
        user.setPassword(password);
        user.setPhone(account);
        //数据库存储
        return Hib.query(session -> (User) session.save(user));
    }

    /**
     * 进行登录操作
     *
     * @param user User
     * @return User
     */
    private static User login(User user) {
        //使用UUID来充当token
        String newToken = UUID.randomUUID().toString();
        //进行base64格式化
        newToken = TextUtil.encodeBase64(newToken);
        user.setToken(newToken);
        return update(user);
    }

    private static String encodePassword(String password) {
        password = password.trim();
        //MD5加密
        password = TextUtil.getMD5(password);
        //Base64加密
        return TextUtil.encodeBase64(password);
    }
}
