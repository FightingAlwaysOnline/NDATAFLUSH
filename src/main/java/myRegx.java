import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class myRegx {

    public HashMap<String,String> testRegxfunc(String regx, String content){

        HashMap<String,String> title_data = new HashMap<String,String>();


        Pattern pattern=Pattern.compile(regx);

        Matcher matcher=pattern.matcher(content);
        matcher.matches();

        while(matcher.find()){

            title_data.put(matcher.group(2),matcher.group(1));


        }

        if (!matcher.find()){
            title_data.put("00","00");
        }

        return title_data;
    }
}
