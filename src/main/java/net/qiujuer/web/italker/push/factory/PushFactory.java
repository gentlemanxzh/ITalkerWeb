package net.qiujuer.web.italker.push.factory;

import com.google.common.base.Strings;
import net.qiujuer.web.italker.push.bean.api.base.PushModel;
import net.qiujuer.web.italker.push.bean.card.MessageCard;
import net.qiujuer.web.italker.push.bean.db.*;
import net.qiujuer.web.italker.push.utils.Hib;
import net.qiujuer.web.italker.push.utils.PushDispatcher;
import net.qiujuer.web.italker.push.utils.TextUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 消息存储于处理的工具类
 */
public class PushFactory {
    //发送一条消息，并在发送历史消息保存
    public static void pushNewMessage(User sender, Message message) {
        if (sender == null || message == null)
            return;
        //消息卡片，用于发送
        MessageCard card = new MessageCard(message);
        //要推送的字符串
        String entity = TextUtil.toJson(card);

        //发送者
        PushDispatcher dispatcher = new PushDispatcher();

        if (message.getGroup() == null && Strings.isNullOrEmpty(message.getGroupId())) {
            //给朋友发送消息
            User receiver = UserFactory.findById(message.getReceiverId());
            if (receiver == null)
                return;

            //历史记录表字段建立
            PushHistory history = new PushHistory();
            //普通消息类型
            history.setEntityType(PushModel.ENTITY_TYPE_MESSAGE);
            history.setEntity(entity);
            history.setReceiver(receiver);
            //接受者当前的设备推送ID
            history.setReceiverId(receiver.getPushId());

            //推送真的是的Model
            PushModel pushModel = new PushModel();
            //每一条的历史记录都是独立的，可以单独发送
            pushModel.add(history.getEntityType(), history.getEntity());
            //把需要发送的数据，丢给发送者进行发送
            dispatcher.add(receiver, pushModel);
            //保存到数据库
            Hib.queryOnly(session -> session.save(history));
        } else {
            Group group = message.getGroup();
            //因为延迟加载可能为null，需要进行ID查找
            if (group == null)
                group = GroupFactory.findById(message.getGroupId());

            if (group == null)
                return;

            //给群成员发送消息
            Set<GroupMember> members = GroupFactory.getMembers(group);
            if (members == null || members.size() == 0)
                return;
            //过滤我自己
            members = members.stream()
                    .filter(groupMember -> !groupMember.getUserId().equalsIgnoreCase(sender.getId()))
                    .collect(Collectors.toSet());
            //一个历史记录列表
            List<PushHistory> histories = new ArrayList<>();
            addGroupMembersPushModel(dispatcher,//推送的发送者
                    histories,//数据库要存储的列表
                    members,//所有的成员
                    entity,//要发送的数据
                    PushModel.ENTITY_TYPE_MESSAGE);//发送类型

            //保存到数据库
            Hib.queryOnly(session -> {
                for (PushHistory history : histories) {
                    session.saveOrUpdate(history);
                }
            });
        }

        //发送者进行真实的提交
        dispatcher.submit();
    }

    /**
     * 给群成员构建一个消息
     * 把消息存储到数据库，每个人每条消息都是一个记录
     */
    private static void addGroupMembersPushModel(PushDispatcher dispatcher,
                                                 List<PushHistory> histories,
                                                 Set<GroupMember> members,
                                                 String entity, int entityTypeMessage) {

        for (GroupMember member : members) {
            //无需通过Id再去找用户
            User receiver = member.getUser();
            if (receiver == null)
                return;

            //历史记录表字段建立
            PushHistory history = new PushHistory();
            //普通消息类型
            history.setEntityType(entityTypeMessage);
            history.setEntity(entity);
            history.setReceiver(receiver);
            //接受者当前的设备推送ID
            history.setReceiverId(receiver.getPushId());

            //推送真的是的Model
            PushModel pushModel = new PushModel();
            //每一条的历史记录都是独立的，可以单独发送
            pushModel.add(history.getEntityType(), history.getEntity());
            //把需要发送的数据，丢给发送者进行发送
            dispatcher.add(receiver, pushModel);
        }

    }
}
