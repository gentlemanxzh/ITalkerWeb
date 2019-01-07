package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.db.User;
import net.qiujuer.web.italker.push.bean.db.UserFollow;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.TextUtil;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public static User findById(String id) {
        //通过主键Id查询更加方便
        return Hib.query(session -> (User) session.get(User.class, id));
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
            List<User> userList = (List<User>) session.createQuery("from User where lower(pushId)=:pushId and id!=:userId")
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
        return Hib.query(session -> {
            session.save(user);
            return user;
        });
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

    /**
     * 获取联系人的列表
     *
     * @param self User
     * @return List<User>
     */
    public static List<User> contacts(User self) {
        return Hib.query(session -> {
            //重新加载一次用户信息到self，和当前session绑定
            session.load(self, self.getId());
            Set<UserFollow> flows = self.getFollowing();
            //使用简写方式
            return flows.stream()
                    .map(UserFollow::getTarget)
                    .collect(Collectors.toList());
        });
    }

    /**
     * 关注人的操作
     *
     * @param origin 发起者
     * @param target 被关注的人
     * @param alias  备注名
     * @return 被关注的人
     */
    public static User follow(final User origin, final User target, final String alias) {
        UserFollow follow = getUserFollow(origin, target);
        if (follow != null) {
            return follow.getTarget();
        }
        return Hib.query(session -> {
            //想要操作懒加载的数据，需要重新load一次
            session.load(origin, origin.getId());
            session.load(target, target.getId());
            //我关注人的时候，同时他也关注我
            //所以需要添加两条UserFollow数据
            UserFollow originFollow = new UserFollow();
            originFollow.setOrigin(origin);
            originFollow.setTarget(target);
            //备注是 我对他的备注，他对我默认没有备注
            originFollow.setAlias(alias);

            //他添加我的
            UserFollow targetFollow = new UserFollow();
            targetFollow.setOrigin(target);
            targetFollow.setTarget(origin);

            //保存操作
            session.save(originFollow);
            session.save(targetFollow);

            return target;
        });

    }

    /**
     * 查询两个人是否已经关注
     *
     * @param origin 发起者
     * @param target 被关注人
     * @return 返回中间类UserFollow
     */
    public static UserFollow getUserFollow(final User origin, final User target) {
        return Hib.query(session -> (UserFollow) session.createQuery("from UserFollow where originId=:originId and targetId = :targetId")
                .setParameter("originId", origin.getId())
                .setParameter("targetId", target.getId())
                .setMaxResults(1)
                .uniqueResult());
    }

    /**
     * 搜索联系人的实现
     *
     * @param name 查询的name 允许为空
     * @return 查询到的用户集合，如果name为null，则返回最近的用户
     */
    @SuppressWarnings("unchecked")
    public static List<User> search(String name) {
        if (Strings.isNullOrEmpty(name))
            name = "";//保证不能为null的情况，减少后面的一下判断和额外的错误
        final String searchName = "%" + name + "%";//模糊匹配

        return Hib.query(session -> {
            //name忽略大小写，并且使用like查询 头像和描述必须完善才能查询到
            return (List<User>) session.createQuery("from User where  lower(name) like :name and portrait is not null and description is not null ")
                    .setParameter("name", searchName)
                    .setMaxResults(20)//最多二十条
                    .list();
        });
    }
}
