
import baidutranslate.TransApi;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtranslate.Language;
import com.gtranslate.Translator;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLFetcher;

import de.l3s.boilerpipe.sax.ImageExtractor;


import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import java.sql.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class DataFlush {
    final static int Max_T=10;
    final static String Filepath="C:\\Users\\91916\\OneDrive\\Documents";

    final static String Database_url="jdbc:mysql://localhost:3306/changsan?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=GMT&autoReconnect=true&useSSL=false";

    private static final String APP_ID = "20190407000285289";
    private static final String SECURITY_KEY = "qB6O4p63wfVKj3WAUZCV";
    public DataFlush(){

    }

    public static void main(String[] args) {
        Statement stmt;
        Connection con;
        String web_content="";
        InputStream in=null;
        int Size=0;


        System.out.println("come here");
        TransApi api = new TransApi(APP_ID, SECURITY_KEY);


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(Database_url,"root","zyj.1234");
            stmt = con.createStatement();

            ResultSet rs=stmt.executeQuery("select * from store_url_info");
            final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
            ImageExtractor ie=ImageExtractor.INSTANCE;
            rs.last();
            Size=rs.getRow();
            rs.beforeFirst();

            FlushFile flushFile=new FlushFile();

            URL url;
            int count=0;
            File doc;
            List<Image> imgUrls;
            InputStream img_in;
            ExecutorService pool = null;
            System.out.println(Size+"------");
            pool= Executors.newFixedThreadPool(Max_T);

            JsonObject jsonObj;
            String res;
            JsonArray js;
            String f_con;
            String docPath;
            URL myUrl;
            String content;
            String str;

        for(int i=0;i<Size;i++){
            rs.next();
            f_con="";
            docPath=rs.getString("title").replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
            myUrl=new URL(rs.getString("url"));
            content=BoilerGetText(myUrl);

            doc=new File(Filepath+"//"+docPath);
            doc.deleteOnExit();
            doc.mkdir();

            str=api.getTransResult(content, "en", "zh");

            jsonObj = (JsonObject)new JsonParser().parse(str);//解析json字段
            res = jsonObj.get("trans_result").toString();//获取json字段中的 result字段，因为result字段本身即是一个json数组字段，所以要进一步解析
            js = new JsonParser().parse(res).getAsJsonArray();//解析json数组字段
             for(int x=0;x<js.size();x++){
                 jsonObj = (JsonObject)js.get(x);//result数组中只有一个元素，所以直接取第一个元素
                 f_con+=jsonObj.get("dst").getAsString()+"\n";

             }


            flushFile.CreateWordFile(Filepath+"//"+docPath,docPath+".docx",f_con);

            imgUrls = ie.process(myUrl, extractor);

            if (!imgUrls.isEmpty()){
                for (Image img : imgUrls) {
                    url=new URL(img.getSrc());
                    img_in= url.openConnection().getInputStream();
                    GetImg getImg=new GetImg(Filepath,docPath,img,img_in,flushFile,count++);
                    Thread td=new Thread(getImg);
                    pool.execute(td);
                }
            }
        }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (BoilerpipeProcessingException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }


    static class GetImg implements Runnable{
        private String Filepath;
        private String docPath;
        private InputStream img_in;
        private Image img;
        private FlushFile flushFile;
        private int count;

        GetImg(String path,String doc,Image ig,InputStream in,FlushFile flush,int co){
            this.Filepath=path;
            this.docPath=doc;
            this.img=ig;
            this.img_in=in;
            this.flushFile=flush;
            this.count=co;

        }
        @Override
        public void run() {
          int state=  flushFile.CreateImg(Filepath,docPath,img.getAlt()+String.valueOf(count++)+".jpg",img_in);

        }
    }

    private static String BoilerGetText(URL myUrl){


        try {

            InputSource document= HTMLFetcher.fetch(myUrl).toInputSource();

            BoilerpipeSAXInput saxInput= null;
            saxInput = new BoilerpipeSAXInput(document);
            TextDocument document1=saxInput.getTextDocument();

            return ArticleExtractor.INSTANCE.getText(document1);

        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (BoilerpipeProcessingException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }


    }

}
