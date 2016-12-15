package br.com.processor.mapper;

import com.google.gson.Gson;

import java.util.List;

/**
 * Created by jordaoesa on 12/12/16.
 */
public class MessageMapper {

    private int tp;
    private String msg;
    private List<String> msgs;

    public MessageMapper() {}

    public MessageMapper(int tp, String msg, List<String> msgs) {
        this.tp = tp;
        this.msg = msg;
        this.msgs = msgs;
    }

    public int getTp() {
        return tp;
    }

    public void setTp(int tp) {
        this.tp = tp;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<String> getMsgs() {
        return msgs;
    }

    public void setMsgs(List<String> msgs) {
        this.msgs = msgs;
    }

    public String toJson(){
        return new Gson().toJson(this);
    }
}
