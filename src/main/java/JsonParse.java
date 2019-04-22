import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonParse {

    public  String JtoS(String str){
        JsonObject jsonObj;
        String res;
        JsonArray js;
        String f_con = "";

        jsonObj = (JsonObject)new JsonParser().parse(str);//解析json字段
        res = jsonObj.get("trans_result").toString();//获取json字段中的 result字段，因为result字段本身即是一个json数组字段，所以要进一步解析
        js = new JsonParser().parse(res).getAsJsonArray();//解析json数组字段
        for(int x=0;x<js.size();x++){
            jsonObj = (JsonObject)js.get(x);//result数组中只有一个元素，所以直接取第一个元素
            f_con+=jsonObj.get("dst").getAsString()+"\n";
        }
        if (f_con=="") f_con="there has nothing,JsonParse.java is here";
        return f_con;
    }
}
