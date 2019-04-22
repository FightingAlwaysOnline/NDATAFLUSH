
import baidutranslate.TransApi;

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

    static TransApi api;
    public DataFlush(){

    }

    public static void main(String[] args) {
        Statement stmt;
        Connection con;
        InputStream in=null;
        int Size=0;


        api = new TransApi(APP_ID, SECURITY_KEY);

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            con = DriverManager.getConnection(Database_url, "root", "zyj.1234");
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("select * from store_url_info");


            rs.last();
            Size = rs.getRow();
            rs.beforeFirst();

            ExecutorService pool = Executors.newFixedThreadPool(Max_T);


            String docPath;
            String url;
            int id;
            for (int i = 0; i < Size; i++) {
                if(rs.isLast()) break;
                rs.next();
                id=rs.getInt("id");
                if (rs.getBoolean("w_status")){
                    continue;
                }
                docPath=rs.getString("title").replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
                url=rs.getString("url");
                pool.submit(new Thread(new MainRun(url,docPath,api,stmt,id)));
            }
            pool.shutdown();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
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
            return "";
        } catch (BoilerpipeProcessingException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            return "";
        }


    }

   static class MainRun implements Runnable{

         String docPath;
         URL myUrl;
         String content;
         String str;

         URL url;
         String web_url;
         int count = 0;

         List<Image> imgUrls;
         JsonParse jsonParse;

       final BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
       final ImageExtractor ie = ImageExtractor.INSTANCE;

       ExecutorService pool1 = null;
       FlushFile flushFile;
       TransApi api;
       Statement stmt;
       int id;

       MainRun(String ras,String path ,TransApi apis,Statement rs,int id) {
            web_url=ras;
            pool1 = Executors.newFixedThreadPool(Max_T);
            this.docPath=path;
            this.api=apis;
            this.stmt=rs;
            this.id=id;
        }

        @Override
        public void run() {

            flushFile = new FlushFile();
            try {
                jsonParse=new JsonParse();
                String docName_json=api.getTransResult(docPath, "en", "zh");

                String Dic_name=jsonParse.JtoS(docName_json).replaceAll("[^a-zA-Z0-9\\.\\-\\u4e00-\\u9fa5]", "_");

                myUrl=new URL(web_url);

                if (myUrl.openConnection().getContentType()==null) return;

                content=BoilerGetText(myUrl);

                if (content.length()<20 || content.equals("")){
                    return ;
                }

                File file=new File(DataFlush.Filepath+"\\"+Dic_name);
                file.deleteOnExit();
                file.mkdir();

                long a=content.substring(0,2040).length();
                str=api.getTransResult(content.substring(0,2040), "en", "zh");

                content=content+"\n"+"this is the web url:"+web_url;
                imgUrls = ie.process(myUrl, extractor);

                int i=flushFile.CreateWordFile(DataFlush.Filepath+"\\"+Dic_name,Dic_name+".docx",jsonParse.JtoS(str));
                stmt.executeUpdate("update store_url_info set w_status=true where id="+id);
                if (i==0){

                    file.deleteOnExit();
                    return;
                }

                if (!imgUrls.isEmpty()){
                    for (Image img : imgUrls) {
                        if (img.getSrc().contains("data:image/gif")) continue;
                        url=new URL(img.getSrc());

                        BufferedInputStream img_in=new BufferedInputStream(url.openConnection().getInputStream());
                        pool1.submit(new GetImg(Filepath,Dic_name,img,img_in,flushFile,count++));
                    }
                    pool1.shutdown();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (BoilerpipeProcessingException e) {
                System.out.println(e.getMessage()+e.getLocalizedMessage()+"this is error");
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
                System.out.println(e.getCause().getMessage()+e.getException()+e.getMessage()+"this is error");;
            } catch (SQLException e) {
                e.printStackTrace();
            }

        }
    }


    static class GetImg implements Runnable{
        private String Filepath;
        private String docPath;
        private BufferedInputStream img_in;
        private Image img;
        private FlushFile flushFile;
        private int count;

        GetImg(String path,String doc,Image ig,BufferedInputStream in,FlushFile flush,int co){
            this.Filepath=path;
            this.docPath=doc;
            this.img=ig;
            this.img_in=in;
            this.flushFile=flush;
            this.count=co;

        }
        @Override
        public void run() {
          flushFile.CreateImg(Filepath,docPath,docPath+String.valueOf(count++)+".jpg",img_in);
        }
    }



}


