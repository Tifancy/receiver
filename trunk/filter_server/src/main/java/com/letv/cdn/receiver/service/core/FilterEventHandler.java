package com.letv.cdn.receiver.service.core;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.letv.cdn.receiver.manger.DisruptorServer;
import com.letv.cdn.receiver.manger.KafkaProcessManager;
import com.letv.cdn.receiver.model.RequestInfo;
import com.lmax.disruptor.EventHandler;

/**
 * 过滤业务实现
 * 
 * @author kk
 */
public class FilterEventHandler implements EventHandler<RequestInfo>{
    
    private static final Logger log = LoggerFactory.getLogger(FilterEventHandler.class);
    private final int ordinal; // 当前消费线程的编号
    
    public FilterEventHandler() {
    
        this(0);
    }
    
    public FilterEventHandler(Integer ordinal) {
    
        this.ordinal = ordinal;
    }
    
    public void onEvent(RequestInfo requestInfo, long sequence, boolean endOfBatch) throws Exception {
    
        if ((sequence % DisruptorServer.NUM_EVENT_HANDLERS) == ordinal) {
            // System.out.println("Filter next: " + sequence);
            String body = requestInfo.getBody();
            String[] bodyLine = body.split("\n");
            for (String line : bodyLine) {
                try {
                    StringBuffer ssb = new StringBuffer();
                    Pattern pattern = Pattern.compile("\"");
                    String[] temp = pattern.split(line);// 总体拆分
                    
                    int tmp = temp[0].indexOf("[");
                    int tmp2 = temp[0].indexOf(" ");
                    String ptime = temp[0].substring(tmp + 1, temp[0].length() - 2);
                    String userip = temp[0].substring(tmp2 + 1, tmp - 1);
                    // String serverip = temp[0].substring(0, tmp2);
                    String serverip = temp[0].substring(0, temp[0].indexOf(":"));
                    String[] tmpArr = temp[2].split(" ");
                    String bandwith = tmpArr[2];
                    String httpcode = tmpArr[1];
                    String responsetime = tmpArr[3];
                    
                    tmp = temp[1].indexOf("?");
                    tmpArr = temp[1].substring(tmp + 1, temp[1].length() - 9).split("&");
                    HashMap<String, String> map = new HashMap<String, String>();
                    String[] tmpArr2 = null;
                    for (String str : tmpArr) {
                        tmpArr2 = str.split("=");
                        map.put(tmpArr2[0], tmpArr2.length < 2 ? "" : tmpArr2[1]);
                    }
                    String maliu = map.containsKey("b") ? map.get("b") : "";
                    String platid = map.containsKey("platid") ? map.get("platid") : "";
                    String splatid = map.containsKey("splatid") ? map.get("splatid") : "";
                    String playid = map.containsKey("playid") ? map.get("playid") : "";
                    String geo = map.containsKey("geo") ? map.get("geo") : "";
                    if ("2".equals(platid) && "0".equals(playid)) {// 持久化云视频的点播
                        // MessageUtil.addPersistence(line);
                        KafkaProcessManager.getPersistenceProcessById(ordinal + 1).sendObjectKafka(line);
                    }
                    String customerName = "";
                    if (!"10".equals(platid)) {
                        String[] cntmp = temp[1].substring(0, 40).split("/");
                        if ("2".equals(platid)) {
                            customerName = cntmp[4] + "_" + cntmp[5];
                        } else {
                            customerName = cntmp[4];
                        }
                    }
                    String sign = map.containsKey("sign") ? map.get("sign") : "";
                    
                    ssb.append(ptime);
                    ssb.append("\t");
                    ssb.append(bandwith);
                    ssb.append("\t");
                    ssb.append(maliu);
                    ssb.append("\t");
                    ssb.append(httpcode);
                    ssb.append("\t");
                    ssb.append(userip);
                    ssb.append("\t");
                    ssb.append(serverip);
                    ssb.append("\t");
                    ssb.append(platid);
                    ssb.append("\t");
                    ssb.append(splatid);
                    ssb.append("\t");
                    ssb.append(sign);
                    ssb.append("\t");
                    ssb.append(playid);
                    ssb.append("\t");
                    ssb.append(responsetime);
                    
                    ssb.append("\t");
                    ssb.append(customerName);
                    ssb.append("\t");
                    ssb.append(geo);
                    
                    String join = ssb.toString();
                    // MessageUtil.addMessage(join);
                    KafkaProcessManager.getMessageProcessById(ordinal).sendObjectKafka(join);
                    // log.debug("send kafka data:" + join);
                } catch (Exception ex) {
                    log.error(ex.getMessage());
                }
            }
        }
    }
}
