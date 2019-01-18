package net.qiujuer.web.italker.push.bean.api.message;

import com.google.common.base.Strings;
import com.google.gson.annotations.Expose;
import net.qiujuer.web.italker.push.bean.api.user.UpdateInfoModel;
import net.qiujuer.web.italker.push.bean.db.Message;

import java.time.LocalDateTime;

/**
 * API请求数据的model
 */
public class MessageCreateModel {
    //ID从客户端生产，一个UUID
    @Expose
    private String id;

    // 内容
    @Expose
    private String content;

    // 附件，附属信息
    @Expose
    private String attach;

    // 消息类型
    @Expose
    private int type = Message.TYPE_STR;

    // 接受者的类型，群，人
    @Expose
    private int receiverType = Message.RECEIVER_TYPE_NONE;

    // 接收者Id
    @Expose
    private String receiverId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAttach() {
        return attach;
    }

    public void setAttach(String attach) {
        this.attach = attach;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(int receiverType) {
        this.receiverType = receiverType;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public static boolean check(MessageCreateModel model) {
        // Model 不允许为null，
        // 并且只需要具有一个及其以上的参数即可
        return model != null
                && !(Strings.isNullOrEmpty(model.id)
                || Strings.isNullOrEmpty(model.receiverId)
                || Strings.isNullOrEmpty(model.content))

                && (model.receiverType == Message.RECEIVER_TYPE_NONE
                || model.receiverType == Message.RECEIVER_TYPE_GROUP)

                && (model.type == Message.TYPE_STR
                || model.type == Message.TYPE_AUDIO
                || model.type == Message.TYPE_FILE
                || model.type == Message.TYPE_PIC);
    }
}
