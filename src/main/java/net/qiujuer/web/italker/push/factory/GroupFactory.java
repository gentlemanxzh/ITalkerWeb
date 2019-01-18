package net.qiujuer.web.italker.push.factory;

import net.qiujuer.web.italker.push.bean.db.Group;
import net.qiujuer.web.italker.push.bean.db.GroupMember;
import net.qiujuer.web.italker.push.bean.db.User;

import java.util.Set;

/**
 * 群数据库处理
 */
public class GroupFactory {
    public static Group findById(String groupId) {
        //查询一个群
        return null;
    }

    public static Group findById(User user,String groupId) {
        //查询一个群
        return null;
    }


    public static Set<GroupMember> getMembers(Group group) {
        //查询一个群的成员
        return null;
    }
}
