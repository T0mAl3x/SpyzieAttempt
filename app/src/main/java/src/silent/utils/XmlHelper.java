package src.silent.utils;

/**
 * Created by all3x on 3/18/2018.
 */

public class XmlHelper {
    public static String buildRegisterPack(String[] payload, String username) throws Exception {
        StringBuilder xmlPayload = new StringBuilder();

        xmlPayload.append("<phone-registration>");
        xmlPayload.append("<username-associated>" + username + "</username-associated>");
        xmlPayload.append("<phone-IMEI>" + payload[0] + "</phone-IMEI>");
        xmlPayload.append("<phone-manufacturer>" + payload[1] + "</phone-manufacturer>");
        xmlPayload.append("<phone-model>" + payload[2] + "</phone-model>");
        xmlPayload.append("</phone-registration>");

        return xmlPayload.toString();
    }
}
